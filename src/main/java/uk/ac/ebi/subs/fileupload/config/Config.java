package uk.ac.ebi.subs.fileupload.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration("FileUploadServiceConfiguration")
public class Config {

    @Bean
    public RestTemplate globusRestTemplate() {
        return new RestTemplateBuilder()
                .errorHandler(new DefaultResponseErrorHandler(){
                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                    }
                })
                .setReadTimeout(30_000)
                .setConnectTimeout(30_000).build();
    }
}
