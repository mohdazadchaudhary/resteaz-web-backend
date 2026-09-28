package com.restaurant.backend.controller;

import com.restaurant.backend.service.MenuImageStorageService;
import com.restaurant.backend.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuControllerTest {
    private MenuService menuService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        menuService = mock(MenuService.class);
        MenuImageStorageService imageStorageService = mock(MenuImageStorageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MenuController(menuService, imageStorageService)).build();
    }

    @Test
    void explainsWhenOrderHistoryPreventsMenuItemDeletion() throws Exception {
        doThrow(new DataIntegrityViolationException("foreign key violation"))
                .when(menuService).deleteMenuItem(9L);

        mockMvc.perform(delete("/api/menu/9"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "This menu item is referenced by existing orders and cannot be deleted. "
                                + "Disable it instead to keep order history intact."));
    }
}
