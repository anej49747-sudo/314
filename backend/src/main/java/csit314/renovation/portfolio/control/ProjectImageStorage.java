package csit314.renovation.portfolio.control;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Local development storage; the API and database store opaque keys, not client filenames. */
@Component
public class ProjectImageStorage {
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_IMAGES = 6;
    private final Path root;

    public ProjectImageStorage(@Value("${app.project-images.directory:${user.home}/.csit314/project-images}")
            String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public record ValidatedImage(byte[] bytes, String contentType, String extension) { }
    public record StoredImage(String key, String contentType, long sizeBytes) { }

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Each image must be between 1 byte and 5 MB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("Each image must be between 1 byte and 5 MB");
            }
            String detectedType;
            String extension;
            if (bytes.length >= 3 && (bytes[0] & 255) == 0xff
                    && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) {
                detectedType = "image/jpeg";
                extension = ".jpg";
            } else if (bytes.length >= 8 && (bytes[0] & 255) == 0x89
                    && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
                detectedType = "image/png";
                extension = ".png";
            } else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                    && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                    && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                long riffSize = ((long) bytes[4] & 255)
                        | (((long) bytes[5] & 255) << 8)
                        | (((long) bytes[6] & 255) << 16)
                        | (((long) bytes[7] & 255) << 24);
                if (bytes.length < 16 || riffSize + 8 != bytes.length
                        || !(matches(bytes, 12, "VP8 ") || matches(bytes, 12, "VP8L")
                                || matches(bytes, 12, "VP8X"))) {
                    throw new IllegalArgumentException("Invalid WebP image");
                }
                detectedType = "image/webp";
                extension = ".webp";
            } else {
                throw new IllegalArgumentException("Images must be JPG, PNG or WebP files");
            }
            if (!detectedType.equals(file.getContentType())) {
                throw new IllegalArgumentException("Image contents must match the selected file format");
            }
            if (!"image/webp".equals(detectedType)) {
                try {
                    var decoded = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (decoded == null || decoded.getWidth() < 1 || decoded.getHeight() < 1) {
                        throw new IllegalArgumentException("Invalid image file");
                    }
                } catch (IOException invalidImage) {
                    throw new IllegalArgumentException("Invalid image file");
                }
            }
            return new ValidatedImage(bytes, detectedType, extension);
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to read image", error);
        }
    }

    private static boolean matches(byte[] bytes, int offset, String marker) {
        for (int index = 0; index < marker.length(); index++) {
            if (bytes[offset + index] != marker.charAt(index)) return false;
        }
        return true;
    }

    public StoredImage store(ValidatedImage image) {
        String key = UUID.randomUUID() + image.extension();
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(key), image.bytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to store image", error);
        }
        return new StoredImage(key, image.contentType(), image.bytes().length);
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(safePath(key));
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to retrieve image", error);
        }
    }

    public void deleteQuietly(String key) {
        try { Files.deleteIfExists(safePath(key)); }
        catch (IOException ignored) { /* best-effort rollback cleanup */ }
    }

    private Path safePath(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("Invalid image key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid image key");
        return path;
    }
}
