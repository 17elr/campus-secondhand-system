package com.campus.market.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeOrder {
  private Long id;
  private Long goodsId;
  private Long buyerId, sellerId;
  private Boolean ownershipVerified;
  private String requestKey, requestHash, cancelReason;
  private LocalDateTime confirmedAt, completedAt, cancelledAt;

  public Long getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(Long v) {
    buyerId = v;
  }

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long v) {
    sellerId = v;
  }

  public Boolean getOwnershipVerified() {
    return ownershipVerified;
  }

  public void setOwnershipVerified(Boolean v) {
    ownershipVerified = v;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String v) {
    requestKey = v;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String v) {
    requestHash = v;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(String v) {
    cancelReason = v;
  }

  public LocalDateTime getConfirmedAt() {
    return confirmedAt;
  }

  public void setConfirmedAt(LocalDateTime v) {
    confirmedAt = v;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime v) {
    completedAt = v;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime v) {
    cancelledAt = v;
  }

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
