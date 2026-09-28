package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderItemResponse {
    private Long menuItemId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private List<OrderItemAddOnResponse> addOns;

    public OrderItemResponse() {
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public List<OrderItemAddOnResponse> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<OrderItemAddOnResponse> addOns) {
        this.addOns = addOns;
    }
}
