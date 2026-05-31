package com.d0w0b.phytotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot 應用程式進入點
 *
 * scanBasePackages : 讓 @Component / @Service / @Controller 等元件掃描整個 com.d0w0b
 *
 * @EnableJpaRepositories : 明確指定 Spring Data JPA 掃描 Repository 介面的套件
 *                        （預設只掃主程式類別所在套件 com.d0w0b.phytotrack，會找不到
 *                        com.d0w0b.repository）
 * @EntityScan : 明確指定 Hibernate 掃描 @Entity 類別的套件
 *             （預設同上，會找不到 com.d0w0b.models 的 Customer 實體）
 */
@SpringBootApplication(scanBasePackages = { "com.d0w0b" })
@EnableJpaRepositories(basePackages = "com.d0w0b.repository")
@EntityScan(basePackages = "com.d0w0b.models")
public class PhytoTrackApplication {

  public static void main(String[] args) {
    SpringApplication.run(PhytoTrackApplication.class, args);
  }

}
