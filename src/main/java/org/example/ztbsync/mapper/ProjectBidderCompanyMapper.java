package org.example.ztbsync.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.ztbsync.domain.ProjectBidderCompany;

/**
 * 投标企业业务表 Mapper。
 *
 * <p>业务唯一键为 project_id + file_id，重复上传时更新该记录。</p>
 */
@Mapper
public interface ProjectBidderCompanyMapper {

    /** 判断指定项目和文件是否已经有投标企业信息。 */
    @Select("""
            SELECT COUNT(1)
            FROM ztb_project_bidder_company
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
            """)
    int countByKey(@Param("projectId") String projectId, @Param("fileId") String fileId);

    /** 插入新的投标企业信息。 */
    @Insert("""
            INSERT INTO ztb_project_bidder_company (
                project_id, file_id, file_name,
                bid_company_name, bidder_contact_phone,
                registered_address, mailing_address,
                project_management_personnel_json,
                task_id, created_at, updated_at
            ) VALUES (
                #{projectId}, #{fileId}, #{fileName},
                #{bidCompanyName}, #{bidderContactPhone},
                #{registeredAddress}, #{mailingAddress},
                #{projectManagementPersonnelJson},
                #{taskId}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(ProjectBidderCompany company);

    /** 覆盖更新已有投标企业信息。 */
    @Update("""
            UPDATE ztb_project_bidder_company
            SET file_name = #{fileName},
                bid_company_name = #{bidCompanyName},
                bidder_contact_phone = #{bidderContactPhone},
                registered_address = #{registeredAddress},
                mailing_address = #{mailingAddress},
                project_management_personnel_json = #{projectManagementPersonnelJson},
                task_id = #{taskId},
                updated_at = #{updatedAt}
            WHERE project_id = #{projectId}
              AND file_id = #{fileId}
            """)
    int updateByKey(ProjectBidderCompany company);
}
