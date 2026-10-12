package com.campus.market.want;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class Want {
    private Long id;
    private Long userId;
    private String requesterName, status;
    @NotBlank
    @Size(max = 120)
    private String title;
    @NotBlank
    @Pattern(regexp = "教材书籍|数码产品|生活用品|运动户外")
    private String category;
    @NotBlank
    @Size(max = 500)
    private String description;
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("99999999.99")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal budget;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String v) {
        requesterName = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String v) {
        category = v;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal v) {
        budget = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
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
}
