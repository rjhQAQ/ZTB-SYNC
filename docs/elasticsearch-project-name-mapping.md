# Elasticsearch Tender Project Name Mapping

本文档用于手动创建招标文件项目名称搜索索引。该索引只保存项目名称和基础文件信息，用于后续按中文分词检索项目名称。

## 前置要求

ES 7.9.3 需要安装 IK analysis 插件，否则 `ik_max_word` 和 `ik_smart` analyzer 无法创建成功。

## 索引信息

- 索引名称：`ztb_tender_project_name`
- 写入时机：招标文件 `TENDER` 抽取结果写入神通业务表成功后
- 文档 ID：`projectId_fileId`
- 项目名称建索引分词器：`ik_max_word`
- 项目名称搜索分词器：`ik_smart`

## 创建 Mapping

```http
PUT /ztb_tender_project_name
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
      "taskId": {
        "type": "keyword"
      },
      "projectName": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "projectNameKeyword": {
        "type": "keyword"
      },
      "tenderCompanyName": {
        "type": "keyword"
      },
      "agencyName": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date"
      },
      "updatedAt": {
        "type": "date"
      }
    }
  }
}
```

## curl 示例

```bash
curl -X PUT 'http://你的ES地址:9200/ztb_tender_project_name' \
  -H 'Content-Type: application/json' \
  -d '{
    "mappings": {
      "properties": {
        "projectId": { "type": "keyword" },
        "fileId": { "type": "keyword" },
        "fileName": { "type": "keyword" },
        "taskId": { "type": "keyword" },
        "projectName": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart"
        },
        "projectNameKeyword": { "type": "keyword" },
        "tenderCompanyName": { "type": "keyword" },
        "agencyName": { "type": "keyword" },
        "createdAt": { "type": "date" },
        "updatedAt": { "type": "date" }
      }
    }
  }'
```

## 查询示例

```bash
curl -X GET 'http://你的ES地址:9200/ztb_tender_project_name/_search' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "match": {
        "projectName": "智慧园区建设"
      }
    }
  }'
```

## 重建索引

测试环境如果需要重建索引，可以先删除再创建：

```bash
curl -X DELETE 'http://你的ES地址:9200/ztb_tender_project_name'
```

注意：已有字段的 analyzer 不能直接修改。如果需要调整中文分词器，建议删除测试索引后重建，生产环境应新建索引并做别名切换。
