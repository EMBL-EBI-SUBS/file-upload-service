package uk.ac.ebi.subs.fileupload.config;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration("FileUploadServiceConfiguration")
public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    @Value("${file-upload.globus.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${file-upload.globus.proxy.port:#{null}}")
    private Integer proxyPort;

    @Bean
    public RestTemplate globusRestTemplate() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        if (proxyHost != null && !proxyHost.isBlank() && proxyPort != null) {
            LOGGER.info("Applying proxy settings to Globus REST client. Host : {}, Port : {}", proxyHost, proxyPort);

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            requestFactory.setOutputStreaming(false);

            restTemplateBuilder = restTemplateBuilder.requestFactory(requestFactory);
        }

        RestTemplate res = restTemplateBuilder
                .errorHandler(new DefaultResponseErrorHandler(){
                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                    }
                })
                .setReadTimeout(15_000)
                .setConnectTimeout(15_000).build();

        return res;
    }
}
