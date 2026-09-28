package com.restaurant.backend.dto;

public class OrderItemAddOnRequest {
    private Long addOnId;
    private int quantity = 1;

    public Long getAddOnId() { return addOnId; }
    public void setAddOnId(Long addOnId) { this.addOnId = addOnId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
