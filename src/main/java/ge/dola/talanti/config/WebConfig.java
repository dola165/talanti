package ge.dola.talanti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Use the 'file:' protocol and ensure the trailing slash is present
        // This tells Spring to look in the project's root directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}