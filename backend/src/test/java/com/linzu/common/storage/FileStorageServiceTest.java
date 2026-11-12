package com.linzu.common.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.linzu.common.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    private static final byte[] PNG = {
        (byte) 0x89,
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
        0x00,
        0x00,
        0x00,
        0x0D,
    };

    @TempDir
    Path tempDir;

    @Test
    void storesAvatarUnderPublicPrefix() throws Exception {
        FileStorageService service = new FileStorageService(
            tempDir.toString(),
            1024,
            "/uploads"
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            PNG
        );

        String url = service.storeAvatar(file);

        assertTrue(url.startsWith("/uploads/avatars/"));
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.exists(stored));
    }

    @Test
    void rejectsWrongContentType() {
        FileStorageService service = new FileStorageService(
            tempDir.toString(),
            1024,
            "/uploads"
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.svg",
            "image/svg+xml",
            "<svg/>".getBytes()
        );

        assertThrows(BusinessException.class, () -> service.storeAvatar(file));
    }

    @Test
    void rejectsSpoofedImageContent() {
        FileStorageService service = new FileStorageService(
            tempDir.toString(),
            1024,
            "/uploads"
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            "not-an-image".getBytes()
        );

        assertThrows(BusinessException.class, () -> service.storeAvatar(file));
    }

    @Test
    void rejectsOversizedFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 4, "/uploads");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            PNG
        );

        assertEquals(12, PNG.length);
        assertThrows(BusinessException.class, () -> service.storeAvatar(file));
    }
}
