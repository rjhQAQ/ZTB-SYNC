package org.example.ztbsync.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class EmbeddingClientTest {

    @Test
    void callsOpenAiCompatibleEmbeddingApi() {
        ZtbProperties properties = properties(3);
        properties.getEmbedding().setApiKey("secret");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        EmbeddingClient client = new EmbeddingClient(properties, restTemplate);
        server.expect(requestTo("http://embedding-service/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
                .andExpect(content().json("""
                        {
                          "model": "bge-base-zh-v1.5",
                          "input": ["片段一", "片段二"]
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"embedding": [0.1, 0.2, 0.3]},
                            {"embedding": [0.4, 0.5, 0.6]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<List<Double>> embeddings = client.embed(List.of("片段一", "片段二"));

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).containsExactly(0.1, 0.2, 0.3);
        server.verify();
    }

    @Test
    void rejectsUnexpectedVectorDimensions() {
        ZtbProperties properties = properties(3);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        EmbeddingClient client = new EmbeddingClient(properties, restTemplate);
        server.expect(requestTo("http://embedding-service/v1/embeddings"))
                .andRespond(withSuccess("""
                        {"data": [{"embedding": [0.1, 0.2]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed(List.of("片段一")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("维度不匹配");
        server.verify();
    }

    private ZtbProperties properties(int dims) {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setBaseUrl("http://embedding-service");
        properties.getEmbedding().setVectorDims(dims);
        return properties;
    }
}
