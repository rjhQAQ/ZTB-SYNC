package org.example.ztbsync.extraction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TenderExtraction {

    private String tenderCompanyName;
    private String agencyName;
    private String projectName;
    private LocalDateTime bidSubmitStartTime;
    private LocalDateTime bidSubmitEndTime;
    private LocalDateTime rangeStartTime;
    private LocalDateTime rangeEndTime;
    private List<TimePoint> timePoints = new ArrayList<>();

    public String getTenderCompanyName() {
        return tenderCompanyName;
    }

    public void setTenderCompanyName(String tenderCompanyName) {
        this.tenderCompanyName = tenderCompanyName;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public LocalDateTime getBidSubmitStartTime() {
        return bidSubmitStartTime;
    }

    public void setBidSubmitStartTime(LocalDateTime bidSubmitStartTime) {
        this.bidSubmitStartTime = bidSubmitStartTime;
    }

    public LocalDateTime getBidSubmitEndTime() {
        return bidSubmitEndTime;
    }

    public void setBidSubmitEndTime(LocalDateTime bidSubmitEndTime) {
        this.bidSubmitEndTime = bidSubmitEndTime;
    }

    public LocalDateTime getRangeStartTime() {
        return rangeStartTime;
    }

    public void setRangeStartTime(LocalDateTime rangeStartTime) {
        this.rangeStartTime = rangeStartTime;
    }

    public LocalDateTime getRangeEndTime() {
        return rangeEndTime;
    }

    public void setRangeEndTime(LocalDateTime rangeEndTime) {
        this.rangeEndTime = rangeEndTime;
    }

    public List<TimePoint> getTimePoints() {
        return timePoints;
    }

    public void setTimePoints(List<TimePoint> timePoints) {
        this.timePoints = timePoints == null ? new ArrayList<>() : new ArrayList<>(timePoints);
    }
}
