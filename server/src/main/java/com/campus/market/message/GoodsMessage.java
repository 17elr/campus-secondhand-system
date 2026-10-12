package com.campus.market.message;

import java.time.LocalDateTime;

public class GoodsMessage {
    private Long id;
    private Long goodsId;
    private Long userId;
    private String userName, content, replyContent, replyName, status, goodsTitle;
    private LocalDateTime createdAt, repliedAt;

    public String getGoodsTitle() {
        return goodsTitle;
    }

    public void setGoodsTitle(String value) {
        goodsTitle = value;
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String v) {
        userName = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public String getReplyContent() {
        return replyContent;
    }

    public void setReplyContent(String v) {
        replyContent = v;
    }

    public String getReplyName() {
        return replyName;
    }

    public void setReplyName(String v) {
        replyName = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime v) {
        repliedAt = v;
    }
}
