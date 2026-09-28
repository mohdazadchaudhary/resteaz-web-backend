package com.restaurant.backend.service;

import com.restaurant.backend.dto.MenuItemAddOnRequest;
import com.restaurant.backend.repository.AddOnRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AddOnServiceTest {
    private final AddOnRepository repository = mock(AddOnRepository.class);
    private final AddOnService service = new AddOnService(repository);

    @Test
    void rejectsBlankNamesAndNonPositivePrices() {
        MenuItemAddOnRequest request = new MenuItemAddOnRequest();
        request.setName("  ");
        request.setPrice(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        request.setName("Cheese");
        request.setPrice(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        request.setPrice(new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    @Test
    void rejectsDuplicateAddOnNamesIgnoringCase() {
        MenuItemAddOnRequest request = new MenuItemAddOnRequest();
        request.setName(" Extra Cheese ");
        request.setPrice(new BigDecimal("30.00"));
        when(repository.existsByNameIgnoreCase("Extra Cheese")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    @Test
    void persistsVegAndNonVegTypes() {
        MenuItemAddOnRequest request = new MenuItemAddOnRequest();
        request.setName("Cheese");
        request.setPrice(new BigDecimal("30.00"));
        request.setDietaryType("veg");
        when(repository.save(any())).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, com.restaurant.backend.entity.AddOn.class);
            saved.setId(1L);
            return saved;
        });

        assertEquals("veg", service.create(request).getDietaryType());

        request.setName("Chicken");
        request.setDietaryType("non-veg");
        assertEquals("non-veg", service.create(request).getDietaryType());
    }
}
