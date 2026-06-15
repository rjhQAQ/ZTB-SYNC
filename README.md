# ZTB-SYNC

ZTB-SYNC 是一个用于招投标文件上传受理、文件下载、DOCX 文本解析、信息抽取和落库的 Spring Boot 服务。

当前版本的上传接口不接收 multipart 文件，只接收项目和文件元数据，然后通过 `fileId` 调用外部 REST 文件服务获取 `byte[]` 文件内容。文件处理采用异步任务模式，接口先返回任务 ID，后台完成下载、抽取和数据库写入。

另外提供一个测试用同步抽取接口，允许直接上传 DOCX 文件和文件类型，立即返回正则、LLM 和合并后的抽取结果。该接口不创建任务、不写数据库。

## 功能要点

- 支持招标文件和投标文件两类文件，接口类型值支持 `TENDER`、`BID`、`招标文件`、`投标文件`。
- v1 仅支持 `.docx` 文件。
- 文件来源通过配置项 `ztb.file-service.download-url-template` 指定，例如 `http://host/files/{fileId}`。
- 抽取方式为正则抽取 + OpenAI-compatible LLM 抽取，正则字段优先，LLM 用于补全复杂或遗漏信息。
- 投标文件上传后会自动和同项目已有投标文件做两两雷同分析。
- 雷同分析使用自实现字符 3-gram Jaccard，不引入额外相似度库；招标文件作为基准文本参与降权。
- 使用 MyBatis 写入神通/Oscar 数据库。
- 重复上传同一个 `projectId + fileId + type` 会重新处理，业务表按最新任务覆盖。
- 如果旧任务晚于新任务执行，会被标记为 `SUPERSEDED`，不会覆盖最新结果。
- 任务状态包括 `PENDING`、`PROCESSING`、`SUCCESS`、`FAILED`、`SUPERSEDED`。

## 技术栈

- Java 21
- Spring Boot 3.5.15
- Spring Web / RestClient
- MyBatis Spring Boot Starter 3.0.4
- Apache POI 5.5.1
- 神通/Oscar JDBC 驱动 `com.oscar:oscar:8`

## 主要接口

### 上传并创建异步任务

```http
POST /api/files/upload
Content-Type: application/json
```

请求体：

```json
{
  "projectId": "project-001",
  "fileId": "file-001",
  "fileName": "招标文件.docx",
  "type": "TENDER"
}
```

成功响应：`202 Accepted`

```json
{
  "taskId": "生成的任务ID",
  "status": "PENDING",
  "projectId": "project-001",
  "fileId": "file-001",
  "fileName": "招标文件.docx",
  "type": "TENDER"
}
```

### 查询任务详情

```http
GET /api/files/tasks/{taskId}
```

返回任务状态、错误信息和抽取结果摘要。

### 查询某文件最新任务

```http
GET /api/files/tasks/latest?projectId=project-001&fileId=file-001&type=TENDER
```

### 测试用同步抽取

```http
POST /api/files/extract-test
Content-Type: multipart/form-data
```

表单字段：

- `file`：上传的 `.docx` 文件。
- `type`：文件类型，支持 `TENDER`、`BID`、`招标文件`、`投标文件`。

响应会返回 `regexResult`、`llmResult` 和 `mergedResult`，用于联调抽取规则和 LLM 返回效果。该接口不会写入数据库，也不会触发雷同分析。

示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/files/extract-test' \
  -F 'type=BID' \
  -F 'file=@/path/to/投标文件.docx'
