# MCP RAG Query

本文档说明围串标 MCP 工具使用的 RAG 查询接口。查询只读取已经写入 Elasticsearch 的 `ztb_file_embedding_v2` chunk，不重新下载文件，也不重新解析 DOCX。

## REST API

```http
POST /api/rag/search
Content-Type: application/json
```

请求体兼容 snake_case 和 camelCase，建议 MCP 侧使用 snake_case：

```json
{
  "project_id": "PROJ-2026-0089X",
  "user_question": "招标文件第三章里关于项目经理资质的具体要求是什么？",
  "top_k": 3,
  "file_type": "TENDER"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "project_id": "PROJ-2026-0089X",
    "matched_documents": [
      {
        "doc_id": "file-1",
        "doc_type": "招标文件",
        "doc_name": "招标文件.docx",
        "relevant_chapters": ["第三章", "资格要求"],
        "source_text": "项目经理须具备相关资质……",
        "score": 0.9123
      }
    ]
  }
}
```

无数据响应：

```json
{
  "code": 404,
  "message": "该项目尚未上传或解析招投标文件，无法进行文档内容审计。",
  "data": null
}
```

embedding 或 ES 查询异常时返回：

```json
{
  "code": 502,
  "message": "RAG 查询服务不可用: ...",
  "data": null
}
```

## MCP Tool

工具名称：

```text
search_ztb_project_documents
```

工具入参：

- `project_id`：项目唯一 ID，用于限定向量检索范围。
- `user_question`：用户原始问题，会先调用 embedding 服务生成查询向量。
- `top_k`：返回的最相关片段数量；不传时使用 `ztb.rag-search.default-top-k`。
- `file_type`：可选文件类型过滤，支持 `ALL`、`TENDER`、`BID`，不传时为 `ALL`。

工具返回值和 REST API 响应结构一致。`top_k` 表示 chunk 片段数量，不做文件去重，因此同一个文件可以多次命中。

## ES 查询方式

默认使用混合检索：

- 向量召回：ES 7.9.3 使用 `dense_vector` 的脚本打分能力。
- 关键词召回：`multi_match` 查询 `chunkText^3`、`sectionPathText^2`、`embeddingText`。
- 应用侧使用 RRF 合并候选，再调用 `bge-rerank` 重排。
- `bge-rerank` 异常时默认降级为混合检索结果。

向量召回请求示例：

```json
{
  "size": 3,
  "_source": [
    "fileId",
    "fileName",
    "fileType",
    "chunkIndex",
    "chunkText",
    "sectionPath"
  ],
  "query": {
    "script_score": {
      "query": {
        "bool": {
          "filter": [
            { "term": { "projectId": "PROJ-2026-0089X" } },
            { "exists": { "field": "embedding" } },
            { "term": { "boilerplate": false } },
            { "term": { "model": "bge-base-zh-v1.5" } }
          ]
        }
      },
      "script": {
        "source": "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
        "params": {
          "queryVector": []
        }
      }
    }
  }
}
```

程序会把 ES `_score - 1.0` 归一化为 `0-1` 的 `score` 返回。

关键词召回请求示例：

```json
{
  "size": 50,
  "_source": [
    "fileId",
    "fileName",
    "fileType",
    "chunkIndex",
    "chunkText",
    "sectionPath"
  ],
  "query": {
    "bool": {
      "filter": [
        { "term": { "projectId": "PROJ-2026-0089X" } },
        { "exists": { "field": "embedding" } },
        { "term": { "boilerplate": false } },
        { "term": { "model": "bge-base-zh-v1.5" } }
      ],
      "must": [
        {
          "multi_match": {
            "query": "项目经理资质",
            "fields": ["chunkText^3", "sectionPathText^2", "embeddingText"]
          }
        }
      ]
    }
  }
}
```

## 配置

```yaml
ztb:
  rag-search:
    mode: HYBRID
    default-top-k: 3
    max-top-k: 20
    include-boilerplate: false
    filter-model: true
    min-score: 0
    vector-candidate-size: 50
    keyword-candidate-size: 50
    rrf-rank-constant: 60
    vector-weight: 0.6
    keyword-weight: 0.4
  rerank:
    enabled: true
    model: bge-rerank
    endpoint: /v1/rerank
    candidate-size: 50
    fallback-to-hybrid: true
```

- `include-boilerplate=false`：默认过滤授权委托书、投标函、声明、签章等制式化 chunk。
- `filter-model=true`：默认只查询当前 `ztb.embedding.model` 写入的向量，避免不同模型或维度混查。
- `min-score`：归一化分数阈值，默认不额外过滤。
- `mode=HYBRID`：默认同时使用向量召回和关键词召回。
- `fallback-to-hybrid=true`：`bge-rerank` 不可用时仍返回混合检索排序结果。

## 重建 v2 索引

已有项目升级到 v2 索引时，需要重新下载 DOCX、重新切分和 embedding：

```http
POST /api/rag/reindex/projects/{projectId}
```

响应包含 `total_files`、`success_count`、`failed_count` 和失败文件列表。单个文件失败不会阻断其它文件重建。
