package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType; // <-- Ensure this is imported
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // UPDATE THIS LINE:
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaDto> uploadMedia(
            @RequestPart("file") MultipartFile file, // Changed @RequestParam to @RequestPart
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            Long userId = currentUser != null ? currentUser.getId() : 1L;

            MediaDto result = mediaService.uploadFile(file, userId);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}