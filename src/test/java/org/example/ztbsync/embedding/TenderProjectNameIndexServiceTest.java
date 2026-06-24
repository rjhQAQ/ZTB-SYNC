package org.example.ztbsync.embedding;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.extraction.TenderExtraction;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class TenderProjectNameIndexServiceTest {

    @Test
    void createsIndexWithIkAnalyzerAndWritesProjectNameDocument() {
        ZtbProperties properties = properties();
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TenderProjectNameIndexService service = new TenderProjectNameIndexService(properties, restTemplate);

        server.expect(requestTo("http://es/ztb_tender_project_name"))
                .andExpect(method(HttpMethod.HEAD))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://es/ztb_tender_project_name"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"analyzer\":\"ik_max_word\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"search_analyzer\":\"ik_smart\"")))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://es/ztb_tender_project_name/_doc/project-1_file-1?refresh=true"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"projectId\":\"project-1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"fileId\":\"file-1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"projectName\":\"smart park project\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"projectNameKeyword\":\"smart park project\"")))
                .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

        service.index(task(), extraction("smart park project"));

        server.verify();
    }

    @Test
    void skipsBlankProjectName() {
        ZtbProperties properties = properties();
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TenderProjectNameIndexService service = new TenderProjectNameIndexService(properties, restTemplate);

        service.index(task(), extraction(" "));

        server.verify();
    }

    @Test
    void throwsWhenEsWriteFails() {
        ZtbProperties properties = properties();
        properties.getElasticsearch().setAutoCreateIndex(false);
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TenderProjectNameIndexService service = new TenderProjectNameIndexService(properties, restTemplate);
        server.expect(requestTo("http://es/ztb_tender_project_name/_doc/project-1_file-1?refresh=true"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.index(task(), extraction("smart park project")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("写入招标项目名称 ES 索引失败");
        server.verify();
    }

    private ZtbProperties properties() {
        ZtbProperties properties = new ZtbProperties();
        properties.getElasticsearch().setBaseUrl("http://es");
        properties.getElasticsearch().setProjectNameIndexName("ztb_tender_project_name");
        properties.getElasticsearch().setProjectNameAnalyzer("ik_max_word");
        properties.getElasticsearch().setProjectNameSearchAnalyzer("ik_smart");
        return properties;
    }

    private FileProcessingTask task() {
        FileProcessingTask task = new FileProcessingTask();
        task.setTaskId("task-1");
        task.setProjectId("project-1");
        task.setFileId("file-1");
        task.setFileName("tender.docx");
        return task;
    }

    private TenderExtraction extraction(String projectName) {
        TenderExtraction extraction = new TenderExtraction();
        extraction.setProjectName(projectName);
        extraction.setTenderCompanyName("tender company");
        extraction.setAgencyName("agency");
        return extraction;
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
