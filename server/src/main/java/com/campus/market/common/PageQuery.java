package com.campus.market.common;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PageQuery {
    @Min(1)
    @Max(1000000)
    private int page = 1;
    @Min(1)
    @Max(100)
    private int size = 9;
    @Size(max = 120)
    private String keyword = "";
    @Size(max = 40)
    private String category = "";
    @Pattern(regexp = "|ON_SALE|OFF_SHELF|RESERVED|SOLD|PENDING|COMPLETED|CANCELLED|PUBLISHED|HIDDEN|FOUND|WITHDRAWN|REPLIED|RESOLVED|IGNORED|0|1")
    private String status = "";
    @Size(max = 30)
    private String condition = "";
    @DecimalMin("0.00")
    @DecimalMax("99999999.99")
    private BigDecimal minPrice;
    @DecimalMin("0.00")
    @DecimalMax("99999999.99")
    private BigDecimal maxPrice;
    @Pattern(regexp = "latest|priceAsc|priceDesc|popular")
    private String sort = "latest";
    private boolean onlyMine;

    @AssertTrue(message = "最高价不能低于最低价")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int value) {
        page = value;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int value) {
        size = value;
    }

    public long getOffset() {
        return (long) (page - 1) * size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String value) {
        keyword = value == null ? "" : value.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        category = value == null ? "" : value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value == null ? "" : value;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String value) {
        condition = value == null ? "" : value;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal value) {
        minPrice = value;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal value) {
        maxPrice = value;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String value) {
        sort = value;
    }

    public boolean isOnlyMine() {
        return onlyMine;
    }

    public void setOnlyMine(boolean value) {
        onlyMine = value;
    }
}
