-- 1. Create the Post Tags table (The core of the scouting engine)
CREATE TABLE public.post_tags (
                                  post_id BIGINT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
                                  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (post_id, user_id)
);

-- Index for faster lookups when loading a player's Match Feed
CREATE INDEX idx_post_tags_user_id ON public.post_tags(user_id);