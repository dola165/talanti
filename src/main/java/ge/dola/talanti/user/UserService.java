package ge.dola.talanti.user;

import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public PublicUserProfileDto getProfile(Long targetUserId, Long currentUserId) {
        return userProfileRepository.getPublicProfile(targetUserId, currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public boolean toggleFollow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        boolean isFollowing = userRepository.isFollowingUser(currentUserId, targetUserId);

        if (isFollowing) {
            userRepository.unfollowUser(currentUserId, targetUserId);
            return false;
        } else {
            userRepository.followUser(currentUserId, targetUserId);
            return true;
        }
    }
}