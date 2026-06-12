package org.example.ztbsync.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ExtractionMergerTest {

    private final TimeNormalizer timeNormalizer = new TimeNormalizer();
    private final ExtractionMerger merger = new ExtractionMerger(timeNormalizer);

    @Test
    void keepsRegexFieldsAndUsesLlmAsFallback() {
        TenderExtraction regex = new TenderExtraction();
        regex.setProjectName("正则项目");
        regex.setTimePoints(List.of(new TimePoint("报名开始", "2026年6月1日 9时00分", "2026-06-01 09:00:00", "REGEX")));

        TenderExtraction llm = new TenderExtraction();
        llm.setProjectName("LLM 项目");
        llm.setTenderCompanyName("LLM 招标公司");
        llm.setTimePoints(List.of(new TimePoint("开标", "2026-06-10 10:00:00", "2026-06-10 10:00:00", "LLM")));

        TenderExtraction merged = merger.mergeTender(regex, llm);

        assertThat(merged.getProjectName()).isEqualTo("正则项目");
        assertThat(merged.getTenderCompanyName()).isEqualTo("LLM 招标公司");
        assertThat(merged.getRangeStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
        assertThat(merged.getRangeEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 10, 10, 0));
        assertThat(merged.getTimePoints()).hasSize(2);
    }

    @Test
    void mergesBidPersonnelWithoutDroppingRegexFields() {
        BidExtraction regex = new BidExtraction();
        regex.setBidCompanyName("正则投标公司");
        regex.setProjectManagementPersonnel(List.of(new ProjectManagementPerson("张三", "项目经理", null, null, "项目经理：张三")));

        BidExtraction llm = new BidExtraction();
        llm.setBidCompanyName("LLM 投标公司");
        llm.setBidderContactPhone("13800138000");
        llm.setProjectManagementPersonnel(List.of(new ProjectManagementPerson("李四", "技术负责人", null, null, "技术负责人：李四")));

        BidExtraction merged = merger.mergeBid(regex, llm);

        assertThat(merged.getBidCompanyName()).isEqualTo("正则投标公司");
        assertThat(merged.getBidderContactPhone()).isEqualTo("13800138000");
        assertThat(merged.getProjectManagementPersonnel()).hasSize(2);
    }
}
