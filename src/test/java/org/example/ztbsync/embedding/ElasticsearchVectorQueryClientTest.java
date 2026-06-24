package org.example.ztbsync.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.rag.RagFileTypeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class ElasticsearchVectorQueryClientTest {

    @Test
    void searchesByProjectAndVectorUsingScriptScore() {
        ZtbProperties properties = properties();
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ElasticsearchVectorQueryClient client = new ElasticsearchVectorQueryClient(properties, restTemplate);

        server.expect(requestTo("http://es/ztb_file_embedding/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"projectId\":\"project-1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cosineSimilarity")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"bge-base-zh-v1.5\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"boilerplate\":false")))
                .andRespond(withSuccess("""
                        {"hits":{"hits":[{"_score":1.82,"_source":{"fileId":"file-1","fileName":"招标文件.docx","fileType":"TENDER","chunkIndex":3,"chunkText":"命中文本","sectionPath":"第三章 > 资格要求"}}]}}
                        """, MediaType.APPLICATION_JSON));

        var hits = client.search("project-1", List.of(0.1, 0.2, 0.3), 3);

        assertThat(hits).singleElement()
                .satisfies(hit -> {
                    assertThat(hit.fileId()).isEqualTo("file-1");
                    assertThat(hit.chunkText()).isEqualTo("命中文本");
                    assertThat(hit.score()).isCloseTo(0.82, within(0.0001));
                });
        server.verify();
    }

    @Test
    void returnsEmptyWhenIndexDoesNotExist() {
        ZtbProperties properties = properties();
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ElasticsearchVectorQueryClient client = new ElasticsearchVectorQueryClient(properties, restTemplate);

        server.expect(requestTo("http://es/ztb_file_embedding/_search"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.search("project-1", List.of(0.1, 0.2, 0.3), 3)).isEmpty();
        server.verify();
    }

    @Test
    void searchesKeywordFieldsWithFileTypeFilter() {
        ZtbProperties properties = properties();
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ElasticsearchVectorQueryClient client = new ElasticsearchVectorQueryClient(properties, restTemplate);

        server.expect(requestTo("http://es/ztb_file_embedding/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"multi_match\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"chunkText^3\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sectionPathText^2\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"fileType\":\"TENDER\"")))
                .andRespond(withSuccess("""
                        {"hits":{"max_score":8.0,"hits":[{"_score":4.0,"_source":{"fileId":"file-1","fileName":"招标文件.docx","fileType":"TENDER","chunkIndex":3,"chunkText":"保证金命中文本","sectionPath":"第二章"}}]}}
                        """, MediaType.APPLICATION_JSON));

        var hits = client.keywordSearch("project-1", "保证金", 10, RagFileTypeFilter.TENDER);

        assertThat(hits).singleElement()
                .satisfies(hit -> {
                    assertThat(hit.fileType()).isEqualTo("TENDER");
                    assertThat(hit.score()).isCloseTo(0.5, within(0.0001));
                });
        server.verify();
    }

    private ZtbProperties properties() {
        ZtbProperties properties = new ZtbProperties();
        properties.getElasticsearch().setBaseUrl("http://es");
        properties.getElasticsearch().setIndexName("ztb_file_embedding");
        properties.getEmbedding().setModel("bge-base-zh-v1.5");
        return properties;
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
