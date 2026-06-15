package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.example.ztbsync.api.ExtractionTestResponse;
import org.example.ztbsync.domain.FileType;
import org.example.ztbsync.exception.BadRequestException;
import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.BidRegexExtractor;
import org.example.ztbsync.extraction.DocxTextExtractor;
import org.example.ztbsync.extraction.ExtractionMerger;
import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.extraction.TenderRegexExtractor;
import org.example.ztbsync.llm.LlmExtractionClient;
import org.example.ztbsync.llm.LlmExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileExtractionTestServiceTest {

    @Test
    void extractsBidFileSynchronously() {
        DocxTextExtractor docxTextExtractor = mock(DocxTextExtractor.class);
        BidRegexExtractor bidRegexExtractor = mock(BidRegexExtractor.class);
        LlmExtractionClient llmExtractionClient = mock(LlmExtractionClient.class);
        ExtractionMerger merger = mock(ExtractionMerger.class);
        FileExtractionTestService service = new FileExtractionTestService(
                docxTextExtractor,
                mock(TenderRegexExtractor.class),
                bidRegexExtractor,
                llmExtractionClient,
                merger);
        String text = "投标人名称：杭州示例投标有限公司";
        BidExtraction regex = new BidExtraction();
        regex.setBidCompanyName("杭州示例投标有限公司");
        BidExtraction llm = new BidExtraction();
        BidExtraction merged = new BidExtraction();
        merged.setBidCompanyName("杭州示例投标有限公司");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "投标文件.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1, 2, 3});

        when(docxTextExtractor.extract(any())).thenReturn(text);
        when(llmExtractionClient.extract(eq(FileType.BID), eq(text)))
                .thenReturn(new LlmExtractionResult(new TenderExtraction(), llm, "{\"ok\":true}"));
        when(bidRegexExtractor.extract(text)).thenReturn(regex);
        when(merger.mergeBid(regex, llm)).thenReturn(merged);

        ExtractionTestResponse response = service.extract(file, "投标文件");

        assertThat(response.type()).isEqualTo("BID");
        assertThat(response.fileName()).isEqualTo("投标文件.docx");
        assertThat(response.llmRawJson()).isEqualTo("{\"ok\":true}");
        assertThat(response.mergedResult()).isSameAs(merged);
    }

    @Test
    void rejectsNonDocxFile() {
        FileExtractionTestService service = new FileExtractionTestService(
                mock(DocxTextExtractor.class),
                mock(TenderRegexExtractor.class),
                mock(BidRegexExtractor.class),
                mock(LlmExtractionClient.class),
                mock(ExtractionMerger.class));
        MockMultipartFile file = new MockMultipartFile("file", "投标文件.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.extract(file, "BID"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("仅支持 DOCX");
    }
}