```

### 查询项目雷同分析结果

```http
GET /api/bid-similarity/projects/{projectId}/results?minScore=70&riskLevel=HIGH&status=SUCCESS&limit=50&offset=0
```

### 查询某个投标文件参与的雷同分析结果

```http
GET /api/bid-similarity/projects/{projectId}/files/{fileId}/results
```

## 抽取规则

### 招标文件

落库到 `ztb_project_info`，抽取字段包括：

- 招标企业名称
- 代理机构名称
- 项目名称
- 投标文件递交开始时间
- 投标文件递交结束时间
- 项目相关所有时间点 JSON
- 项目整体时间范围开始时间
- 项目整体时间范围结束时间

说明：

- “所有时间点”只抽取项目相关时间点，不抽取投标人或人员相关时间。
- 整体时间范围由所有可归一化项目时间点计算最早和最晚时间。
- `all_time_points_json` 用于查看抽取到的时间点明细。

### 投标文件

落库到 `ztb_project_bidder_company`，抽取字段包括：

- 投标公司名称
- 投标人联系电话
- 注册地址
- 通信地址
- 项目管理人员信息 JSON

投标文件不抽取时间字段。

## 雷同分析规则

雷同分析只针对投标文件 `BID`。当前投标文件复用本次解析出的 DOCX 文本；历史投标文件和项目唯一招标文件每次根据 `fileId` 重新调用文件接口下载并解析，不保存全文或文本快照。

分析流程：

1. 将 DOCX 段落和表格行拆成片段，保留原文用于命中展示。
2. 对片段做归一化：去空白、统一全半角、去常见标点和页码噪声，保留中文、英文和数字。
3. 过滤封面、目录、页眉页脚、签章、固定声明、明显标题、空段和短段。
4. 每个投标片段先和招标文件片段计算 3-gram Jaccard；达到阈值时标记为招标来源片段，计分权重降为 `0.2`。
5. 两份投标文件片段之间计算 3-gram Jaccard；达到阈值时保存为命中片段。
6. 对相同异常数字、备案号、证书号、联系电话、单位串用，以及“备案/评标/单位/材料/结果”等关键词片段额外加分。

默认风险等级：

- `<70`：`LOW`
- `70-84.99`：`SUSPECTED`
- `>=85`：`HIGH`

命中片段默认最多保存前 20 条，字段包含两边原文、相似度、是否招标来源、权重和命中原因。

## 处理流程

1. 调用 `POST /api/files/upload` 创建任务，任务状态为 `PENDING`。
2. 事务提交后，后台线程开始处理任务。
3. 任务状态更新为 `PROCESSING`。
4. 根据 `fileId` 调用文件下载接口获取 `ResponseEntity<byte[]>` 中的文件内容。
5. 使用 Apache POI 解析 DOCX 文本。
6. 执行正则抽取。
7. 如果启用 LLM，则调用 OpenAI-compatible `/chat/completions` 接口并解析 JSON。
8. 合并正则和 LLM 结果，正则优先。
9. 写入对应业务表，并将任务状态更新为 `SUCCESS`。
10. 异常时任务状态更新为 `FAILED`，并记录错误信息。

## 数据库

DDL 文件位于：

```text
src/main/resources/db/schema.sql
```

应用不会自动建表，部署前需要在神通/Oscar 数据库中手动执行该 SQL。

三张表：

- `ztb_file_processing_task`：异步任务状态、错误信息、LLM 原始 JSON、结果摘要。
- `ztb_project_info`：招标文件抽取后的项目信息。
- `ztb_project_bidder_company`：投标文件抽取后的投标企业信息。
- `ztb_bid_similarity_analysis`：投标文件两两雷同分析结果。

## 配置

配置文件位于：

```text
src/main/resources/application.yml
```

### 数据库配置

运行前需要配置真实神通数据库连接：

```properties
spring.datasource.driver-class-name=com.oscar.Driver
spring.datasource.url=jdbc:oscar://127.0.0.1:2003/OSRDB
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

对应的 yml 写法：

```yaml
spring:
  datasource:
    driver-class-name: com.oscar.Driver
    url: jdbc:oscar://127.0.0.1:2003/OSRDB
    username: 你的用户名
    password: 你的密码
```

### 文件服务配置

```yaml
ztb:
  file-service:
    download-url-template: http://127.0.0.1:8081/files/{fileId}
```

也可以通过环境变量覆盖：

```bash
export ZTB_FILE_DOWNLOAD_URL_TEMPLATE='http://127.0.0.1:8081/files/{fileId}'
```

### LLM 配置

LLM 默认关闭：

```yaml
ztb:
  llm:
    enabled: false
```

启用 OpenAI-compatible 接口：

```bash
export ZTB_LLM_ENABLED=true
export ZTB_LLM_BASE_URL='https://api.example.com/v1'
export ZTB_LLM_API_KEY='你的API Key'
export ZTB_LLM_MODEL='gpt-4o-mini'
```

代码会请求：

```text
{ZTB_LLM_BASE_URL}/chat/completions
```

### 异步线程池配置

```yaml
ztb:
  async:
    core-pool-size: 2
    max-pool-size: 4
    queue-capacity: 200
```

### 雷同分析配置

```yaml
ztb:
  similarity:
    enabled: true
    segment-match-threshold: 0.82
    tender-match-threshold: 0.85
    tender-derived-weight: 0.2
    suspected-threshold: 70
    high-risk-threshold: 85
    top-hit-limit: 20
    min-segment-chars: 30
    llm-review-enabled: false
    llm-review-min-score: 70
```

## 日志

关键链路已经输出日志，包括：

- 上传受理和任务 ID
- 旧任务 supersede 数量
- 文件下载开始和完成字节数
- DOCX 解析出的文本长度
- LLM 调用模型、输入长度和返回 JSON 长度
- 招标/投标业务表插入或更新
- 雷同分析成功、失败、得分和风险等级
- 任务成功、失败或 supersede

日志不会输出完整 DOCX 文本和完整 LLM 返回内容，避免招投标文件内容进入日志。

## 构建和测试

本项目使用指定 Maven：

```bash
/Users/renjiahao/Documents/java/apache-maven-3.8.3/bin/mvn test
```

当前测试覆盖：

- DOCX 文本提取
- 招标文件正则抽取
- 投标文件正则抽取
- 时间范围计算
- 正则和 LLM 结果合并
- 测试用同步抽取接口
- 3-gram Jaccard 雷同分析
- 招标来源片段降权
- 雷同分析查询接口
- 上传接口校验
- 异步 worker 关键分支

## 目录说明

```text
src/main/java/org/example/ztbsync/api          REST 接口和响应模型
src/main/java/org/example/ztbsync/service      上传受理、文件下载、异步处理、落库服务
src/main/java/org/example/ztbsync/extraction   DOCX 解析、正则抽取、时间归一化、结果合并
src/main/java/org/example/ztbsync/similarity   投标文件雷同分析、3-gram Jaccard、招标降权
src/main/java/org/example/ztbsync/llm          OpenAI-compatible LLM 调用和响应映射
src/main/java/org/example/ztbsync/mapper       MyBatis Mapper
src/main/java/org/example/ztbsync/domain       任务、业务表实体和枚举
src/main/resources/db/schema.sql               数据库建表 SQL
```
