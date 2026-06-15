package org.example.ztbsync.api;

import java.util.List;

import org.example.ztbsync.service.BidSimilarityQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投标文件雷同分析结果查询接口。
 */
@RestController
@RequestMapping("/api/bid-similarity")
public class BidSimilarityController {

    private final BidSimilarityQueryService queryService;

    public BidSimilarityController(BidSimilarityQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/projects/{projectId}/results")
    public List<BidSimilarityResultResponse> findByProject(
            @PathVariable String projectId,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return queryService.findByProject(projectId, minScore, riskLevel, status, limit, offset);
    }

    @GetMapping("/projects/{projectId}/files/{fileId}/results")
    public List<BidSimilarityResultResponse> findByProjectAndFile(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return queryService.findByProjectAndFile(projectId, fileId, minScore, riskLevel, status, limit, offset);
    }
}
