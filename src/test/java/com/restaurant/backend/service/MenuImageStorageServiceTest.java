package com.restaurant.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuImageStorageServiceTest {
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j5fsAAAAASUVORK5CYII=");

    @TempDir
    Path storageDirectory;

    @Test
    void storesSupportedImageWithGeneratedPublicUrl() throws Exception {
        MenuImageStorageService storageService = new MenuImageStorageService(storageDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "dish.png", "image/png", ONE_PIXEL_PNG);

        String imageUrl = storageService.store(file);
        Path storedImage = storageDirectory.resolve(Path.of(imageUrl).getFileName());

        assertEquals("/uploads/menu-items/" + storedImage.getFileName(), imageUrl);
        org.junit.jupiter.api.Assertions.assertArrayEquals(ONE_PIXEL_PNG, Files.readAllBytes(storedImage));
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        MenuImageStorageService storageService = new MenuImageStorageService(storageDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "dish.png", "image/png", "not an image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storageService.store(file));
    }
}
