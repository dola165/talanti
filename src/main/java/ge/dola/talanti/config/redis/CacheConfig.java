//package ge.dola.talanti.config.redis;
//
//import lombok.extern.slf4j.Slf4j;
//import org.jspecify.annotations.NonNull;
//import org.springframework.cache.Cache;
//import org.springframework.cache.annotation.CachingConfigurer;
//import org.springframework.cache.interceptor.CacheErrorHandler;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@Configuration
//public class CacheConfig implements CachingConfigurer {
//
//    @Override
//    public CacheErrorHandler errorHandler() {
//        return new CacheErrorHandler() {
//            @Override
//            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
//                log.warn("Redis GET error! Falling back to database. Cache: {}, Key: {}", cache.getName(), key);
//            }
//
//            @Override
//            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
//                log.warn("Redis PUT error! Data not cached. Cache: {}, Key: {}", cache.getName(), key);
//            }
//
//            @Override
//            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
//                log.warn("Redis EVICT error! Cache may be stale. Cache: {}, Key: {}", cache.getName(), key);
//            }
//
//            @Override
//            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
//                log.warn("Redis CLEAR error! Cache: {}", cache.getName());
//            }
//        };
//    }
//}