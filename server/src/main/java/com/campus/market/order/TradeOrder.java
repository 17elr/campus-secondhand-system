package com.campus.market.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeOrder {
    private Long id;
    private Long goodsId;
    private String title, buyerName, sellerName, location, status;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long v) {
        goodsId = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String v) {
        buyerName = v;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String v) {
        sellerName = v;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String v) {
        location = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal v) {
        price = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
