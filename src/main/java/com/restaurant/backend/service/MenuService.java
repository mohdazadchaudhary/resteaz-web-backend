package com.restaurant.backend.service;

import com.restaurant.backend.dto.MenuItemAvailabilityRequest;
import com.restaurant.backend.dto.AddOnResponse;
import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.entity.AddOn;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.entity.MenuItem;
import com.restaurant.backend.repository.AddOnRepository;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final AddOnRepository addOnRepository;

    public MenuService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository,
                       AddOnRepository addOnRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.addOnRepository = addOnRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

        String imageUrl = validateImageUrl(request.getImageUrl());

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
        menuItem.setImageUrl(imageUrl);
        menuItem.setDietaryType(normalizeDietaryType(request.getDietaryType()));
        menuItem.setPreparationTimeMinutes(validatePreparationTime(request.getPreparationTimeMinutes()));
        menuItem.setCategory(category);
        menuItem.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        menuItem.setAddOns(request.getAddOns() == null ? new ArrayList<>() : resolveAddOns(request.getAddOns()));

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
            menuItem.setImageUrl(validateImageUrl(request.getImageUrl()));
        }

        if (request.getDietaryType() != null) {
            menuItem.setDietaryType(normalizeDietaryType(request.getDietaryType()));
        }

        if (request.getPreparationTimeMinutes() != null) {
            menuItem.setPreparationTimeMinutes(validatePreparationTime(request.getPreparationTimeMinutes()));
        }

        if (request.getAvailable() != null) {
            menuItem.setAvailable(request.getAvailable());
        }

        if (request.getAddOns() != null) {
            menuItem.setAddOns(resolveAddOns(request.getAddOns()));
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
        response.setDietaryType(menuItem.getDietaryType());
        response.setPreparationTimeMinutes(menuItem.getPreparationTimeMinutes());
        response.setAvailable(menuItem.isAvailable());

        if (menuItem.getCategory() != null) {
            com.restaurant.backend.dto.CategoryResponse categoryResponse = new com.restaurant.backend.dto.CategoryResponse();
            categoryResponse.setId(menuItem.getCategory().getId());
            categoryResponse.setName(menuItem.getCategory().getName());
            categoryResponse.setActive(menuItem.getCategory().isActive());
            response.setCategory(categoryResponse);
        }

        response.setAddOns(menuItem.getAddOns().stream()
                .map(this::mapAddOn)
                .collect(Collectors.toList()));
        return response;
    }

    private List<AddOn> resolveAddOns(List<MenuItemAddOnRequest> requests) {
        List<AddOn> addOns = new ArrayList<>();
        java.util.Set<Long> seenIds = new HashSet<>();
        java.util.Set<String> seenNames = new HashSet<>();
        for (MenuItemAddOnRequest request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Add-on entry is required");
            }
            AddOn addOn;
            if (request.getId() != null) {
                if (!seenIds.add(request.getId())) {
                    throw new IllegalArgumentException("An add-on cannot be attached more than once");
                }
                addOn = addOnRepository.findById(request.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Add-on not found: " + request.getId()));
                if (request.getPrice() != null) {
                    validateAddOnPrice(request.getPrice());
                    addOn.setPrice(request.getPrice());
                }
                if (request.getName() != null && !request.getName().isBlank()) {
                    addOn.setName(validateAddOnName(request.getName()));
                }
                if (request.getDescription() != null) {
                    addOn.setDescription(request.getDescription().trim());
                }
                if (request.getDietaryType() != null && !request.getDietaryType().isBlank()) {
                    addOn.setDietaryType(normalizeAddOnDietaryType(request.getDietaryType()));
                }
                if (request.getAvailable() != null) {
                    addOn.setAvailable(request.getAvailable());
                }
                addOns.add(addOnRepository.save(addOn));
            } else {
                addOn = new AddOn();
                addOn.setName(validateAddOnName(request.getName()));
                String normalizedName = addOn.getName().toLowerCase(java.util.Locale.ROOT);
                if (!seenNames.add(normalizedName) || addOnRepository.existsByNameIgnoreCase(addOn.getName())) {
                    throw new IllegalArgumentException(
                            "Add-on already exists; attach the existing add-on instead");
                }
                addOn.setPrice(request.getPrice());
                validateAddOnPrice(addOn.getPrice());
                addOn.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
                addOn.setDietaryType(normalizeAddOnDietaryType(request.getDietaryType()));
                addOn.setAvailable(request.getAvailable() == null || request.getAvailable());
                addOns.add(addOnRepository.save(addOn));
            }
        }
        return addOns;
    }

    private String validateAddOnName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new IllegalArgumentException("Add-on name is required and must be at most 120 characters");
        }
        return name.trim();
    }

    private void validateAddOnPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Add-on price must be greater than zero");
        }
    }

    private String normalizeAddOnDietaryType(String dietaryType) {
        if (dietaryType == null || dietaryType.isBlank()) {
            return null;
        }
        String normalized = dietaryType.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("veg", "non-veg").contains(normalized)) {
            throw new IllegalArgumentException("Add-on type must be veg or non-veg");
        }
        return normalized;
    }

    private AddOnResponse mapAddOn(AddOn addOn) {
        AddOnResponse response = new AddOnResponse();
        response.setId(addOn.getId());
        response.setName(addOn.getName());
        response.setDescription(addOn.getDescription());
        response.setPrice(addOn.getPrice());
        response.setDietaryType(addOn.getDietaryType());
        response.setAvailable(addOn.isAvailable());
        return response;
    }

    private String normalizeDietaryType(String dietaryType) {
        if (dietaryType == null || dietaryType.isBlank()) {
            return null;
        }
        String normalized = dietaryType.trim().toLowerCase();
        if (!List.of("veg", "non-veg", "vegan").contains(normalized)) {
            throw new IllegalArgumentException("Dietary type must be veg, non-veg, or vegan");
        }
        return normalized;
    }

    private Integer validatePreparationTime(Integer preparationTimeMinutes) {
        if (preparationTimeMinutes != null && preparationTimeMinutes < 0) {
            throw new IllegalArgumentException("Preparation time cannot be negative");
        }
        return preparationTimeMinutes;
    }

    private String validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String value = imageUrl.trim();
        if (value.matches("^/uploads/menu-items/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|gif)$")) {
            return value;
        }
        if (value.length() > 2048) {
            throw new InvalidImageUrlException("Image URL cannot exceed 2048 characters");
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidImageUrlException("Image URL must be a valid HTTP or HTTPS URL");
        }

        String scheme = uri.getScheme();
        if (uri.getHost() == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535))
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new InvalidImageUrlException("Image URL must be a valid HTTP or HTTPS URL");
        }
        String normalizedUrl = uri.toASCIIString();
        if (normalizedUrl.length() > 2048) {
            throw new InvalidImageUrlException("Image URL cannot exceed 2048 characters");
        }
        return normalizedUrl;
    }
}
