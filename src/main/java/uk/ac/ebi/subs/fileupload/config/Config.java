package uk.ac.ebi.subs.fileupload.config;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration("FileUploadServiceConfiguration")
public class Config {

    @Value("${file-upload.globus.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${file-upload.globus.proxy.port:#{null}}")
    private Integer proxyPort;

    @Bean
    public RestTemplate globusRestTemplate() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        if (proxyHost != null && proxyPort != null) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(HttpClientBuilder.create().setProxy(proxy).build());

            restTemplateBuilder.requestFactory(requestFactory);
        }

        return restTemplateBuilder
                .errorHandler(new DefaultResponseErrorHandler(){
                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                    }
                })
                .setReadTimeout(30_000)
                .setConnectTimeout(30_000).build();
    }
}
