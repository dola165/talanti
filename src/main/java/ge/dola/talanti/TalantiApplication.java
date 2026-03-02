package ge.dola.talanti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TalantiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalantiApplication.class, args);
	}

}
