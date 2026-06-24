package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class FileDownloadClientTest {

    @Test
    void downloadsFileByPostBody() {
        ZtbProperties properties = new ZtbProperties();
        properties.getFileService().setDownloadUrlTemplate("http://file-service/download/{fileId}");
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        FileDownloadClient client = new FileDownloadClient(restTemplate, properties, new ObjectMapper());
        server.expect(requestTo("http://file-service/download/file-1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "fileId": "file-1",
                          "fileName": "投标文件.docx",
                          "projectId": "project-1"
                        }
                        """))
                .andRespond(withSuccess(
                        new ByteArrayResource(new byte[] {1, 2, 3}),
                        MediaType.APPLICATION_OCTET_STREAM));

        byte[] bytes = client.download("file-1", "投标文件.docx", "project-1");

        assertThat(bytes).containsExactly(1, 2, 3);
        server.verify();
    }

    @Test
    void followsPermanentRedirectWithPostBody() {
        ZtbProperties properties = new ZtbProperties();
        properties.getFileService().setDownloadUrlTemplate("http://file-service/old/{fileId}");
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        FileDownloadClient client = new FileDownloadClient(restTemplate, properties, new ObjectMapper());
        server.expect(requestTo("http://file-service/old/file-1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "fileId": "file-1",
                          "fileName": "投标文件.docx",
                          "projectId": "project-1"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION, "http://file-service/new/file-1"));
        server.expect(requestTo(URI.create("http://file-service/new/file-1")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "fileId": "file-1",
                          "fileName": "投标文件.docx",
                          "projectId": "project-1"
                        }
                        """))
                .andRespond(withSuccess(
                        new ByteArrayResource(new byte[] {4, 5, 6}),
                        MediaType.APPLICATION_OCTET_STREAM));

        byte[] bytes = client.download("file-1", "投标文件.docx", "project-1");

        assertThat(bytes).containsExactly(4, 5, 6);
        server.verify();
    }

    @Test
    void includesErrorResponseBodyWhenFileServiceRejectsRequest() {
        ZtbProperties properties = new ZtbProperties();
        properties.getFileService().setDownloadUrlTemplate("http://file-service/download");
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        FileDownloadClient client = new FileDownloadClient(restTemplate, properties, new ObjectMapper());
        server.expect(requestTo("http://file-service/download"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "fileId": "file-1",
                          "fileName": "投标文件.docx",
                          "projectId": "project-1"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("no body"));

        assertThatThrownBy(() -> client.download("file-1", "投标文件.docx", "project-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("no body");
        server.verify();
    }

    private RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return restTemplate;
    }
}
