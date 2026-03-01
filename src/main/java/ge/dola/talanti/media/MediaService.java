package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;

    // The folder where files will be saved in your project directory
    private final String UPLOAD_DIR = "uploads/";

    public MediaDto uploadFile(MultipartFile file, Long uploaderId) throws IOException {
        // 1. Ensure the "uploads" directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Generate a unique file name so uploads don't overwrite each other
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 3. Save the actual file to your hard drive
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        // 4. Generate the public URL that React will use to display it
        String fileUrl = "/uploads/" + uniqueFilename;

        // 5. Save the record in the database
        Long mediaId = mediaRepository.saveMedia(fileUrl, file.getContentType(), file.getSize(), uploaderId);

        return new MediaDto(mediaId, fileUrl, file.getContentType());
    }
}