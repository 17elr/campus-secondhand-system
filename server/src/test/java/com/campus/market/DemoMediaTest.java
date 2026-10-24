package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DemoMediaTest extends MarketTestSupport {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;

  @Test
  void demoPhotosAreLocalValidDistinctAndSeparateFromBanners() throws Exception {
    List<String> images =
        jdbc.query(
            "select image_url from goods where id<=78 order by id",
            (result, rowNumber) -> result.getString("image_url"));
    assertEquals(78, images.size());
    assertEquals(78, new HashSet<>(images).size());
    Set<String> hashes = new HashSet<>();
    Path publicDir = Path.of("..", "web", "public").toAbsolutePath().normalize();
    for (String image : images) {
      assertTrue(image.matches("/images/goods/[0-9]{3}\\.webp"), image);
      Path file = publicDir.resolve(image.substring(1));
      byte[] bytes = Files.readAllBytes(file);
      assertTrue(bytes.length > 1000, image);
      assertEquals("RIFF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
      assertEquals("WEBP", new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
      assertTrue(
          hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))),
          image + " repeats another photo");
    }
    List<String> pages =
        List.of(
            "market",
            "wanted",
            "mine",
            "favorites",
            "orders",
            "messages",
            "admin-overview",
            "admin-goods",
            "admin-users",
            "admin-orders",
            "admin-messages",
            "admin-wants",
            "admin-reports",
            "admin-market");
    for (String page : pages) {
      for (int slide = 1; slide <= 3; slide++) {
        String image = "/images/banners/" + page + "-" + slide + ".webp";
        assertFalse(images.contains(image));
        byte[] bytes = Files.readAllBytes(publicDir.resolve(image.substring(1)));
        assertTrue(bytes.length > 1000, image);
        assertEquals("RIFF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals("WEBP", new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(
            hashes.add(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))),
            image + " duplicates another page or product");
      }
    }
    assertEquals(120, hashes.size());
  }

  @Test
  void refreshingDemoImagesDoesNotOverwriteUserPhotoWithSameTitle() {
    String uploaded = "https://example.com/user-upload.jpg";
    jdbc.update(
        "insert into goods(title,price,seller_name,status,image_url,created_at)"
            + " values(?,10,'测试同学','ON_SALE',?,CURRENT_TIMESTAMP)",
        "Java Web 开发实战教材",
        uploaded);
    Long userGoodsId = jdbc.queryForObject("select max(id) from goods", Long.class);
    ResourceDatabasePopulator refresh =
        new ResourceDatabasePopulator(new ClassPathResource("image-refresh.sql"));
    refresh.setSqlScriptEncoding("UTF-8");
    refresh.execute(dataSource);
    assertEquals(
        uploaded,
        jdbc.queryForObject("select image_url from goods where id=?", String.class, userGoodsId));
  }
}
