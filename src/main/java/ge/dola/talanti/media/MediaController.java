package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaDto> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "context", defaultValue = "general") String context,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            Long userId = currentUser != null ? currentUser.getId() : 1L;

            // Pass the context to the service
            MediaDto result = mediaService.uploadFile(file, userId, context);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace(); // Log this properly in production
            return ResponseEntity.internalServerError().build();
        }
    }
}