package org.example.ztbsync.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.ztbsync.domain.FileProcessingTask;

/**
 * 文件处理任务表 Mapper。
 *
 * <p>负责任务创建、状态流转、最新任务查询以及 LLM/结果摘要更新。</p>
 */
@Mapper
public interface FileProcessingTaskMapper {

    String COLUMNS = """
            task_id, project_id, file_id, file_name, file_type, status,
            error_message, llm_raw_json, result_summary_json,
            created_at, updated_at, started_at, finished_at
            """;

    @Insert("""
            INSERT INTO ztb_file_processing_task (
                task_id, project_id, file_id, file_name, file_type, status,
                error_message, llm_raw_json, result_summary_json,
                created_at, updated_at, started_at, finished_at
            ) VALUES (
                #{taskId}, #{projectId}, #{fileId}, #{fileName}, #{fileType}, #{status},
                #{errorMessage}, #{llmRawJson}, #{resultSummaryJson},
                #{createdAt}, #{updatedAt}, #{startedAt}, #{finishedAt}
            )
            """)
    int insert(FileProcessingTask task);

    /** 按任务 ID 查询单条任务。 */
    @Select("SELECT " + COLUMNS + " FROM ztb_file_processing_task WHERE task_id = #{taskId}")
    FileProcessingTask findById(@Param("taskId") String taskId);

    /** 按业务键查询任务列表，最新任务排在第一条。 */
    @Select("""
            SELECT task_id, project_id, file_id, file_name, file_type, status,
                   error_message, llm_raw_json, result_summary_json,
                   created_at, updated_at, started_at, finished_at
            FROM ztb_file_processing_task
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
              AND file_type = #{fileType}
            ORDER BY created_at DESC
            """)
    List<FileProcessingTask> findByBusinessKey(
            @Param("projectId") String projectId,
            @Param("fileId") String fileId,
            @Param("fileType") String fileType);

    /** 同一文件新任务创建后，将旧的待处理/处理中任务标记为已覆盖。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET status = 'SUPERSEDED',
                updated_at = #{updatedAt},
                finished_at = #{updatedAt}
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
              AND file_type = #{fileType}
              AND task_id <> #{currentTaskId}
              AND status IN ('PENDING', 'PROCESSING')
            """)
    int markOlderActiveTasksSuperseded(
            @Param("projectId") String projectId,
            @Param("fileId") String fileId,
            @Param("fileType") String fileType,
            @Param("currentTaskId") String currentTaskId,
            @Param("updatedAt") LocalDateTime updatedAt);

    /** 标记任务进入后台处理阶段。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET status = 'PROCESSING',
                started_at = #{startedAt},
                updated_at = #{startedAt}
            WHERE task_id = #{taskId}
            """)
    int markProcessing(@Param("taskId") String taskId, @Param("startedAt") LocalDateTime startedAt);

    /** 保存 LLM 原始 JSON，方便后续排查模型抽取效果。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET llm_raw_json = #{llmRawJson},
                updated_at = #{updatedAt}
            WHERE task_id = #{taskId}
            """)
    int updateLlmRawJson(
            @Param("taskId") String taskId,
            @Param("llmRawJson") String llmRawJson,
            @Param("updatedAt") LocalDateTime updatedAt);

    /** 标记任务成功，并保存最终抽取结果摘要。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET status = 'SUCCESS',
                error_message = NULL,
                result_summary_json = #{resultSummaryJson},
                updated_at = #{finishedAt},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId}
            """)
    int markSuccess(
            @Param("taskId") String taskId,
            @Param("resultSummaryJson") String resultSummaryJson,
            @Param("finishedAt") LocalDateTime finishedAt);

    /** 标记任务失败，并保存错误摘要。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET status = 'FAILED',
                error_message = #{errorMessage},
                updated_at = #{finishedAt},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId}
            """)
    int markFailed(
            @Param("taskId") String taskId,
            @Param("errorMessage") String errorMessage,
            @Param("finishedAt") LocalDateTime finishedAt);

    /** 标记任务已被更新任务覆盖。 */
    @Update("""
            UPDATE ztb_file_processing_task
            SET status = 'SUPERSEDED',
                updated_at = #{finishedAt},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId}
            """)
    int markSuperseded(@Param("taskId") String taskId, @Param("finishedAt") LocalDateTime finishedAt);
}
