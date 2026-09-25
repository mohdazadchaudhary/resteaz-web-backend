package com.restaurant.backend.controller;

import com.restaurant.backend.dto.TableRequest;
import com.restaurant.backend.dto.TableResponse;
import com.restaurant.backend.dto.TableStatusRequest;
import com.restaurant.backend.service.TableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables() {
        List<TableResponse> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    @PostMapping
    public ResponseEntity<?> createTable(@RequestBody TableRequest request) {
        try {
            TableResponse table = tableService.createTable(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(table);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{qrToken}")
    public ResponseEntity<?> getTableByQrToken(@PathVariable String qrToken) {
        try {
            TableResponse table = tableService.getTableByQrToken(qrToken);
            return ResponseEntity.ok(table);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTableStatus(@PathVariable Long id, @RequestBody TableStatusRequest request) {
        try {
            TableResponse table = tableService.updateTableStatus(id, request);
            return ResponseEntity.ok(table);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}