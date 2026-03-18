package ge.dola.talanti.media;

import ge.dola.talanti.media.dto.MediaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(value = "context", defaultValue = "general") String context) throws Exception {

        // STRICT ENFORCEMENT: Context resolution delegated to the Service layer
        return ResponseEntity.ok(mediaService.uploadFile(file, context));
    }
}