package com.restaurant.backend.service;

import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.entity.AddOn;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.repository.AddOnRepository;
import com.restaurant.backend.repository.CategoryRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceAddOnTest {
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
        category.setName("Mains");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    }

    @Test
    void attachesExistingReusableAddOnToMenuItem() {
        AddOn cheese = new AddOn();
        cheese.setId(8L);
        cheese.setName("Cheese");
        cheese.setPrice(new BigDecimal("30.00"));
        cheese.setAvailable(true);
        when(addOnRepository.findById(8L)).thenReturn(Optional.of(cheese));
        when(addOnRepository.save(any(AddOn.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem item = invocation.getArgument(0);
            item.setId(3L);
            return item;
        });

        MenuItemAddOnRequest attachment = new MenuItemAddOnRequest();
        attachment.setId(8L);
        MenuItemRequest request = menuRequest(List.of(attachment));

        assertEquals(8L, menuService.createMenuItem(request).getAddOns().get(0).getId());
    }

    @Test
    void createsAddOnAsPartOfMenuItemRequestAndReturnsIt() {
        when(addOnRepository.save(any(AddOn.class))).thenAnswer(invocation -> {
            AddOn addOn = invocation.getArgument(0);
            addOn.setId(9L);
            return addOn;
        });
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem item = invocation.getArgument(0);
            item.setId(4L);
            return item;
        });

        MenuItemAddOnRequest newAddOn = new MenuItemAddOnRequest();
        newAddOn.setName("Mayonnaise");
        newAddOn.setPrice(new BigDecimal("20.00"));

        var response = menuService.createMenuItem(menuRequest(List.of(newAddOn)));

        assertEquals(9L, response.getAddOns().get(0).getId());
        assertEquals("Mayonnaise", response.getAddOns().get(0).getName());
    }

    private MenuItemRequest menuRequest(List<MenuItemAddOnRequest> addOns) {
        MenuItemRequest request = new MenuItemRequest();
        request.setName("Burger");
        request.setPrice(new BigDecimal("180.00"));
        request.setCategoryId(1L);
        request.setAddOns(addOns);
        return request;
    }
}
