"""Audit/import an explicit H2 CSV export into a freshly migrated MySQL schema.

Uses the mysql CLI, no Python packages. Default is read-only. No nickname
inference is performed. Password hashes remain in the private source export;
reports contain only counts, IDs, totals, image paths and file checksums.
"""
import argparse
import collections
import csv
from decimal import Decimal
import hashlib
import json
import os
from pathlib import Path
import subprocess

TABLES = {
    "sys_user": "id username password_hash nickname role status created_at",
    "goods": "id title price category condition_level location description image_url seller_name status favorite_count created_at",
    "trade_order": "id goods_id title price buyer_name seller_name location status created_at",
    "favorite": "user_id goods_id created_at",
    "want_post": "id user_id requester_name title category budget description status created_at",
    "goods_message": "id goods_id user_id user_name content reply_content reply_name status created_at replied_at",
    "report": "id reporter_name target_type target_id reason status created_at",
}


def literal(value):
    return "NULL" if value is None else "CONVERT(0x" + str(value).encode("utf-8").hex() + " USING utf8mb4)"


def audit(rows, mapping):
    users = {int(row["id"]) for row in rows["sys_user"]}
    goods = {int(row["id"]): row for row in rows["goods"]}
    active = collections.defaultdict(list)
    for row in rows["trade_order"]:
        if row["status"] in ("PENDING", "CONFIRMED"):
            active[int(row["goods_id"])].append(int(row["id"]))
    seller_map = {int(k): int(v) for k, v in mapping.get("goods", {}).items()}
    order_map = {int(k): v for k, v in mapping.get("trade_order", {}).items()}
    reporter_map = {int(k): int(v) for k, v in mapping.get("report", {}).items()}
    assert set(seller_map) <= set(goods), "Mapping refers to a missing goods ID"
    assert all(uid in users for uid in seller_map.values()), "Unknown mapped seller ID"
    assert all(uid in users for uid in reporter_map.values()), "Unknown mapped reporter ID"
    unresolved_orders, verified_orders, reservations = [], {}, {}
    for row in rows["trade_order"]:
        oid, gid = int(row["id"]), int(row["goods_id"])
        known = order_map.get(oid, {})
        buyer, seller = known.get("buyerId"), known.get("sellerId")
        valid = buyer in users and seller in users and buyer != seller and seller_map.get(gid) == seller
        if row["status"] in ("PENDING", "CONFIRMED"):
            valid = valid and len(active[gid]) == 1 and goods.get(gid, {}).get("status") == "RESERVED"
        if valid:
            verified_orders[oid] = (buyer, seller)
            if row["status"] in ("PENDING", "CONFIRMED"):
                reservations[gid] = oid
        else:
            unresolved_orders.append(oid)
    blocked_goods = {gid for gid, ids in active.items() if any(oid not in verified_orders for oid in ids)}
    # A reserved record without a provable active order must also remain read-only.
    blocked_goods.update(gid for gid, row in goods.items() if row["status"] == "RESERVED" and gid not in reservations)
    unresolved_goods = sorted(gid for gid in goods if gid not in seller_map or gid in blocked_goods)
    invalid = []
    for table in ("favorite", "want_post", "goods_message"):
        for row in rows[table]:
            if int(row["user_id"]) not in users or ("goods_id" in row and int(row["goods_id"]) not in goods):
                invalid.append({"table": table, "id": row.get("id"), "userId": row["user_id"], "goodsId": row.get("goods_id")})
    for row in rows["trade_order"]:
        if int(row["goods_id"]) not in goods:
            invalid.append({"table": "trade_order", "id": row["id"], "goodsId": row["goods_id"]})
    report = {
        "rows": {table: len(data) for table, data in rows.items()},
        "amounts": {table: str(sum((Decimal(row[field]) for row in rows[table] if row[field] is not None), Decimal(0))) for table, field in (("goods", "price"), ("trade_order", "price"), ("want_post", "budget"))},
        "unresolvedGoods": unresolved_goods,
        "unresolvedOrders": unresolved_orders,
        "activeOrderConflicts": {str(gid): ids for gid, ids in active.items() if len(ids) > 1},
        "invalidReferences": invalid,
        "images": {row["id"]: row["image_url"] for row in rows["goods"]},
        "applied": False,
    }
    return report, seller_map, verified_orders, reservations, reporter_map


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("export", type=Path, help="Directory with seven CSVWRITE exports; NULL string must be \\N")
    parser.add_argument("--mapping", type=Path, help="Explicit record IDs -> verified user IDs, see docs/MIGRATION.md")
    parser.add_argument("--report", type=Path, required=True)
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--verify-only", action="store_true", help="Validate an existing import without writing database data")
    parser.add_argument("--mysql", default="mysql")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=13319)
    parser.add_argument("--user", default="market")
    parser.add_argument("--database", default="market")
    args = parser.parse_args()
    rows = {}
    for table, columns in TABLES.items():
        with (args.export / (table + ".csv")).open(encoding="utf-8-sig", newline="") as source:
            data = [{k.lower(): None if v == "\\N" else v for k, v in row.items()} for row in csv.DictReader(source)]
        assert all(set(row) == set(columns.split()) for row in data), "Unexpected CSV columns for " + table
        rows[table] = data
    mapping = json.loads(args.mapping.read_text(encoding="utf-8")) if args.mapping else {}
    report, sellers, orders, reservations, reporters = audit(rows, mapping)
    report["sourceChecksums"] = {table: hashlib.sha256((args.export / (table + ".csv")).read_bytes()).hexdigest() for table in TABLES}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assert not report["invalidReferences"], "Invalid existing references: report written, nothing imported"
    if not (args.apply or args.verify_only):
        print("Audit complete; no database writes. Unresolved records will remain read-only.")
        return
    assert os.environ.get("MYSQL_PWD"), "Set MYSQL_PWD privately before applying"
    command = [args.mysql, "--no-defaults", "--host=" + args.host, "--port=" + str(args.port), "--user=" + args.user, "--database=" + args.database, "--default-character-set=utf8mb4", "--batch", "--raw", "--skip-column-names"]

    def sql(text):
        result = subprocess.run(command, input=text, text=True, encoding="utf-8", capture_output=True)
        # Never echo SQL or driver errors: import SQL contains private password hashes.
        if result.returncode:
            raise RuntimeError("MySQL import/check failed; source export preserved. No SQL printed.")
        return result.stdout.strip()

    if args.apply:
        occupied = sum(int(sql("select count(*) from " + table)) for table in TABLES)
        assert occupied == 0, "Import requires an empty business schema; never overwrite existing data"
    if args.apply:
        statements = ["CREATE TEMPORARY TABLE migration_guard(ok BOOLEAN CHECK(ok=TRUE))", "START TRANSACTION"]
        for table, columns in TABLES.items():
            for row in rows[table]:
                statements.append("insert into " + table + "(" + ",".join(columns.split()) + ") values(" + ",".join(literal(row[c]) for c in columns.split()) + ")")
        for gid, uid in sellers.items():
            verified = gid not in report["unresolvedGoods"]
            statements.append(f"update goods set seller_id={uid},ownership_verified={int(verified)} where id={gid}")
        for oid, (buyer, seller) in orders.items():
            statements.append(f"update trade_order set buyer_id={buyer},seller_id={seller},request_key='legacy-{oid}',ownership_verified=TRUE where id={oid}")
        for gid, oid in reservations.items():
            statements.append(f"update goods set reserved_order_id={oid} where id={gid}")
        for rid, uid in reporters.items():
            statements.append(f"update report set reporter_id={uid} where id={rid}")
        statements.append("update goods g set favorite_count=(select count(*) from favorite f where f.goods_id=g.id)")
        # Any failed check stops the mysql batch and rolls back before COMMIT.
        for table, data in rows.items():
            statements.append(f"insert into migration_guard select count(*)={len(data)} from {table}")
            for row in data:
                conditions = " AND ".join(c + " <=> " + literal(row[c]) for c in TABLES[table].split() if c != "favorite_count")
                statements.append(f"insert into migration_guard select exists(select 1 from {table} where {conditions})")
        statements.append("COMMIT")
        sql(";\n".join(statements) + ";")
    for table, data in rows.items():
        assert int(sql("select count(*) from " + table)) == len(data), "Post-import count mismatch"
        fields = [c for c in ("id", "goods_id", "user_id", "price", "budget", "image_url") if c in TABLES[table].split()]
        expression = "JSON_OBJECT(" + ",".join(literal(c) + "," + c for c in fields) + ")"
        target = [json.loads(line) for line in sql("select " + expression + " from " + table).splitlines()]
        def normalized(row):
            return tuple("<NULL>" if row[c] is None else format(Decimal(str(row[c])).normalize(), "f") if c in ("price", "budget") else str(row[c]) for c in fields)
        assert sorted(map(normalized, target)) == sorted(map(normalized, data)), "Post-import IDs, amounts or images mismatch"
    report["applied"] = True
    report["verifiedExisting"] = args.verify_only
    report["validated"] = "All seven row counts, record IDs, relationship IDs, prices, budgets and image paths match the source export"
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Row counts, amounts, IDs and image paths verified. Source files preserved; no existing data overwritten.")


if __name__ == "__main__":
    main()
