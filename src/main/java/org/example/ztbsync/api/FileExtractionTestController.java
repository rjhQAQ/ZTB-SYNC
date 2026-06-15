package org.example.ztbsync.api;

import org.example.ztbsync.service.FileExtractionTestService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 测试用文件同步抽取接口。
 *
 * <p>这个接口直接接收 DOCX 文件内容并返回抽取结果，不创建异步任务，也不写业务表。</p>
 */
@RestController
@RequestMapping("/api/files")
public class FileExtractionTestController {

    private final FileExtractionTestService extractionTestService;

    public FileExtractionTestController(FileExtractionTestService extractionTestService) {
        this.extractionTestService = extractionTestService;
    }

    /**
     * 上传 DOCX 文件和类型，立即返回正则、LLM 和合并后的抽取结果。
     */
    @PostMapping(value = "/extract-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionTestResponse extract(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String type) {
        return extractionTestService.extract(file, type);
    }
}
