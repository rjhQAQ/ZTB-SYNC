# Elasticsearch Embedding Mapping

本文档用于手动创建 RAG embedding 写入使用的 Elasticsearch 7.9.3 索引。

## 索引信息

- 索引名称：`ztb_file_embedding_v2`
- 向量字段：`embedding`
- 向量类型：`dense_vector`
- 向量维度：`768`
- 对应模型：`bge-base-zh-v1.5`

程序写入 ES 的文档 ID 规则：

```text
projectId_fileId_fileType_taskId_chunkIndex
```

## 创建 Mapping

```http
PUT /ztb_file_embedding_v2
Content-Type: application/json
```

```json
{
  "mappings": {
    "properties": {
      "projectId": {
        "type": "keyword"
      },
      "fileId": {
        "type": "keyword"
      },
      "fileName": {
        "type": "keyword"
      },
      "fileType": {
        "type": "keyword"
      },
      "taskId": {
        "type": "keyword"
      },
      "chunkIndex": {
        "type": "integer"
      },
      "chunkText": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "sectionPath": {
        "type": "keyword"
      },
      "sectionPathText": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "blockTypes": {
        "type": "keyword"
      },
      "boilerplate": {
        "type": "boolean"
      },
      "charStart": {
        "type": "integer"
      },
      "charEnd": {
        "type": "integer"
      },
      "model": {
        "type": "keyword"
      },
      "embeddingText": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 768
      },
      "createdAt": {
        "type": "date"
      }
    }
  }
}
```

## curl 示例

```bash
curl -X PUT 'http://你的ES地址:9200/ztb_file_embedding_v2' \
  -H 'Content-Type: application/json' \
  -d '{
    "mappings": {
      "properties": {
        "projectId": { "type": "keyword" },
        "fileId": { "type": "keyword" },
        "fileName": { "type": "keyword" },
        "fileType": { "type": "keyword" },
        "taskId": { "type": "keyword" },
        "chunkIndex": { "type": "integer" },
        "chunkText": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart"
        },
        "sectionPath": { "type": "keyword" },
        "sectionPathText": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart"
        },
        "blockTypes": { "type": "keyword" },
        "boilerplate": { "type": "boolean" },
        "charStart": { "type": "integer" },
        "charEnd": { "type": "integer" },
        "model": { "type": "keyword" },
        "embeddingText": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart"
        },
        "embedding": {
          "type": "dense_vector",
          "dims": 768
        },
        "createdAt": { "type": "date" }
      }
    }
  }'
```

## 重建索引

测试环境如果需要重建索引，可以先删除再创建：

```bash
curl -X DELETE 'http://你的ES地址:9200/ztb_file_embedding_v2'
```

注意：已有索引中的 `dense_vector.dims` 不能直接修改。如果 embedding 模型切换导致维度变化，需要新建索引或删除后重建。

## 查询方式

v2 查询默认使用混合检索：

- 向量召回：`embedding` + `cosineSimilarity`。
- 关键词召回：`chunkText^3`、`sectionPathText^2`、`embeddingText` 的 `multi_match`。
- 应用侧使用 RRF 合并候选，再调用 `bge-rerank` 重排。

`embeddingText` 用于向量化和重排，包含文件类型、文件名、章节路径和正文；`chunkText` 保留原文片段，用于接口返回。
