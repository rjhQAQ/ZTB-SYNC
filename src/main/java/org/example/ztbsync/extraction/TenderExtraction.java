package org.example.ztbsync.extraction;

import java.util.ArrayList;
import java.util.List;

public class TenderExtraction {

    private String tenderCompanyName;
    private String agencyName;
    private String projectName;
    private String bidSubmitStartTime;
    private String bidSubmitEndTime;
    private String rangeStartTime;
    private String rangeEndTime;
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

    public String getBidSubmitStartTime() {
        return bidSubmitStartTime;
    }

    public void setBidSubmitStartTime(String bidSubmitStartTime) {
        this.bidSubmitStartTime = bidSubmitStartTime;
    }

    public String getBidSubmitEndTime() {
        return bidSubmitEndTime;
    }

    public void setBidSubmitEndTime(String bidSubmitEndTime) {
        this.bidSubmitEndTime = bidSubmitEndTime;
    }

    public String getRangeStartTime() {
        return rangeStartTime;
    }

    public void setRangeStartTime(String rangeStartTime) {
        this.rangeStartTime = rangeStartTime;
    }

    public String getRangeEndTime() {
        return rangeEndTime;
    }

    public void setRangeEndTime(String rangeEndTime) {
        this.rangeEndTime = rangeEndTime;
    }

    public List<TimePoint> getTimePoints() {
        return timePoints;
    }

    public void setTimePoints(List<TimePoint> timePoints) {
        this.timePoints = timePoints == null ? new ArrayList<>() : new ArrayList<>(timePoints);
    }
}
