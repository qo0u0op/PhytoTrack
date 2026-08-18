package com.d0w0b.phytotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 應用程式進入點
 *
 * 所有子套件（controller、service、repository、models 等）都位於本類別所在套件
 * com.d0w0b.phytotrack 之下，因此預設的元件掃描（Component Scan）即可涵蓋全部，
 * 無需額外設定 scanBasePackages。
 */
@SpringBootApplication
public class PhytoTrackApplication {

  public static void main(String[] args) {
    SpringApplication.run(PhytoTrackApplication.class, args);
  }

}
