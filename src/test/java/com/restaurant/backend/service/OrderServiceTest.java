package com.restaurant.backend.service;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.dto.TableResponse;
import com.restaurant.backend.entity.Category;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RestaurantTableRepository tableRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    private OrderService orderService;
    private TableService tableService;
    private RestaurantTable table;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, tableRepository, menuItemRepository);
        tableService = new TableService(tableRepository, restaurantRepository);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(7L);
        restaurant.setName("Test restaurant");

        table = new RestaurantTable();
        table.setId(12L);
        table.setTableNumber("12");
        table.setQrToken("table-qr-token");
        table.setRestaurant(restaurant);

        Category category = new Category();
        category.setId(3L);
        category.setName("Mains");

        menuItem = new MenuItem();
        menuItem.setId(21L);
        menuItem.setName("Noodles");
        menuItem.setPrice(new BigDecimal("12.50"));
        menuItem.setCategory(category);
    }

    @Test
    void createsOrderFromScannedQrAndReturnsServerPricedConfirmation() {
        when(tableRepository.findByQrToken("table-qr-token")).thenReturn(Optional.of(table));
        when(orderRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.empty());
        when(menuItemRepository.findById(21L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        TableResponse scannedTable = tableService.getTableByQrToken("table-qr-token");
        OrderResponse response = orderService.createOrder(request("table-qr-token", "checkout-1", 2));

        assertEquals(12L, scannedTable.getId());
        assertEquals(7L, scannedTable.getRestaurant().getId());
        assertEquals(99L, response.getId());
        assertEquals(7L, response.getRestaurantId());
        assertEquals(12L, response.getTableId());
        assertEquals("12", response.getTableNumber());
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("25.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("12.50"), response.getItems().get(0).getUnitPrice());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void duplicateCheckoutKeyReturnsExistingOrderInsteadOfCreatingAnother() {
        Order existingOrder = savedOrder();
        when(tableRepository.findByQrToken("table-qr-token")).thenReturn(Optional.of(table));
        when(orderRepository.findByIdempotencyKey("checkout-1"))
                .thenReturn(Optional.empty(), Optional.of(existingOrder));
        when(menuItemRepository.findById(21L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        OrderResponse firstResponse = orderService.createOrder(request("table-qr-token", "checkout-1", 2));
        OrderResponse retryResponse = orderService.createOrder(request("table-qr-token", "checkout-1", 2));

        assertEquals(firstResponse.getId(), retryResponse.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void rejectsMismatchedTableAndUnavailableItemsWithoutCreatingOrder() {
        OrderRequest request = request("table-qr-token", "checkout-1", 1);
        request.setTableId(13L);
        when(tableRepository.findByQrToken("table-qr-token")).thenReturn(Optional.of(table));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any(Order.class));

        request.setTableId(12L);
        when(orderRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.empty());
        menuItem.setAvailable(false);
        when(menuItemRepository.findById(21L)).thenReturn(Optional.of(menuItem));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsInvalidQrAndEmptyCart() {
        when(tableRepository.findByQrToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> tableService.getTableByQrToken("invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(request("invalid", "checkout-1", 1)));

        OrderRequest emptyCart = request("table-qr-token", "checkout-2", 0);
        emptyCart.setItems(List.of());
        when(tableRepository.findByQrToken("table-qr-token")).thenReturn(Optional.of(table));
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(emptyCart));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void allowsOnlyLegalForwardTransitionsAndCancellationBeforePreparation() {
        Order order = savedOrder();
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("CONFIRMED", orderService.updateOrderStatus(99L, "CONFIRMED").getStatus());
        assertEquals("PREPARING", orderService.updateOrderStatus(99L, "PREPARING").getStatus());
        assertEquals("READY", orderService.updateOrderStatus(99L, "READY").getStatus());
        assertEquals("COMPLETED", orderService.updateOrderStatus(99L, "COMPLETED").getStatus());
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(99L, "CANCELLED"));

        Order cancellableOrder = savedOrder();
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(cancellableOrder));
        assertEquals("CANCELLED", orderService.cancelOrder(100L).getStatus());
        assertThrows(IllegalStateException.class, () -> {
            cancellableOrder.setStatus(OrderStatus.COMPLETED);
            orderService.cancelOrder(100L);
        });
    }

    @Test
    void rejectsSkippingOrderLifecycleStages() {
        Order order = savedOrder();
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.of(order));
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(99L, "PREPARING"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    private OrderRequest request(String qrToken, String idempotencyKey, int quantity) {
        OrderRequest request = new OrderRequest();
        request.setQrToken(qrToken);
        request.setIdempotencyKey(idempotencyKey);
        request.setRestaurantId(7L);
        request.setTableId(12L);
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(21L);
        item.setQuantity(quantity);
        request.setItems(List.of(item));
        return request;
    }

    private Order savedOrder() {
        Order order = new Order();
        order.setId(99L);
        order.setRestaurant(table.getRestaurant());
        order.setTable(table);
        order.setTotalAmount(new BigDecimal("25.00"));
        OrderItem item = new OrderItem();
        item.setMenuItem(menuItem);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.50"));
        item.setSubtotal(new BigDecimal("25.00"));
        item.setOrder(order);
        order.setItems(List.of(item));
        return order;
    }
}
