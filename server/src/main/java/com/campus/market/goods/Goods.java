package com.campus.market.goods;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Goods {
  private Long id;
  private Long sellerId, reservedOrderId, version;
  private Boolean ownershipVerified;

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long v) {
    sellerId = v;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public Long getReservedOrderId() {
    return reservedOrderId;
  }

  public void setReservedOrderId(Long v) {
    reservedOrderId = v;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long v) {
    version = v;
  }

  public Boolean getOwnershipVerified() {
    return ownershipVerified;
  }

  public void setOwnershipVerified(Boolean v) {
    ownershipVerified = v;
  }

  @NotBlank
  @jakarta.validation.constraints.Size(max = 120)
  private String title;

  @NotNull
  @DecimalMin("0.00")
  @jakarta.validation.constraints.DecimalMax("99999999.99")
  @jakarta.validation.constraints.Digits(integer = 8, fraction = 2)
  private BigDecimal price;

  @NotBlank
  @jakarta.validation.constraints.Pattern(regexp = "教材书籍|数码产品|生活用品|运动户外")
  private String category;

  @NotBlank
  @jakarta.validation.constraints.Pattern(regexp = "全新|九成新|八成新|七成新")
  private String conditionLevel;

  @NotBlank
  @jakarta.validation.constraints.Size(max = 80)
  private String location;

  @NotBlank
  @jakarta.validation.constraints.Size(max = 1000)
  private String description;

  @jakarta.validation.constraints.Size(max = 500)
  private String imageUrl;

  private String sellerName, status;
  private Integer favoriteCount;
  private Boolean favorite;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String v) {
    title = v;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal v) {
    price = v;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String v) {
    category = v;
  }

  public String getConditionLevel() {
    return conditionLevel;
  }

  public void setConditionLevel(String v) {
    conditionLevel = v;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String v) {
    location = v;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String v) {
    description = v;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String v) {
    imageUrl = v;
  }

  public String getSellerName() {
    return sellerName;
  }

  public void setSellerName(String v) {
    sellerName = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    status = v;
  }

  public Integer getFavoriteCount() {
    return favoriteCount;
  }

  public void setFavoriteCount(Integer v) {
    favoriteCount = v;
  }

  public Boolean getFavorite() {
    return favorite;
  }

  public void setFavorite(Boolean v) {
    favorite = v;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime v) {
    createdAt = v;
  }
}
