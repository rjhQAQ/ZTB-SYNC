package org.example.ztbsync.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class TenderRegexExtractorTest {

    private final TimeNormalizer timeNormalizer = new TimeNormalizer();
    private final TenderRegexExtractor extractor = new TenderRegexExtractor(timeNormalizer);

    @Test
    void extractsTenderFieldsAndProjectTimeRange() {
        String text = """
                项目名称：智慧园区建设项目
                招标人：上海示例招标有限公司
                招标代理机构：北京示例代理有限公司
                投标文件递交开始时间：2026年06月12日 09时00分
                投标文件递交截止时间：2026年06月15日 17时30分
                开标时间：2026年06月16日 10时00分
                """;

        TenderExtraction result = extractor.extract(text);

        assertThat(result.getProjectName()).isEqualTo("智慧园区建设项目");
        assertThat(result.getTenderCompanyName()).isEqualTo("上海示例招标有限公司");
        assertThat(result.getAgencyName()).isEqualTo("北京示例代理有限公司");
        assertThat(result.getBidSubmitStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 12, 9, 0));
        assertThat(result.getBidSubmitEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 15, 17, 30));
        assertThat(result.getRangeStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 12, 9, 0));
        assertThat(result.getRangeEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 16, 10, 0));
        assertThat(result.getTimePoints()).hasSize(3);
    }
}
