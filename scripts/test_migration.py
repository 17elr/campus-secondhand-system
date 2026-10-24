import unittest
from migrate_legacy import audit, literal, TABLES


class MigrationTest(unittest.TestCase):
    def rows(self):
        rows = {table: [] for table in TABLES}
        rows["sys_user"] = [{"id": "1"}, {"id": "2"}, {"id": "3"}]
        rows["goods"] = [{"id": "10", "price": "12.34", "image_url": "/uploads/original.jpg", "status": "RESERVED"}]
        rows["trade_order"] = [{"id": "20", "goods_id": "10", "price": "12.34", "status": "PENDING"}]
        return rows

    def test_no_nickname_inference_and_source_is_preserved(self):
        rows = self.rows()
        report, sellers, orders, reservations, _ = audit(rows, {})
        self.assertEqual(report["unresolvedGoods"], [10])
        self.assertEqual(report["unresolvedOrders"], [20])
        self.assertEqual(report["amounts"]["goods"], "12.34")
        self.assertEqual(sellers, {})
        self.assertEqual(rows["goods"][0]["image_url"], "/uploads/original.jpg")

    def test_explicit_mapping_can_enable_one_reserved_order(self):
        report, _, orders, reservations, _ = audit(self.rows(), {"goods": {"10": 1}, "trade_order": {"20": {"buyerId": 2, "sellerId": 1}}})
        self.assertEqual(report["unresolvedGoods"], [])
        self.assertEqual(orders, {20: (2, 1)})
        self.assertEqual(reservations, {10: 20})

    def test_conflicts_are_reported_without_choosing_or_deleting_an_order(self):
        rows = self.rows()
        rows["trade_order"].append({"id": "21", "goods_id": "10", "price": "12.34", "status": "PENDING"})
        report, _, orders, _, _ = audit(rows, {"goods": {"10": 1}, "trade_order": {"20": {"buyerId": 2, "sellerId": 1}, "21": {"buyerId": 3, "sellerId": 1}}})
        self.assertEqual(report["activeOrderConflicts"], {"10": [20, 21]})
        self.assertEqual(orders, {})
        self.assertEqual(len(rows["trade_order"]), 2)

    def test_values_are_hex_encoded_and_null_is_distinct(self):
        self.assertEqual(literal(None), "NULL")
        self.assertEqual(literal("x'; DROP TABLE goods;--"), "CONVERT(0x78273b2044524f50205441424c4520676f6f64733b2d2d USING utf8mb4)")


if __name__ == "__main__":
    unittest.main()
