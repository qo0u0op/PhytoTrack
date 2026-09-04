package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table (name = "cities")
public class City {
  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private Long cityId;

  @Column (nullable = false)
  private String city;

  /** 反向關聯：此縣市下的鄉鎮市區清單 (配合 @EntityGraph 一次抓取) */
  @OneToMany (mappedBy = "city", fetch = FetchType.LAZY)
  private List<District> districts;
}
