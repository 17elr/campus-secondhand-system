package com.campus.market.auth;

import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@org.springframework.context.annotation.Profile("demo")
@Configuration
public class DataInitializer {
  @Bean
  CommandLineRunner seedUsers(
      UserMapper mapper,
      BCryptPasswordEncoder encoder,
      DataSource dataSource,
      org.springframework.transaction.PlatformTransactionManager manager) {
    return args ->
        new org.springframework.transaction.support.TransactionTemplate(manager)
            .executeWithoutResult(
                transaction -> {
                  JdbcTemplate check = new JdbcTemplate(dataSource);
                  if (check.queryForObject("select count(*) from demo_seed", Integer.class) > 0)
                    return;
                  if (check.queryForObject("select count(*) from sys_user", Integer.class) > 0
                      || check.queryForObject("select count(*) from goods", Integer.class) > 0)
                    throw new IllegalStateException(
                        "Demo initialization requires an empty database");
                  String[][] accounts = {
                    {"student", "林同学"},
                    {"admin", "管理员"},
                    {"zhou", "周同学"},
                    {"chen", "陈同学"},
                    {"wang", "王同学"},
                    {"ajie", "阿杰"},
                    {"zhao", "小赵"},
                    {"li", "李同学"},
                    {"sun", "孙同学"},
                    {"xu", "许同学"},
                    {"wu", "吴同学"},
                    {"gu", "顾同学"},
                    {"demo1", "唐同学"},
                    {"demo2", "赵同学"},
                    {"demo3", "杨同学"},
                    {"demo4", "何同学"},
                    {"demo5", "蒋同学"},
                    {"demo6", "胡同学"},
                    {"demo7", "郑同学"},
                    {"demo8", "高同学"},
                    {"demo9", "罗同学"},
                    {"demo10", "梁同学"},
                    {"demo11", "宋同学"},
                    {"demo12", "谢同学"},
                    {"demo13", "杜同学"},
                    {"demo14", "冯同学"},
                    {"demo15", "程同学"},
                    {"demo16", "韩同学"},
                    {"demo17", "潘同学"},
                    {"demo18", "陆同学"},
                    {"demo19", "彭同学"},
                    {"demo20", "邓同学"},
                    {"demo21", "范同学"},
                    {"demo22", "苏同学"},
                    {"demo23", "叶同学"},
                    {"demo24", "方同学"}
                  };
                  String passwordHash = encoder.encode("123456");
                  for (String[] account : accounts) {
                    if (mapper.findByUsername(account[0]) != null) continue;
                    User user = new User();
                    user.setUsername(account[0]);
                    user.setNickname(account[1]);
                    user.setRole("admin".equals(account[0]) ? "ADMIN" : "USER");
                    user.setPasswordHash(
                        "admin".equals(account[0]) ? encoder.encode("admin123") : passwordHash);
                    mapper.insert(user);
                  }
                  JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                  ResourceDatabasePopulator baseData =
                      new ResourceDatabasePopulator(new ClassPathResource("demo/base-goods.sql"));
                  baseData.setSqlScriptEncoding("UTF-8");
                  baseData.execute(dataSource);
                  if (jdbcTemplate.queryForObject(
                          "select count(*) from goods where title='Java Web 开发实战教材'", Integer.class)
                      == 0) {
                    ResourceDatabasePopulator demoData =
                        new ResourceDatabasePopulator(new ClassPathResource("demo-data.sql"));
                    demoData.setSqlScriptEncoding("UTF-8");
                    demoData.execute(dataSource);
                  }
                  if (jdbcTemplate.queryForObject(
                          "select count(*) from goods where title='毕业季高数习题集'", Integer.class)
                      == 0) {
                    ResourceDatabasePopulator paginationData =
                        new ResourceDatabasePopulator(new ClassPathResource("pagination-data.sql"));
                    paginationData.setSqlScriptEncoding("UTF-8");
                    paginationData.execute(dataSource);
                  }
                  ResourceDatabasePopulator imageRefresh =
                      new ResourceDatabasePopulator(new ClassPathResource("image-refresh.sql"));
                  imageRefresh.setSqlScriptEncoding("UTF-8");
                  imageRefresh.execute(dataSource);
                  // Fixture account names above prove this mapping; never apply nickname inference
                  // to user data.
                  jdbcTemplate.update(
                      "update goods g join sys_user u on u.nickname=g.seller_name set"
                          + " g.seller_id=u.id,g.ownership_verified=TRUE");
                  jdbcTemplate.update(
                      "update trade_order o join sys_user b on b.nickname=o.buyer_name join"
                          + " sys_user s on s.nickname=o.seller_name set"
                          + " o.buyer_id=b.id,o.seller_id=s.id,o.request_key=concat('fixture-',o.id),o.ownership_verified=FALSE");
                  jdbcTemplate.update(
                      "update trade_order set ownership_verified=TRUE where status not in"
                          + " ('PENDING','CONFIRMED')");
                  jdbcTemplate.update(
                      "update trade_order o join (select goods_id from (select goods_id from"
                          + " trade_order where status in ('PENDING','CONFIRMED') group by goods_id"
                          + " having count(*)=1) ids) x on x.goods_id=o.goods_id join goods g on"
                          + " g.id=o.goods_id set o.ownership_verified=TRUE where o.status in"
                          + " ('PENDING','CONFIRMED') and g.status in ('RESERVED','ON_SALE') and"
                          + " o.buyer_id is not null and o.seller_id is not null");
                  jdbcTemplate.update(
                      "update goods g join trade_order o on o.goods_id=g.id set"
                          + " g.status='RESERVED',g.reserved_order_id=o.id where"
                          + " o.ownership_verified=TRUE and o.status in ('PENDING','CONFIRMED')");
                  jdbcTemplate.update(
                      "update goods g set ownership_verified=FALSE where exists(select 1 from"
                          + " trade_order o where o.goods_id=g.id and o.ownership_verified=FALSE"
                          + " and o.status in ('PENDING','CONFIRMED'))");
                  jdbcTemplate.update(
                      "update goods g set favorite_count=(select count(*) from favorite f where"
                          + " f.goods_id=g.id)");
                  jdbcTemplate.update(
                      "update report r join sys_user u on u.nickname=r.reporter_name set"
                          + " r.reporter_id=u.id");
                  jdbcTemplate.update("insert into demo_seed(id) values(1)");
                });
  }
}
