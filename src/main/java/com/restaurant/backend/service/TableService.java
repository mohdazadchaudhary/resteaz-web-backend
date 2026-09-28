package com.restaurant.backend.service;

import com.restaurant.backend.dto.TableRequest;
import com.restaurant.backend.dto.TableResponse;
import com.restaurant.backend.dto.TableStatusRequest;
import com.restaurant.backend.entity.Restaurant;
import com.restaurant.backend.entity.RestaurantTable;
import com.restaurant.backend.repository.RestaurantRepository;
import com.restaurant.backend.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantRepository restaurantRepository;

    public TableService(RestaurantTableRepository tableRepository, RestaurantRepository restaurantRepository) {
        this.tableRepository = tableRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public List<TableResponse> getAllTables() {
        return tableRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TableResponse createTable(TableRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + request.getRestaurantId()));

        RestaurantTable table = new RestaurantTable();
        table.setRestaurant(restaurant);
        table.setTableNumber(request.getTableNumber());
        table.setQrToken(generateUniqueQrToken());
        table.setActive(true);

        RestaurantTable savedTable = tableRepository.save(table);
        return mapToResponse(savedTable);
    }

    public TableResponse getTableByQrToken(String qrToken) {
        RestaurantTable table = tableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR token"));

        if (!table.isActive()) {
            throw new IllegalStateException("Table is currently unavailable");
        }
        if (table.getRestaurant() == null || !table.getRestaurant().isActive()) {
            throw new IllegalStateException("Restaurant is currently unavailable");
        }

        return mapToResponse(table);
    }

    @Transactional
    public TableResponse updateTableStatus(Long id, TableStatusRequest request) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found with id: " + id));

        table.setActive(request.isActive());
        RestaurantTable savedTable = tableRepository.save(table);
        return mapToResponse(savedTable);
    }

    private String generateUniqueQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (tableRepository.findByQrToken(token).isPresent());
        return token;
    }

    private TableResponse mapToResponse(RestaurantTable table) {
        TableResponse response = new TableResponse();
        response.setId(table.getId());
        response.setTableNumber(table.getTableNumber());
        response.setQrToken(table.getQrToken());
        response.setActive(table.isActive());
        response.setRestaurant(table.getRestaurant());
        return response;
    }
}
