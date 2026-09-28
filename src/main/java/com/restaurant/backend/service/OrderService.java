package com.restaurant.backend.service;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemAddOnRequest;
import com.restaurant.backend.dto.OrderItemAddOnResponse;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.entity.AddOn;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemAddOn;
import com.restaurant.backend.entity.OrderStatus;
import com.restaurant.backend.entity.PaymentStatus;
import com.restaurant.backend.entity.Restaurant;
import com.restaurant.backend.entity.RestaurantTable;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.AddOnRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantTableRepository tableRepository;
    private final MenuItemRepository menuItemRepository;
    private final AddOnRepository addOnRepository;

    public OrderService(
            OrderRepository orderRepository,
            RestaurantTableRepository tableRepository,
            MenuItemRepository menuItemRepository,
            AddOnRepository addOnRepository) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
        this.addOnRepository = addOnRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required");
        }

        String qrToken = request.getQrToken() == null ? "" : request.getQrToken().trim();
        if (qrToken.isEmpty()) {
            throw new IllegalArgumentException("Table QR token is required");
        }

        String idempotencyKey = request.getIdempotencyKey() == null ? "" : request.getIdempotencyKey().trim();
        if (idempotencyKey.isEmpty() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("A valid idempotency key (up to 128 characters) is required");
        }

        RestaurantTable table = tableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR token"));
        if (!table.isActive()) {
            throw new IllegalArgumentException("Table is not active");
        }

        Restaurant restaurant = table.getRestaurant();
        if (restaurant == null || !restaurant.isActive()) {
            throw new IllegalArgumentException("Restaurant is not active");
        }
        if (!Objects.equals(request.getTableId(), table.getId())) {
            throw new IllegalArgumentException("Table ID does not match the scanned QR code");
        }
        if (!Objects.equals(request.getRestaurantId(), restaurant.getId())) {
            throw new IllegalArgumentException("Restaurant ID does not match the scanned QR code");
        }

        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingOrder != null) {
            if (!existingOrder.getTable().getId().equals(table.getId())
                    || !sameItems(existingOrder, request.getItems())) {
                throw new IllegalStateException("Idempotency key was already used for a different order");
            }
            return mapToResponse(existingOrder);
        }

        // Validate order items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getMenuItemId() == null) {
                throw new IllegalArgumentException("Every order item must include a menu item ID");
            }

            // Validate menu item exists
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + itemRequest.getMenuItemId()));

            // Validate menu item is available
            if (!menuItem.isAvailable() || menuItem.getCategory() == null || !menuItem.getCategory().isActive()) {
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
            List<OrderItemAddOn> selectedAddOns = new ArrayList<>();
            Map<Long, Boolean> seenAddOns = new HashMap<>();
            if (itemRequest.getAddOns() != null) {
                for (OrderItemAddOnRequest addOnRequest : itemRequest.getAddOns()) {
                    if (addOnRequest == null || addOnRequest.getAddOnId() == null) {
                        throw new IllegalArgumentException("Every selected add-on must include an ID");
                    }
                    if (addOnRequest.getQuantity() <= 0 || addOnRequest.getQuantity() > 20) {
                        throw new IllegalArgumentException("Add-on quantity must be between 1 and 20");
                    }
                    if (seenAddOns.put(addOnRequest.getAddOnId(), true) != null) {
                        throw new IllegalArgumentException("An add-on cannot be selected more than once per menu item");
                    }
                    AddOn addOn = addOnRepository.findById(addOnRequest.getAddOnId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Add-on not found: " + addOnRequest.getAddOnId()));
                    if (!addOn.isAvailable()) {
                        throw new IllegalArgumentException("Add-on '" + addOn.getName() + "' is not available");
                    }
                    if (menuItem.getAddOns().stream().noneMatch(associated -> associated.getId().equals(addOn.getId()))) {
                        throw new IllegalArgumentException("Add-on '" + addOn.getName()
                                + "' is not available for menu item '" + menuItem.getName() + "'");
                    }

                    OrderItemAddOn snapshot = new OrderItemAddOn();
                    snapshot.setOrderItem(orderItem);
                    snapshot.setAddOnId(addOn.getId());
                    snapshot.setName(addOn.getName());
                    snapshot.setUnitPrice(addOn.getPrice());
                    snapshot.setQuantity(addOnRequest.getQuantity());
                    snapshot.setSubtotal(addOn.getPrice()
                            .multiply(BigDecimal.valueOf(addOnRequest.getQuantity()))
                            .multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
                    selectedAddOns.add(snapshot);
                }
            }
            orderItem.setAddOns(selectedAddOns);
            orderItem.calculateSubtotal(); // Calculate subtotal

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        // Create order
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setTable(table);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setIdempotencyKey(idempotencyKey);
        order.setCustomerName(trimToNull(request.getCustomerName()));
        order.setCustomerPhone(trimToNull(request.getCustomerPhone()));
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // Set order reference in order items
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    private boolean sameItems(Order order, List<OrderItemRequest> requestedItems) {
        if (requestedItems == null || order.getItems().size() != requestedItems.size()) {
            return false;
        }

        Map<OrderItemKey, Integer> savedItems = new HashMap<>();
        for (OrderItem orderItem : order.getItems()) {
            savedItems.merge(orderItemKey(orderItem.getMenuItem().getId(), orderItem.getQuantity(),
                    orderItem.getAddOns().stream().collect(Collectors.toMap(
                            OrderItemAddOn::getAddOnId, OrderItemAddOn::getQuantity))), 1, Integer::sum);
        }

        Map<OrderItemKey, Integer> requestedOrderItems = new HashMap<>();
        for (OrderItemRequest requestedItem : requestedItems) {
            if (requestedItem == null || requestedItem.getMenuItemId() == null) {
                return false;
            }
            Map<Long, Integer> addOns = new HashMap<>();
            if (requestedItem.getAddOns() != null) {
                for (OrderItemAddOnRequest addOn : requestedItem.getAddOns()) {
                    if (addOn == null || addOn.getAddOnId() == null
                            || addOns.put(addOn.getAddOnId(), addOn.getQuantity()) != null) {
                        return false;
                    }
                }
            }
            requestedOrderItems.merge(orderItemKey(requestedItem.getMenuItemId(), requestedItem.getQuantity(), addOns),
                    1, Integer::sum);
        }
        return savedItems.equals(requestedOrderItems);
    }

    private OrderItemKey orderItemKey(Long menuItemId, int quantity, Map<Long, Integer> addOns) {
        return new OrderItemKey(menuItemId, quantity, Map.copyOf(addOns));
    }

    private record OrderItemKey(Long menuItemId, int quantity, Map<Long, Integer> addOns) {
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {
        return orderRepository.findByRestaurantIdWithItems(restaurantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getTableOrders(Long tableId) {
        return orderRepository.findByTableIdWithItems(tableId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException("Order status is required");
        }
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.valueOf(requestedStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported order status: " + requestedStatus);
        }
        if (!isLegalTransition(order.getStatus(), nextStatus)) {
            throw new IllegalStateException(
                    "Order status cannot transition from " + order.getStatus() + " to " + nextStatus);
        }
        order.setStatus(nextStatus);
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        return updateOrderStatus(id, OrderStatus.CANCELLED.name());
    }

    private boolean isLegalTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.READY;
            case READY -> next == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setRestaurantId(order.getRestaurant().getId());
        response.setTableId(order.getTable().getId());
        response.setTableNumber(order.getTable().getTableNumber());
        response.setStatus(order.getStatus().name());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

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
        response.setAddOns(orderItem.getAddOns().stream().map(addOn -> {
            OrderItemAddOnResponse addOnResponse = new OrderItemAddOnResponse();
            addOnResponse.setAddOnId(addOn.getAddOnId());
            addOnResponse.setName(addOn.getName());
            addOnResponse.setQuantity(addOn.getQuantity());
            addOnResponse.setUnitPrice(addOn.getUnitPrice());
            addOnResponse.setSubtotal(addOn.getSubtotal());
            return addOnResponse;
        }).toList());
        return response;
    }
}
