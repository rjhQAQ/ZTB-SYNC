package org.example.ztbsync.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.ztbsync.domain.ProjectInfo;

/**
 * 招标项目业务表 Mapper。
 *
 * <p>业务唯一键为 project_id + file_id，重复上传时更新该记录。</p>
 */
@Mapper
public interface ProjectInfoMapper {

    /** 判断指定项目和文件是否已经有招标项目信息。 */
    @Select("""
            SELECT COUNT(1)
            FROM ztb_project_info
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
            """)
    int countByKey(@Param("projectId") String projectId, @Param("fileId") String fileId);

    /** 查询项目下的招标文件信息，最新记录排在前面。 */
    @Select("""
            SELECT id, project_id, file_id, file_name,
                   tender_company_name, agency_name, project_name,
                   bid_submit_start_time, bid_submit_end_time,
                   range_start_time, range_end_time, all_time_points_json,
                   task_id, created_at, updated_at
            FROM ztb_project_info
            WHERE project_id = #{projectId}
            ORDER BY updated_at DESC
            """)
    List<ProjectInfo> findByProjectId(@Param("projectId") String projectId);

    /** 插入新的招标项目信息。 */
    @Insert("""
            INSERT INTO ztb_project_info (
                project_id, file_id, file_name,
                tender_company_name, agency_name, project_name,
                bid_submit_start_time, bid_submit_end_time,
                range_start_time, range_end_time, all_time_points_json,
                task_id, created_at, updated_at
            ) VALUES (
                #{projectId}, #{fileId}, #{fileName},
                #{tenderCompanyName}, #{agencyName}, #{projectName},
                #{bidSubmitStartTime}, #{bidSubmitEndTime},
                #{rangeStartTime}, #{rangeEndTime}, #{allTimePointsJson},
                #{taskId}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(ProjectInfo projectInfo);

    /** 覆盖更新已有招标项目信息。 */
    @Update("""
            UPDATE ztb_project_info
            SET file_name = #{fileName},
                tender_company_name = #{tenderCompanyName},
                agency_name = #{agencyName},
                project_name = #{projectName},
                bid_submit_start_time = #{bidSubmitStartTime},
                bid_submit_end_time = #{bidSubmitEndTime},
                range_start_time = #{rangeStartTime},
                range_end_time = #{rangeEndTime},
                all_time_points_json = #{allTimePointsJson},
                task_id = #{taskId},
                updated_at = #{updatedAt}
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
            """)
    int updateByKey(ProjectInfo projectInfo);
}
