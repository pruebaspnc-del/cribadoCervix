package  cribadocervix

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ImportResource
import org.springframework.transaction.annotation.EnableTransactionManagement


@SpringBootApplication(scanBasePackages = ["base", "es.rm", " jade", "asset.pipeline.springboot"])
@ImportResource("classpath:beans.groovy")
@EnableTransactionManagement
class Application extends SpringBootServletInitializer implements ApplicationRunner {
	
	@Autowired
	BootStrap bootStrap
	
	static void main(String[] args) {
		SpringApplication.run Application, args
	}
	
	void run(ApplicationArguments args) throws Exception {
		bootStrap.init()
	}
	
}



