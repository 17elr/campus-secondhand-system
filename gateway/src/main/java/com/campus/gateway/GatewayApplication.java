package com.campus.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.*;

@SpringBootApplication
public class GatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> marketRoutes(@Value("${market.url}") String market) {
    return route("market")
        .route(path("/api/**").or(path("/uploads/**")), http())
        .before(uri(market))
        .before(removeRequestHeader("X-User-Id"))
        .before(removeRequestHeader("X-User-Role"))
        .before(removeRequestHeader("X-Internal-Token"))
        .build();
  }
}
