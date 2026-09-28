package com.restaurant.backend.service;

import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.AddOnRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImageUrlTest {
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AddOnRepository addOnRepository;

    private MenuService menuService;
    private Category category;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(menuItemRepository, categoryRepository, addOnRepository);
        category = new Category();
        category.setId(1L);
        category.setName("Main");
    }

    @Test
    void acceptsHttpAndHttpsImageUrls() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String imageUrl : List.of("http://images.example.org/dish.jpg", "https://images.example.org/dish.jpg")) {
            MenuItemRequest request = request(imageUrl);
            assertEquals(imageUrl, menuService.createMenuItem(request).getImageUrl());
        }
    }

    @Test
    void acceptsPublicImageUrlsLongerThanVarchar255() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String imageUrl = "https://images.example.org/" + "a".repeat(280) + ".jpg";

        assertEquals(imageUrl, menuService.createMenuItem(request(imageUrl)).getImageUrl());
    }

    @Test
    void acceptsDescriptionsLongerThanVarchar255WithinFormLimit() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MenuItemRequest request = request("/uploads/menu-items/123e4567-e89b-12d3-a456-426614174000.png");
        request.setDescription("a".repeat(300));

        assertEquals(300, menuService.createMenuItem(request).getDescription().length());
    }

    @Test
    void rejectsNonHttpImageUrls() {
        MenuItemRequest request = request("javascript:alert(1)");

        assertThrows(InvalidImageUrlException.class, () -> menuService.createMenuItem(request));
    }

    @Test
    void rejectsDataUrlsInsteadOfStoringImageData() {
        MenuItemRequest request = request("data:image/png;base64,aGVsbG8=");

        InvalidImageUrlException error = assertThrows(
                InvalidImageUrlException.class,
                () -> menuService.createMenuItem(request));
        assertEquals("Image data must be uploaded; provide an HTTP or HTTPS image URL", error.getMessage());
    }

    @Test
    void rejectsImageUrlsLongerThanSupportedLimit() {
        MenuItemRequest request = request("https://images.example.org/" + "a".repeat(2048));

        assertThrows(InvalidImageUrlException.class, () -> menuService.createMenuItem(request));
    }

    @Test
    void rejectsMenuItemNameLongerThanFormLimit() {
        MenuItemRequest request = request("");
        request.setName("a".repeat(121));

        assertEquals(
                "Menu item name cannot exceed 120 characters",
                assertThrows(IllegalArgumentException.class, () -> menuService.createMenuItem(request)).getMessage());
    }

    @Test
    void rejectsMenuItemDescriptionLongerThanFormLimit() {
        MenuItemRequest request = request("");
        request.setDescription("a".repeat(501));

        assertEquals(
                "Menu item description cannot exceed 500 characters",
                assertThrows(IllegalArgumentException.class, () -> menuService.createMenuItem(request)).getMessage());
    }

    @Test
    void blankImageUrlRemovesAnExistingImage() {
        MenuItem item = new MenuItem();
        item.setId(12L);
        item.setName("Soup");
        item.setPrice(new BigDecimal("5.00"));
        item.setImageUrl("https://images.example.org/soup.jpg");
        item.setCategory(category);
        when(menuItemRepository.findById(12L)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemRequest request = new MenuItemRequest();
        request.setImageUrl(" ");

        assertNull(menuService.updateMenuItem(12L, request).getImageUrl());
        verify(menuItemRepository).save(item);
    }

    private MenuItemRequest request(String imageUrl) {
        MenuItemRequest request = new MenuItemRequest();
        request.setName("Dish");
        request.setPrice(new BigDecimal("5.00"));
        request.setCategoryId(1L);
        request.setImageUrl(imageUrl);
        return request;
    }
}
