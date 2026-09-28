package com.restaurant.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class MenuImageStorageService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 6000;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;

    private final Path storageDirectory;

    public MenuImageStorageService(
            @Value("${app.menu-image.storage-dir:uploads/menu-items}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller");
        }

        String extension = getValidatedExtension(file);
        Files.createDirectories(storageDirectory);
        String filename = UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) {
            Files.copy(input, storageDirectory.resolve(filename));
        }
        return "/uploads/menu-items/" + filename;
    }

    private String getValidatedExtension(MultipartFile file) throws IOException {
        try (var fileInput = file.getInputStream();
             ImageInputStream input = ImageIO.createImageInputStream(fileInput)) {
            if (input == null) {
                throw new IllegalArgumentException("Uploaded file is not a supported image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG, PNG, and GIF images are supported");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions exceed the supported limit");
                }

                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                return switch (format) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> throw new IllegalArgumentException("Only JPEG, PNG, and GIF images are supported");
                };
            } finally {
                reader.dispose();
            }
        }
    }
}
