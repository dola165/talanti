package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import ge.dola.talanti.media.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StorageProvider storageProvider;

    private static final Set<String> ALLOWED_IMAGE_MIME_PREFIXES = Set.of("image/");

    public MediaDto uploadFile(MultipartFile file, Long uploaderId, String context) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!isImage(file)) {
            throw new IllegalArgumentException("Invalid file type. Only true images are allowed.");
        }

        byte[] optimizedBytes = optimizeImage(file, context);
        InputStream inputStream = new ByteArrayInputStream(optimizedBytes);

        // CHANGE: Force .jpg extension instead of .webp
        String uniqueFilename = UUID.randomUUID().toString() + ".jpg";

        // CHANGE: Delegate to the Storage Provider with image/jpeg
        String fileUrl = storageProvider.store(inputStream, uniqueFilename, "image/jpeg");

        // CHANGE: Save metadata to DB as image/jpeg
        Long mediaId = mediaRepository.saveMedia(fileUrl, "image/jpeg", optimizedBytes.length, uploaderId);

        return new MediaDto(mediaId, fileUrl, "image/jpeg");
    }

    private byte[] optimizeImage(MultipartFile file, String context) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int width = "banner".equalsIgnoreCase(context) ? 1200 : 400;
        int height = "banner".equalsIgnoreCase(context) ? 400 : 400;

        Thumbnails.of(file.getInputStream())
                .size(width, height)
                .outputFormat("jpg") // CHANGE: Use highly-supported jpg encoder
                .outputQuality(0.85)
                .toOutputStream(os);

        return os.toByteArray();
    }

    private boolean isImage(MultipartFile file) {
        String ct = file.getContentType();
        boolean looksLikeImage = (ct != null && ALLOWED_IMAGE_MIME_PREFIXES.stream().anyMatch(ct::startsWith));
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            return ImageIO.read(in) != null && looksLikeImage;
        } catch (IOException e) {
            return false;
        }
    }
}