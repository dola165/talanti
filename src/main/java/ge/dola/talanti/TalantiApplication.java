package ge.dola.talanti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
//@EnableCaching
public class TalantiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalantiApplication.class, args);
	}

}
