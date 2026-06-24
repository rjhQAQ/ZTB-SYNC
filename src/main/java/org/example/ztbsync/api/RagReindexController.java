package org.example.ztbsync.api;

import org.example.ztbsync.service.RagReindexService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG v2 索引重建接口，用于已有项目升级或联调重刷。
 */
@RestController
@RequestMapping("/api/rag")
public class RagReindexController {

    private final RagReindexService ragReindexService;

    public RagReindexController(RagReindexService ragReindexService) {
        this.ragReindexService = ragReindexService;
    }

    @PostMapping("/reindex/projects/{projectId}")
    public RagReindexResponse reindexProject(@PathVariable String projectId) {
        return ragReindexService.reindexProject(projectId);
    }
}
