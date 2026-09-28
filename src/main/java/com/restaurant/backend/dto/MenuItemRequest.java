package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class MenuItemRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String dietaryType;
    private Integer preparationTimeMinutes;
    private Long categoryId;
    private Boolean available;
    private List<MenuItemAddOnRequest> addOns;

    public MenuItemRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(String dietaryType) {
        this.dietaryType = dietaryType;
    }

    public Integer getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public void setPreparationTimeMinutes(Integer preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public List<MenuItemAddOnRequest> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<MenuItemAddOnRequest> addOns) {
        this.addOns = addOns;
    }
}
