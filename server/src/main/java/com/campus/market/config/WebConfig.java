package com.campus.market.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${market.upload-dir}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uri = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(uri.endsWith("/") ? uri : uri + "/");
  }
}
