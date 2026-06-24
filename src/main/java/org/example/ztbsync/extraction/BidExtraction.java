package org.example.ztbsync.extraction;

import java.util.ArrayList;
import java.util.List;

public class BidExtraction {

    private String bidCompanyName;
    private String bidderContactPhone;
    private String registeredAddress;
    private String mailingAddress;
    private List<ProjectManagementPerson> projectManagementPersonnel = new ArrayList<>();

    public String getBidCompanyName() {
        return bidCompanyName;
    }

    public void setBidCompanyName(String bidCompanyName) {
        this.bidCompanyName = ExtractionTextUtils.cleanCompanyName(bidCompanyName);
    }

    public String getBidderContactPhone() {
        return bidderContactPhone;
    }

    public void setBidderContactPhone(String bidderContactPhone) {
        this.bidderContactPhone = bidderContactPhone;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(String mailingAddress) {
        this.mailingAddress = mailingAddress;
    }

    public List<ProjectManagementPerson> getProjectManagementPersonnel() {
        return projectManagementPersonnel;
    }

    public void setProjectManagementPersonnel(List<ProjectManagementPerson> projectManagementPersonnel) {
        this.projectManagementPersonnel = projectManagementPersonnel == null
                ? new ArrayList<>()
                : new ArrayList<>(projectManagementPersonnel);
    }
}
