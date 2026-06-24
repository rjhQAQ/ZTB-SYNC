package org.example.ztbsync.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.rag.RagEmbeddingTextBuilder;
import org.example.ztbsync.rag.RagSearchHit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

class BgeRerankClientTest {

    @Test
    void reranksCandidatesByReturnedScore() {
        ZtbProperties properties = new ZtbProperties();
        properties.getRerank().setBaseUrl("http://rerank");
        properties.getRerank().setApiKey("secret");
        RestTemplate restTemplate = restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BgeRerankClient client = new BgeRerankClient(properties, restTemplate, new RagEmbeddingTextBuilder());

        server.expect(requestTo("http://rerank/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"bge-rerank\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("章节路径：第二章")))
                .andRespond(withSuccess("""
                        {"results":[{"index":1,"relevance_score":0.91},{"index":0,"relevance_score":0.12}]}
                        """, MediaType.APPLICATION_JSON));

        List<RagSearchHit> reranked = client.rerank("保证金", List.of(
                new RagSearchHit("file-1", "招标.docx", "TENDER", 1, "项目经理要求", "第三章", 0.6),
                new RagSearchHit("file-2", "招标.docx", "TENDER", 2, "保证金要求", "第二章", 0.7)), 1);

        assertThat(reranked).singleElement()
                .satisfies(hit -> {
                    assertThat(hit.fileId()).isEqualTo("file-2");
                    assertThat(hit.score()).isEqualTo(0.91);
                });
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
