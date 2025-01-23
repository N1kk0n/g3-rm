package g3.rm.resourcemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResourceManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceManagerApplication.class, args);
	}

}
