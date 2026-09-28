package com.restaurant.backend.service;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderStatus;
import com.restaurant.backend.entity.Restaurant;
import com.restaurant.backend.entity.RestaurantTable;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RestaurantRepository;
import com.restaurant.backend.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            RestaurantTableRepository tableRepository,
            MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Validate restaurant
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + request.getRestaurantId()));

        // Validate table
        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + request.getTableId()));

        // Validate table belongs to restaurant
        if (!table.getRestaurant().getId().equals(restaurant.getId())) {
            throw new IllegalArgumentException("Table does not belong to the specified restaurant");
        }

        // Validate table is active
        if (!table.isActive()) {
            throw new IllegalArgumentException("Table is not active");
        }

        // Validate order items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            // Validate menu item exists
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + itemRequest.getMenuItemId()));

            // Validate menu item is available
            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("Menu item '" + menuItem.getName() + "' is not available");
            }

            // Validate quantity
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 for menu item: " + menuItem.getName());
            }

            // Create order item with actual price from database
            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice()); // Use actual price from DB
            orderItem.calculateSubtotal(); // Calculate subtotal

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        // Create order
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setTable(table);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // Set order reference in order items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setRestaurantId(order.getRestaurant().getId());
        response.setTableId(order.getTable().getId());
        response.setTableNumber(order.getTable().getTableNumber());
        response.setStatus(order.getStatus().name());
        response.setTotalAmount(order.getTotalAmount());

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapOrderItemToResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemResponse mapOrderItemToResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setMenuItemId(orderItem.getMenuItem().getId());
        response.setName(orderItem.getMenuItem().getName());
        response.setQuantity(orderItem.getQuantity());
        response.setUnitPrice(orderItem.getUnitPrice());
        response.setSubtotal(orderItem.getSubtotal());
        return response;
    }
}
