package org.example.ztbsync.mcp;

import org.example.ztbsync.api.RagSearchResponse;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.exception.RagSearchNoDataException;
import org.example.ztbsync.exception.RagSearchUnavailableException;
import org.example.ztbsync.service.RagSearchService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 面向 MCP 客户端暴露的招投标文档 RAG 查询工具。
 */
@Component
public class RagMcpTool {

    private final RagSearchService ragSearchService;

    public RagMcpTool(RagSearchService ragSearchService) {
        this.ragSearchService = ragSearchService;
    }

    @McpTool(
            name = "search_ztb_project_documents",
            description = "按项目 ID 查询已入 Elasticsearch 向量库的招标文件和投标文件相关片段。")
    public RagSearchResponse searchZtbProjectDocuments(
            @McpToolParam(description = "项目唯一 ID，用于限定向量检索范围。") String project_id,
            @McpToolParam(description = "用户原始问题，会先调用 embedding 服务生成查询向量。") String user_question,
            @McpToolParam(required = false, description = "返回的最相关片段数量，默认使用系统配置。") Integer top_k,
            @McpToolParam(required = false, description = "文件类型过滤：ALL、TENDER 或 BID。") String file_type) {
        try {
            return ragSearchService.search(project_id, user_question, top_k, file_type);
        } catch (RagSearchNoDataException exception) {
            return RagSearchResponse.error(404, exception.getMessage());
        } catch (RagSearchUnavailableException exception) {
            return RagSearchResponse.error(502, exception.getMessage());
        } catch (BadRequestException exception) {
            return RagSearchResponse.error(400, exception.getMessage());
        }
    }

    public RagSearchResponse searchZtbProjectDocuments(String project_id, String user_question, Integer top_k) {
        return searchZtbProjectDocuments(project_id, user_question, top_k, null);
    }
}
