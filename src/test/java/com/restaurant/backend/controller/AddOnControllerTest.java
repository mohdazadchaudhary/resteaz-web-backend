package com.restaurant.backend.controller;

import com.restaurant.backend.dto.AddOnResponse;
import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.service.AddOnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddOnControllerTest {
    private AddOnService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AddOnService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AddOnController(service)).build();
    }

    @Test
    void listsReusableAddOnsThroughMenuApi() throws Exception {
        AddOnResponse response = response(7L, "Extra Cheese", "30.00", true);
        when(service.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/add-ons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Extra Cheese"))
                .andExpect(jsonPath("$[0].price").value(30.0))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void createsAddOnThroughMenuApi() throws Exception {
        when(service.create(any(MenuItemAddOnRequest.class)))
                .thenReturn(response(8L, "Mayonnaise", "20.00", true));

        mockMvc.perform(post("/api/add-ons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mayonnaise","price":20.00,"available":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Mayonnaise"))
                .andExpect(jsonPath("$.price").value(20.0));
    }

    private AddOnResponse response(Long id, String name, String price, boolean available) {
        AddOnResponse response = new AddOnResponse();
        response.setId(id);
        response.setName(name);
        response.setPrice(new BigDecimal(price));
        response.setAvailable(available);
        return response;
    }
}
