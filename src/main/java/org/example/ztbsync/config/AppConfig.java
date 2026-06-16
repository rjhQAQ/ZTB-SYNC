package org.example.ztbsync.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ZtbProperties.class)
public class AppConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder, ZtbProperties properties) {
        return builder
                .requestFactory(() -> requestFactory(properties.getFileService().isTrustAllSsl()))
                .errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) {
                        return false;
                    }
                })
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(boolean trustAllSsl) {
        SSLSocketFactory socketFactory = trustAllSsl ? trustAllSslSocketFactory() : null;
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        return new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
                if (trustAllSsl && connection instanceof HttpsURLConnection httpsConnection) {
                    httpsConnection.setSSLSocketFactory(socketFactory);
                    httpsConnection.setHostnameVerifier(hostnameVerifier);
                }
            }
        };
    }

    private SSLSocketFactory trustAllSslSocketFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {trustAllManager()}, new SecureRandom());
            return context.getSocketFactory();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("初始化文件下载 HTTPS 信任配置失败", exception);
        }
    }

    private X509TrustManager trustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
