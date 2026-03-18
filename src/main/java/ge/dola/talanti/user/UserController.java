package ge.dola.talanti.user;

import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.user.dto.CompleteProfileDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.util.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    // 🛡️ ONLY the Service is injected. No Repositories, no DSLContext.
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserProfileDto> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> toggleFollow(@PathVariable Long id) {
        // The GlobalExceptionHandler will now automatically catch any IllegalArgumentException thrown here!
        boolean isFollowed = userService.toggleFollow(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("following", isFollowed));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResult<UserSearchDto>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.searchUsers(query, page, size));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody CompleteProfileDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("HTTP PUT /me/profile triggered by User ID: {}", currentUserId);

        userService.completeUserProfile(currentUserId, dto);
        return ResponseEntity.ok(Map.of("message", "Profile completed successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMe() {
        Map<String, Object> myProfileData = userService.getMyProfileData(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(myProfileData);
    }
}