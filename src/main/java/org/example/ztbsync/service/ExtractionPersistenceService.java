package org.example.ztbsync.service;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.ProjectBidderCompany;
import org.example.ztbsync.domain.ProjectInfo;
import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.TenderExtraction;
import org.example.ztbsync.mapper.FileProcessingTaskMapper;
import org.example.ztbsync.mapper.ProjectBidderCompanyMapper;
import org.example.ztbsync.mapper.ProjectInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionPersistenceService.class);

    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectBidderCompanyMapper projectBidderCompanyMapper;
    private final FileProcessingTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public ExtractionPersistenceService(
            ProjectInfoMapper projectInfoMapper,
            ProjectBidderCompanyMapper projectBidderCompanyMapper,
            FileProcessingTaskMapper taskMapper,
            ObjectMapper objectMapper) {
        this.projectInfoMapper = projectInfoMapper;
        this.projectBidderCompanyMapper = projectBidderCompanyMapper;
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persistTenderAndComplete(FileProcessingTask task, TenderExtraction extraction) {
        LocalDateTime now = LocalDateTime.now();
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setProjectId(task.getProjectId());
        projectInfo.setFileId(task.getFileId());
        projectInfo.setFileName(task.getFileName());
        projectInfo.setTenderCompanyName(extraction.getTenderCompanyName());
        projectInfo.setAgencyName(extraction.getAgencyName());
        projectInfo.setProjectName(extraction.getProjectName());
        projectInfo.setBidSubmitStartTime(extraction.getBidSubmitStartTime());
        projectInfo.setBidSubmitEndTime(extraction.getBidSubmitEndTime());
        projectInfo.setRangeStartTime(extraction.getRangeStartTime());
        projectInfo.setRangeEndTime(extraction.getRangeEndTime());
        projectInfo.setAllTimePointsJson(writeJson(extraction.getTimePoints()));
        projectInfo.setTaskId(task.getTaskId());
        projectInfo.setCreatedAt(now);
        projectInfo.setUpdatedAt(now);

        if (projectInfoMapper.countByKey(task.getProjectId(), task.getFileId()) > 0) {
            projectInfoMapper.updateByKey(projectInfo);
            log.info("Updated tender project info: taskId={}, projectId={}, fileId={}, timePoints={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), extraction.getTimePoints().size());
        } else {
            projectInfoMapper.insert(projectInfo);
            log.info("Inserted tender project info: taskId={}, projectId={}, fileId={}, timePoints={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(), extraction.getTimePoints().size());
        }
        taskMapper.markSuccess(task.getTaskId(), writeJson(extraction), now);
    }

    @Transactional
    public void persistBidAndComplete(FileProcessingTask task, BidExtraction extraction) {
        LocalDateTime now = LocalDateTime.now();
        ProjectBidderCompany company = new ProjectBidderCompany();
        company.setProjectId(task.getProjectId());
        company.setFileId(task.getFileId());
        company.setFileName(task.getFileName());
        company.setBidCompanyName(extraction.getBidCompanyName());
        company.setBidderContactPhone(extraction.getBidderContactPhone());
        company.setRegisteredAddress(extraction.getRegisteredAddress());
        company.setMailingAddress(extraction.getMailingAddress());
        company.setProjectManagementPersonnelJson(writeJson(extraction.getProjectManagementPersonnel()));
        company.setTaskId(task.getTaskId());
        company.setCreatedAt(now);
        company.setUpdatedAt(now);

        if (projectBidderCompanyMapper.countByKey(task.getProjectId(), task.getFileId()) > 0) {
            projectBidderCompanyMapper.updateByKey(company);
            log.info("Updated bidder company info: taskId={}, projectId={}, fileId={}, personnel={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(),
                    extraction.getProjectManagementPersonnel().size());
        } else {
            projectBidderCompanyMapper.insert(company);
            log.info("Inserted bidder company info: taskId={}, projectId={}, fileId={}, personnel={}",
                    task.getTaskId(), task.getProjectId(), task.getFileId(),
                    extraction.getProjectManagementPersonnel().size());
        }
        taskMapper.markSuccess(task.getTaskId(), writeJson(extraction), now);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("抽取结果 JSON 序列化失败", exception);
        }
    }
}
