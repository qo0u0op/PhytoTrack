package com.d0w0b.phytotrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA 回退：前端 Vue Router 為 history 模式，/login、/cases 等非 /api 路徑
 * 若無對應 Controller，應回退至 static/index.html 由前端接管
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers (ResourceHandlerRegistry registry) {
    registry.addResourceHandler ("/**")
        .addResourceLocations ("classpath:/static/")
        .resourceChain (true)
        .addResolver (new PathResourceResolver () {
          @Override
          protected Resource getResource (String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative (resourcePath);
            // 若請求資源存在（js/css/png）則直接回傳，否則回退 index.html
            if (requested.exists () && requested.isReadable ()) {
              return requested;
            }
            // /api 與 /actuator 不回退，由 Security/Controller 處理
            if (resourcePath.startsWith ("api/") || resourcePath.startsWith ("actuator/") || resourcePath.startsWith ("v3/") || resourcePath.startsWith ("swagger")) {
              return null;
            }
            return new ClassPathResource ("/static/index.html");
          }
        });
  }
}
