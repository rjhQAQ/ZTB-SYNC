package org.example.ztbsync.api;

import org.example.ztbsync.service.FileEmbeddingTestService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 测试用文件 embedding 入 Elasticsearch 接口。
 *
 * <p>该接口直接接收 DOCX 文件并写入 ES，不创建异步任务，也不写神通任务表。</p>
 */
@RestController
@RequestMapping("/api/files")
public class FileEmbeddingTestController {

    private final FileEmbeddingTestService embeddingTestService;

    public FileEmbeddingTestController(FileEmbeddingTestService embeddingTestService) {
        this.embeddingTestService = embeddingTestService;
    }

    /**
     * 上传 DOCX 文件，立即切分 chunk、调用 embedding 服务并写入 Elasticsearch。
     */
    @PostMapping(value = "/embedding-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmbeddingTestResponse embeddingTest(
            @RequestPart("file") MultipartFile file,
            @RequestParam("projectId") String projectId,
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam("type") String type) {
        return embeddingTestService.index(file, projectId, fileId, fileName, type);
    }
}
