package org.example.ztbsync.api;

import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.service.RagSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 文档片段向量查询接口。
 */
@RestController
@RequestMapping("/api/rag")
public class RagSearchController {

    private final RagSearchService ragSearchService;

    public RagSearchController(RagSearchService ragSearchService) {
        this.ragSearchService = ragSearchService;
    }

    /**
     * 根据项目 ID 和用户问题检索已写入 ES 的招投标文件 chunk。
     */
    @PostMapping("/search")
    public ResponseEntity<RagSearchResponse> search(@RequestBody RagSearchRequest request) {
        try {
            return ResponseEntity.ok(ragSearchService.search(request));
        } catch (RagSearchNoDataException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RagSearchResponse.error(404, exception.getMessage()));
        } catch (RagSearchUnavailableException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(RagSearchResponse.error(502, exception.getMessage()));
        }
    }
}
