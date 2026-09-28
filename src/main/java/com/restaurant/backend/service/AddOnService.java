package com.restaurant.backend.service;

import com.restaurant.backend.dto.AddOnResponse;
import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.entity.AddOn;
import com.restaurant.backend.repository.AddOnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AddOnService {
    private final AddOnRepository repository;

    public AddOnService(AddOnRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AddOnResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddOnResponse create(MenuItemAddOnRequest request) {
        validate(request);
        if (repository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new IllegalArgumentException("Add-on already exists; attach the existing add-on instead");
        }
        AddOn addOn = new AddOn();
        apply(addOn, request);
        return toResponse(repository.save(addOn));
    }

    @Transactional
    public AddOnResponse update(Long id, MenuItemAddOnRequest request) {
        AddOn addOn = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Add-on not found: " + id));
        if (request.getName() != null && repository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new IllegalArgumentException("Another add-on with this name already exists");
        }
        apply(addOn, request);
        if (addOn.getName() == null || addOn.getName().isBlank() || addOn.getName().length() > 120) {
            throw new IllegalArgumentException("Add-on name is required and must be at most 120 characters");
        }
        if (addOn.getPrice() == null || addOn.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Add-on price must be greater than zero");
        }
        if (addOn.getDescription() != null && addOn.getDescription().length() > 500) {
            throw new IllegalArgumentException("Add-on description cannot exceed 500 characters");
        }
        if (request.getDietaryType() != null) {
            addOn.setDietaryType(normalizeDietaryType(request.getDietaryType()));
        }
        return toResponse(repository.save(addOn));
    }

    private void validate(MenuItemAddOnRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getName().trim().length() > 120) {
            throw new IllegalArgumentException("Add-on name is required and must be at most 120 characters");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Add-on price must be greater than zero");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new IllegalArgumentException("Add-on description cannot exceed 500 characters");
        }
        if (request.getDietaryType() != null && !request.getDietaryType().isBlank()) {
            normalizeDietaryType(request.getDietaryType());
        }
    }

    private void apply(AddOn addOn, MenuItemAddOnRequest request) {
        if (request.getName() != null) addOn.setName(request.getName().trim());
        if (request.getPrice() != null) addOn.setPrice(request.getPrice());
        if (request.getDescription() != null) addOn.setDescription(request.getDescription().trim());
        if (request.getDietaryType() != null) {
            addOn.setDietaryType(normalizeDietaryType(request.getDietaryType()));
        }
        if (request.getAvailable() != null) addOn.setAvailable(request.getAvailable());
    }

    private String normalizeDietaryType(String dietaryType) {
        String normalized = dietaryType.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("veg", "non-veg").contains(normalized)) {
            throw new IllegalArgumentException("Add-on type must be veg or non-veg");
        }
        return normalized;
    }

    private AddOnResponse toResponse(AddOn addOn) {
        AddOnResponse response = new AddOnResponse();
        response.setId(addOn.getId());
        response.setName(addOn.getName());
        response.setDescription(addOn.getDescription());
        response.setPrice(addOn.getPrice());
        response.setDietaryType(addOn.getDietaryType());
        response.setAvailable(addOn.isAvailable());
        return response;
    }
}
