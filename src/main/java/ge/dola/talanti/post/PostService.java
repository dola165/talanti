package ge.dola.talanti.post;

import ge.dola.talanti.jooq.tables.records.PostsRecord;
import ge.dola.talanti.post.dto.CreatePostDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostsRecord createPost(Long currentUserId, CreatePostDto dto) {
        // You could add validation here: "Is this user actually a member of this clubId?"
        // For the 3-day MVP, we will keep it open and simple.

        return postRepository.createPost(
                currentUserId,
                dto.content(),
                dto.clubId(),
                dto.isPublic()
        );
    }
}