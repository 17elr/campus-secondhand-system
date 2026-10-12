package com.campus.market.goods;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Goods {
    private Long id;
    @NotBlank
    private String title;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
    private String category, conditionLevel, location, description, imageUrl, sellerName, status;
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
