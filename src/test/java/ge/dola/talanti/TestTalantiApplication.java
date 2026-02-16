package ge.dola.talanti;

import org.springframework.boot.SpringApplication;

public class TestTalantiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TalantiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
