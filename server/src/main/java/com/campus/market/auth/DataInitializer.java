package com.campus.market.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.sql.DataSource;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserMapper mapper, BCryptPasswordEncoder encoder, DataSource dataSource) {
        return args -> {
            String[][] accounts = { { "student", "林同学" }, { "admin", "管理员" }, { "zhou", "周同学" },
                    { "chen", "陈同学" }, { "wang", "王同学" }, { "ajie", "阿杰" }, { "zhao", "小赵" }, { "li", "李同学" },
                    { "sun", "孙同学" }, { "xu", "许同学" }, { "wu", "吴同学" }, { "gu", "顾同学" }, { "demo1", "唐同学" },
                    { "demo2", "赵同学" }, { "demo3", "杨同学" }, { "demo4", "何同学" }, { "demo5", "蒋同学" },
                    { "demo6", "胡同学" }, { "demo7", "郑同学" }, { "demo8", "高同学" }, { "demo9", "罗同学" },
                    { "demo10", "梁同学" }, { "demo11", "宋同学" }, { "demo12", "谢同学" }, { "demo13", "杜同学" },
                    { "demo14", "冯同学" }, { "demo15", "程同学" }, { "demo16", "韩同学" }, { "demo17", "潘同学" },
                    { "demo18", "陆同学" }, { "demo19", "彭同学" }, { "demo20", "邓同学" }, { "demo21", "范同学" },
                    { "demo22", "苏同学" }, { "demo23", "叶同学" }, { "demo24", "方同学" } };
            String passwordHash = encoder.encode("123456");
            for (String[] account : accounts) {
                if (mapper.findByUsername(account[0]) != null)
                    continue;
                User user = new User();
                user.setUsername(account[0]);
                user.setNickname(account[1]);
                user.setRole("admin".equals(account[0]) ? "ADMIN" : "USER");
                user.setPasswordHash("admin".equals(account[0]) ? encoder.encode("admin123") : passwordHash);
                mapper.insert(user);
            }
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            if (jdbcTemplate.queryForObject("select count(*) from goods where title='Java Web 开发实战教材'",
                    Integer.class) == 0) {
                ResourceDatabasePopulator demoData = new ResourceDatabasePopulator(
                        new ClassPathResource("demo-data.sql"));
                demoData.setSqlScriptEncoding("UTF-8");
                demoData.execute(dataSource);
            }
            if (jdbcTemplate.queryForObject("select count(*) from goods where title='毕业季高数习题集'",
                    Integer.class) == 0) {
                ResourceDatabasePopulator paginationData = new ResourceDatabasePopulator(
                        new ClassPathResource("pagination-data.sql"));
                paginationData.setSqlScriptEncoding("UTF-8");
                paginationData.execute(dataSource);
            }
            ResourceDatabasePopulator imageRefresh = new ResourceDatabasePopulator(
                    new ClassPathResource("image-refresh.sql"));
            imageRefresh.setSqlScriptEncoding("UTF-8");
            imageRefresh.execute(dataSource);
        };
    }
}
