package com.linzu.common.storage;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储。演示环境保存到 UPLOAD_DIR，由 WebMvcConfig 映射为公开静态路径。
 */
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg",
        "jpg",
        "image/jpg",
        "jpg",
        "image/png",
        "png",
        "image/gif",
        "gif",
        "image/webp",
        "webp"
    );
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern(
        "yyyy/MM",
        Locale.ROOT
    );

    private final Path root;
    private final long maxAvatarBytes;
    private final String publicPrefix;

    public FileStorageService(
        @Value("${app.upload.dir:./uploads}") String uploadDir,
        @Value("${app.upload.max-avatar-bytes:2097152}") long maxAvatarBytes,
        @Value("${app.upload.public-prefix:/uploads}") String publicPrefix
    ) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxAvatarBytes = maxAvatarBytes;
        this.publicPrefix = publicPrefix.replaceAll("/+$", "");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录：" + root, e);
        }
    }

    public String storeAvatar(MultipartFile file) {
        return storeImage(file, "avatars");
    }

    /**
     * 通用图片存储，folder 决定子目录，例如 avatars / items。
     */
    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择图片文件");
        }
        if (file.getSize() > maxAvatarBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType() == null
            ? ""
            : file.getContentType().toLowerCase(Locale.ROOT);
        int parameterIndex = contentType.indexOf(';');
        if (parameterIndex >= 0) {
            contentType = contentType.substring(0, parameterIndex).trim();
        }
        String extension = EXTENSIONS.get(contentType);
        if (extension == null || !matchesImageSignature(file, contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String safeFolder = folder == null || folder.isBlank()
            ? "misc"
            : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String relativePath =
            safeFolder + "/" + LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件路径不合法");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("头像保存失败", e);
        }
        return publicPrefix + "/" + relativePath;
    }

    private boolean matchesImageSignature(MultipartFile file, String contentType) {
        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException e) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> startsWith(
                header,
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }
            );
            case "image/png" -> startsWith(
                header,
                new byte[] {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A,
                }
            );
            case "image/gif" -> startsWith(header, "GIF8".getBytes(StandardCharsets.US_ASCII));
            case "image/webp" -> (
                header.length >= 12 &&
                startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII)) &&
                Arrays.equals(
                    Arrays.copyOfRange(header, 8, 12),
                    "WEBP".getBytes(StandardCharsets.US_ASCII)
                )
            );
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }
}
