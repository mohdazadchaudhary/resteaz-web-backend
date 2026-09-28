package com.restaurant.backend.controller;

import com.restaurant.backend.dto.MenuItemAvailabilityRequest;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.service.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getMenu() {
        List<MenuItemResponse> menuItems = menuService.getAllMenuItems();
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMenuItemById(@PathVariable Long id) {
        try {
            MenuItemResponse menuItem = menuService.getMenuItemById(id);
            return ResponseEntity.ok(menuItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createMenuItem(@RequestBody MenuItemRequest request) {
        try {
            MenuItemResponse menuItem = menuService.createMenuItem(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(menuItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMenuItem(@PathVariable Long id, @RequestBody MenuItemRequest request) {
        try {
            MenuItemResponse menuItem = menuService.updateMenuItem(id, request);
            return ResponseEntity.ok(menuItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMenuItem(@PathVariable Long id) {
        try {
            menuService.deleteMenuItem(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<?> updateMenuItemAvailability(@PathVariable Long id, @RequestBody MenuItemAvailabilityRequest request) {
        try {
            MenuItemResponse menuItem = menuService.updateMenuItemAvailability(id, request);
            return ResponseEntity.ok(menuItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}