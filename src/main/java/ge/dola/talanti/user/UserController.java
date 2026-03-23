package ge.dola.talanti.user;

import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.user.dto.CompleteProfileDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.dto.UpdateCurrentUserProfileDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.util.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    // 🛡️ ONLY the Service is injected. No Repositories, no DSLContext.
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserProfileDto> getPublicProfile(@PathVariable @Positive Long id) {
        Long currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        return ResponseEntity.ok(userService.getProfile(id, currentUserId));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> toggleFollow(@PathVariable @Positive Long id) {
        boolean isFollowed = userService.toggleFollow(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("following", isFollowed));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResult<UserSearchDto>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(userService.searchUsers(query, page, size));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody CompleteProfileDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("HTTP PUT /me/profile triggered by User ID: {}", currentUserId);

        userService.completeUserProfile(currentUserId, dto);
        return ResponseEntity.ok(Map.of("message", "Profile completed successfully"));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PublicUserProfileDto> updateMyProfile(@Valid @RequestBody UpdateCurrentUserProfileDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateMyProfile(currentUserId, dto));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMe() {
        Map<String, Object> myProfileData = userService.getMyProfileData(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(myProfileData);
    }
}
