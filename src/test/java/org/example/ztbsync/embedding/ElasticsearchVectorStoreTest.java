package org.example.ztbsync.embedding;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.rag.RagChunk;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class ElasticsearchVectorStoreTest {

    @Test
    void createsIndexDeletesOldChunksAndBulkWritesCurrentChunks() {
        ZtbProperties properties = new ZtbProperties();
        properties.getElasticsearch().setBaseUrl("http://es");
        properties.getElasticsearch().setIndexName("ztb_file_embedding");
        properties.getEmbedding().setVectorDims(3);
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ElasticsearchVectorStore store = new ElasticsearchVectorStore(
                properties, restTemplate, new ObjectMapper(), new RagEmbeddingTextBuilder());
        FileProcessingTask task = task();

        server.expect(requestTo("http://es/ztb_file_embedding"))
                .andExpect(method(HttpMethod.HEAD))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://es/ztb_file_embedding"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"dense_vector\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"dims\":3")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"analyzer\":\"ik_max_word\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sectionPathText\"")))
                .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://es/ztb_file_embedding/_delete_by_query?conflicts=proceed"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"fileId\":\"file-1\"")))
                .andRespond(withSuccess("{\"deleted\":1}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://es/_bulk?refresh=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"chunkText\":\"project-content\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"embeddingText\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chapter-one")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"embedding\":[0.1,0.2,0.3]")))
                .andRespond(withSuccess("{\"errors\":false}", MediaType.APPLICATION_JSON));

        store.replaceFileChunks(
                task,
                List.of(new RagChunk(0, "project-content", "chapter-one", List.of("PARAGRAPH"), false, 0, 15)),
                List.of(List.of(0.1, 0.2, 0.3)));

        server.verify();
    }

    private FileProcessingTask task() {
        FileProcessingTask task = new FileProcessingTask();
        task.setProjectId("project-1");
        task.setFileId("file-1");
        task.setFileName("bid.docx");
        task.setFileType("BID");
        task.setTaskId("task-1");
        return task;
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
