package com.restaurant.backend.controller;

import com.restaurant.backend.dto.AddOnResponse;
import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.service.AddOnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/add-ons")
@CrossOrigin(origins = "*")
public class AddOnController {
    private final AddOnService service;

    public AddOnController(AddOnService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddOnResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MenuItemAddOnRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MenuItemAddOnRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
