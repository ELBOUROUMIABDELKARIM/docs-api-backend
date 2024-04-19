package fr.norsys.docsapi;

import fr.norsys.docsapi.utils.DocumentStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		DocumentStorageProperties.class,
})
public class DocsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocsApiApplication.class, args);
	}

}
