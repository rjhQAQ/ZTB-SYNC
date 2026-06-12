package org.example.ztbsync.extraction;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TenderRegexExtractor {

    private static final List<String> TENDER_COMPANY_KEYS = List.of("招标企业名称", "招标人名称", "招标人", "招标单位", "采购人");
    private static final List<String> AGENCY_KEYS = List.of("代理机构名称", "招标代理机构", "采购代理机构", "代理机构");
    private static final List<String> PROJECT_KEYS = List.of("项目名称", "工程名称");
    private static final List<String> SUBMIT_START_KEYS = List.of("投标文件递交开始时间", "递交投标文件开始时间", "递交开始时间", "开始接收时间");
    private static final List<String> SUBMIT_END_KEYS = List.of("投标文件递交截止时间", "投标截止时间", "递交截止时间", "投标文件递交结束时间", "递交结束时间");

    private final TimeNormalizer timeNormalizer;

    public TenderRegexExtractor(TimeNormalizer timeNormalizer) {
        this.timeNormalizer = timeNormalizer;
    }

    public TenderExtraction extract(String text) {
        TenderExtraction extraction = new TenderExtraction();
        extraction.setTenderCompanyName(findField(text, TENDER_COMPANY_KEYS));
        extraction.setAgencyName(findField(text, AGENCY_KEYS));
        extraction.setProjectName(findField(text, PROJECT_KEYS));
        extraction.setBidSubmitStartTime(findTimeAfterKeywords(text, SUBMIT_START_KEYS));
        extraction.setBidSubmitEndTime(findTimeAfterKeywords(text, SUBMIT_END_KEYS));
        extraction.setTimePoints(timeNormalizer.extractTimePoints(text));
        fillTimeRange(extraction);
        return extraction;
    }

    private String findField(String text, List<String> keys) {
        for (String line : ExtractionTextUtils.lines(text)) {
            for (String key : keys) {
                int index = line.indexOf(key);
                if (index >= 0) {
                    String tail = line.substring(index + key.length());
                    if (tail.matches("^\\s*[:：].+")) {
                        return ExtractionTextUtils.cleanValue(tail);
                    }
                }
            }
        }
        return null;
    }

    private LocalDateTime findTimeAfterKeywords(String text, List<String> keys) {
        for (String line : ExtractionTextUtils.lines(text)) {
            for (String key : keys) {
                int index = line.indexOf(key);
                if (index >= 0) {
                    String tail = line.substring(index + key.length());
                    return timeNormalizer.parse(tail).orElse(null);
                }
            }
        }
        return null;
    }

    private void fillTimeRange(TenderExtraction extraction) {
        timeNormalizer.earliest(extraction.getTimePoints()).ifPresent(extraction::setRangeStartTime);
        timeNormalizer.latest(extraction.getTimePoints()).ifPresent(extraction::setRangeEndTime);
    }
}
