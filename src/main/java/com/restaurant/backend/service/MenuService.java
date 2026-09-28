package com.restaurant.backend.service;

import com.restaurant.backend.dto.MenuItemAvailabilityRequest;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public MenuService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + id));
        return mapToResponse(menuItem);
    }

    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item name is required");
        }

        // Validate price
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be greater than or equal to 0");
        }

        // Validate category
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Category is not active");
        }

        MenuItem menuItem = new MenuItem();
        menuItem.setName(request.getName().trim());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setCategory(category);
        menuItem.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return mapToResponse(savedMenuItem);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + id));

        // Validate name
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            menuItem.setName(request.getName().trim());
        }

        // Validate price
        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be greater than or equal to 0");
            }
            menuItem.setPrice(request.getPrice());
        }

        // Validate category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));

            if (!category.isActive()) {
                throw new IllegalArgumentException("Category is not active");
            }
            menuItem.setCategory(category);
        }

        if (request.getDescription() != null) {
            menuItem.setDescription(request.getDescription());
        }

        if (request.getImageUrl() != null) {
            menuItem.setImageUrl(request.getImageUrl());
        }

        if (request.getAvailable() != null) {
            menuItem.setAvailable(request.getAvailable());
        }

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return mapToResponse(savedMenuItem);
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + id));
        menuItemRepository.delete(menuItem);
    }

    @Transactional
    public MenuItemResponse updateMenuItemAvailability(Long id, MenuItemAvailabilityRequest request) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found with id: " + id));

        menuItem.setAvailable(request.isAvailable());
        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return mapToResponse(savedMenuItem);
    }

    private MenuItemResponse mapToResponse(MenuItem menuItem) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(menuItem.getId());
        response.setName(menuItem.getName());
        response.setDescription(menuItem.getDescription());
        response.setPrice(menuItem.getPrice());
        response.setImageUrl(menuItem.getImageUrl());
        response.setAvailable(menuItem.isAvailable());

        if (menuItem.getCategory() != null) {
            com.restaurant.backend.dto.CategoryResponse categoryResponse = new com.restaurant.backend.dto.CategoryResponse();
            categoryResponse.setId(menuItem.getCategory().getId());
            categoryResponse.setName(menuItem.getCategory().getName());
            categoryResponse.setActive(menuItem.getCategory().isActive());
            response.setCategory(categoryResponse);
        }

        return response;
    }
}
