package com.restaurant.backend.dto;

import java.math.BigDecimal;

public class OrderItemAddOnResponse {
    private Long addOnId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public Long getAddOnId() { return addOnId; }
    public void setAddOnId(Long addOnId) { this.addOnId = addOnId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
