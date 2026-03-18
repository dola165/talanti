//package ge.dola.talanti.config.redis;
//
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import tools.jackson.databind.ObjectMapper;
//import org.springframework.cache.interceptor.KeyGenerator;
//import org.springframework.util.StringUtils;
//
//import java.time.Duration;
//
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    // 1. Configure the Cache Manager (For @Cacheable annotations)
//    @Bean
//    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
//        // Pass Spring's ObjectMapper into the serializer to fix the 0-arguments error
//        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(objectMapper);
//
//        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(60))
//                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
//                .disableCachingNullValues();
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(defaultConfig)
//                .build();
//    }
//
//    // 2. Configure the RedisTemplate (For manual caching, rate limiting, and Python bot data)
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(objectMapper);
//
//        // Use Strings for Keys
//        template.setKeySerializer(new StringRedisSerializer());
//        // Use JSON for Values
//        template.setValueSerializer(serializer);
//
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(serializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
//
//
//    @Bean("talantiKeyGenerator")
//    public KeyGenerator keyGenerator() {
//        return (target, method, params) -> {
//            // Generates keys like: ClassName_methodName_param1_param2
//            return target.getClass().getSimpleName() + "_"
//                    + method.getName() + "_"
//                    + StringUtils.arrayToDelimitedString(params, "_");
//        };
//    }
//}