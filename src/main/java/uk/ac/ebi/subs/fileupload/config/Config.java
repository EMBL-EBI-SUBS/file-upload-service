package uk.ac.ebi.subs.fileupload.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration("FileUploadServiceConfiguration")
public class Config {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder().setConnectTimeout(30_000).build();
    }
}
