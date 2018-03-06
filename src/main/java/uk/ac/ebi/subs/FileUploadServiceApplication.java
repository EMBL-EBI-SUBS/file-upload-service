package uk.ac.ebi.subs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;

@SpringBootApplication
public class FileUploadServiceApplication {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(FileUploadServiceApplication.class);
		ApplicationPidFileWriter applicationPidFileWriter = new ApplicationPidFileWriter();
		springApplication.addListeners( applicationPidFileWriter );
		springApplication.run(args);
	}
}
