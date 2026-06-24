package org.example.ztbsync.api;

import java.nio.charset.StandardCharsets;

import org.example.ztbsync.service.FileDownloadClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试用文件下载接口。
 *
 * <p>该接口会代调真实文件服务，并把下载到的 byte[] 直接返回，便于联调文件服务参数。</p>
 */
@RestController
@RequestMapping("/api/files")
public class FileDownloadTestController {

    private final FileDownloadClient fileDownloadClient;

    public FileDownloadTestController(FileDownloadClient fileDownloadClient) {
        this.fileDownloadClient = fileDownloadClient;
    }

    /**
     * 根据项目 ID、文件 ID 和文件名称测试下载文件字节流。
     */
    @PostMapping("/download-test")
    public ResponseEntity<byte[]> download(@RequestBody DownloadFileTestRequest request) {
        request.validate();
        byte[] bytes = fileDownloadClient.download(
                request.normalizedFileId(),
                request.normalizedFileName(),
                request.normalizedProjectId());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(request.normalizedFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(bytes);
    }
}
