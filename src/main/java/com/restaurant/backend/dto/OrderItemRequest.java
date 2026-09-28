package com.restaurant.backend.dto;

import java.util.List;

public class OrderItemRequest {
    private Long menuItemId;
    private int quantity;
    private List<OrderItemAddOnRequest> addOns;

    public OrderItemRequest() {
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<OrderItemAddOnRequest> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<OrderItemAddOnRequest> addOns) {
        this.addOns = addOns;
    }
}
