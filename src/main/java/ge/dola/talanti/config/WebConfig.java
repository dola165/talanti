package ge.dola.talanti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the URL "/uploads/..." directly to the "uploads" folder in your project root
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}