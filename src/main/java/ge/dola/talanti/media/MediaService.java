package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final String UPLOAD_DIR = "uploads/";

    // Keep this tight; add only what you truly support. Ported from your old code!
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_IMAGE_MIME_PREFIXES = Set.of("image/");

    public MediaDto uploadFile(MultipartFile file, Long uploaderId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // 1. DEEP SECURITY CHECK (From your Grasskicks FileStorageService)
        if (!isImage(file)) {
            throw new IllegalArgumentException("Invalid file type. Only true images are allowed.");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        // 2. Safe Extension Extraction
        String ext = safeExtension(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID().toString() + "." + ext;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        String fileUrl = "/uploads/" + uniqueFilename;
        Long mediaId = mediaRepository.saveMedia(fileUrl, file.getContentType(), file.getSize(), uploaderId);

        return new MediaDto(mediaId, fileUrl, file.getContentType());
    }

    // --- SECURITY HELPER METHODS PORTED FROM OLD APP ---

    private boolean isImage(MultipartFile file) {
        String ct = file.getContentType();
        boolean looksLikeImage = (ct != null && ALLOWED_IMAGE_MIME_PREFIXES.stream().anyMatch(ct::startsWith));
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            // This physically verifies the file header is a real image
            return ImageIO.read(in) != null && looksLikeImage;
        } catch (IOException e) {
            return false;
        }
    }

    private String safeExtension(String originalName) {
        if (originalName == null) return "bin";
        int dot = originalName.lastIndexOf('.');
        String ext = (dot >= 0 && dot < originalName.length() - 1) ? originalName.substring(dot + 1) : "";
        ext = ext.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); // Strip weird characters
        if (!ALLOWED_EXTENSIONS.contains(ext)) throw new IllegalArgumentException("Unsupported file extension.");
        return ext;
    }
}