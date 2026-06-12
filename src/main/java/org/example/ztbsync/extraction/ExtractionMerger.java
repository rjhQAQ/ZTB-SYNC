package org.example.ztbsync.extraction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ExtractionMerger {

    private final TimeNormalizer timeNormalizer;

    public ExtractionMerger(TimeNormalizer timeNormalizer) {
        this.timeNormalizer = timeNormalizer;
    }

    public TenderExtraction mergeTender(TenderExtraction regex, TenderExtraction llm) {
        TenderExtraction merged = new TenderExtraction();
        TenderExtraction safeRegex = regex == null ? new TenderExtraction() : regex;
        TenderExtraction safeLlm = llm == null ? new TenderExtraction() : llm;

        // Clear label matches from regex are treated as higher confidence; LLM fills gaps.
        merged.setTenderCompanyName(ExtractionTextUtils.firstText(
                safeRegex.getTenderCompanyName(), safeLlm.getTenderCompanyName()));
        merged.setAgencyName(ExtractionTextUtils.firstText(safeRegex.getAgencyName(), safeLlm.getAgencyName()));
        merged.setProjectName(ExtractionTextUtils.firstText(safeRegex.getProjectName(), safeLlm.getProjectName()));
        merged.setBidSubmitStartTime(safeRegex.getBidSubmitStartTime() != null
                ? safeRegex.getBidSubmitStartTime()
                : safeLlm.getBidSubmitStartTime());
        merged.setBidSubmitEndTime(safeRegex.getBidSubmitEndTime() != null
                ? safeRegex.getBidSubmitEndTime()
                : safeLlm.getBidSubmitEndTime());
        merged.setTimePoints(mergeTimePoints(safeRegex.getTimePoints(), safeLlm.getTimePoints()));
        timeNormalizer.earliest(merged.getTimePoints()).ifPresent(merged::setRangeStartTime);
        timeNormalizer.latest(merged.getTimePoints()).ifPresent(merged::setRangeEndTime);
        return merged;
    }

    public BidExtraction mergeBid(BidExtraction regex, BidExtraction llm) {
        BidExtraction merged = new BidExtraction();
        BidExtraction safeRegex = regex == null ? new BidExtraction() : regex;
        BidExtraction safeLlm = llm == null ? new BidExtraction() : llm;

        // Keep deterministic fields from regex first, then add LLM-only details such as richer personnel rows.
        merged.setBidCompanyName(ExtractionTextUtils.firstText(
                safeRegex.getBidCompanyName(), safeLlm.getBidCompanyName()));
        merged.setBidderContactPhone(ExtractionTextUtils.firstText(
                safeRegex.getBidderContactPhone(), safeLlm.getBidderContactPhone()));
        merged.setRegisteredAddress(ExtractionTextUtils.firstText(
                safeRegex.getRegisteredAddress(), safeLlm.getRegisteredAddress()));
        merged.setMailingAddress(ExtractionTextUtils.firstText(
                safeRegex.getMailingAddress(), safeLlm.getMailingAddress()));
        merged.setProjectManagementPersonnel(mergePersonnel(
                safeRegex.getProjectManagementPersonnel(), safeLlm.getProjectManagementPersonnel()));
        return merged;
    }

    private List<TimePoint> mergeTimePoints(List<TimePoint> regexPoints, List<TimePoint> llmPoints) {
        Map<String, TimePoint> merged = new LinkedHashMap<>();
        addTimePoints(merged, regexPoints);
        addTimePoints(merged, llmPoints);
        return new ArrayList<>(merged.values());
    }

    private void addTimePoints(Map<String, TimePoint> target, List<TimePoint> points) {
        if (points == null) {
            return;
        }
        for (TimePoint point : points) {
            if (point == null || !ExtractionTextUtils.hasText(point.normalizedTime())) {
                continue;
            }
            String key = point.normalizedTime() + "|" + nullToEmpty(point.label());
            target.putIfAbsent(key, point);
        }
    }

    private List<ProjectManagementPerson> mergePersonnel(
            List<ProjectManagementPerson> regexPersonnel,
            List<ProjectManagementPerson> llmPersonnel) {
        Map<String, ProjectManagementPerson> merged = new LinkedHashMap<>();
        addPersonnel(merged, regexPersonnel);
        addPersonnel(merged, llmPersonnel);
        return new ArrayList<>(merged.values());
    }

    private void addPersonnel(Map<String, ProjectManagementPerson> target, List<ProjectManagementPerson> personnel) {
        if (personnel == null) {
            return;
        }
        for (ProjectManagementPerson person : personnel) {
            if (person == null) {
                continue;
            }
            String key = nullToEmpty(person.name()) + "|" + nullToEmpty(person.role());
            if (!key.equals("|")) {
                target.putIfAbsent(key, person);
            }
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
