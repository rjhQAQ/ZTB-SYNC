package org.example.ztbsync.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.ztbsync.domain.BidSimilarityAnalysis;

/**
 * 投标文件雷同分析结果 Mapper。
 */
@Mapper
public interface BidSimilarityAnalysisMapper {

    String COLUMNS = """
            id, project_id, tender_file_id, tender_file_name,
            left_file_id, left_file_name, left_company_name,
            right_file_id, right_file_name, right_company_name,
            score, risk_level, status, hit_fragments_json, llm_review_json,
            error_message, task_id, created_at, updated_at
            """;

    @Select("""
            SELECT COUNT(1)
            FROM ztb_bid_similarity_analysis
            WHERE project_id = #{projectId}
              AND left_file_id = #{leftFileId}
              AND right_file_id = #{rightFileId}
            """)
    int countByPair(
            @Param("projectId") String projectId,
            @Param("leftFileId") String leftFileId,
            @Param("rightFileId") String rightFileId);

    @Insert("""
            INSERT INTO ztb_bid_similarity_analysis (
                project_id, tender_file_id, tender_file_name,
                left_file_id, left_file_name, left_company_name,
                right_file_id, right_file_name, right_company_name,
                score, risk_level, status, hit_fragments_json, llm_review_json,
                error_message, task_id, created_at, updated_at
            ) VALUES (
                #{projectId}, #{tenderFileId}, #{tenderFileName},
                #{leftFileId}, #{leftFileName}, #{leftCompanyName},
                #{rightFileId}, #{rightFileName}, #{rightCompanyName},
                #{score}, #{riskLevel}, #{status}, #{hitFragmentsJson}, #{llmReviewJson},
                #{errorMessage}, #{taskId}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(BidSimilarityAnalysis analysis);

    @Update("""
            UPDATE ztb_bid_similarity_analysis
            SET tender_file_id = #{tenderFileId},
                tender_file_name = #{tenderFileName},
                left_file_name = #{leftFileName},
                left_company_name = #{leftCompanyName},
                right_file_name = #{rightFileName},
                right_company_name = #{rightCompanyName},
                score = #{score},
                risk_level = #{riskLevel},
                status = #{status},
                hit_fragments_json = #{hitFragmentsJson},
                llm_review_json = #{llmReviewJson},
                error_message = #{errorMessage},
                task_id = #{taskId},
                updated_at = #{updatedAt}
            WHERE project_id = #{projectId}
              AND left_file_id = #{leftFileId}
              AND right_file_id = #{rightFileId}
            """)
    int updateByPair(BidSimilarityAnalysis analysis);

    @Select("""
            <script>
            SELECT id, project_id, tender_file_id, tender_file_name,
                   left_file_id, left_file_name, left_company_name,
                   right_file_id, right_file_name, right_company_name,
                   score, risk_level, status, hit_fragments_json, llm_review_json,
                   error_message, task_id, created_at, updated_at
            FROM ztb_bid_similarity_analysis
            WHERE project_id = #{projectId}
            <if test="minScore != null">
              AND score &gt;= #{minScore}
            </if>
            <if test="riskLevel != null and riskLevel != ''">
              AND risk_level = #{riskLevel}
            </if>
            <if test="status != null and status != ''">
              AND status = #{status}
            </if>
            ORDER BY score DESC, updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<BidSimilarityAnalysis> findByProject(
            @Param("projectId") String projectId,
            @Param("minScore") Double minScore,
            @Param("riskLevel") String riskLevel,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT id, project_id, tender_file_id, tender_file_name,
                   left_file_id, left_file_name, left_company_name,
                   right_file_id, right_file_name, right_company_name,
                   score, risk_level, status, hit_fragments_json, llm_review_json,
                   error_message, task_id, created_at, updated_at
            FROM ztb_bid_similarity_analysis
            WHERE project_id = #{projectId}
              AND (left_file_id = #{fileId} OR right_file_id = #{fileId})
            <if test="minScore != null">
              AND score &gt;= #{minScore}
            </if>
            <if test="riskLevel != null and riskLevel != ''">
              AND risk_level = #{riskLevel}
            </if>
            <if test="status != null and status != ''">
              AND status = #{status}
            </if>
            ORDER BY score DESC, updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<BidSimilarityAnalysis> findByProjectAndFile(
            @Param("projectId") String projectId,
            @Param("fileId") String fileId,
            @Param("minScore") Double minScore,
            @Param("riskLevel") String riskLevel,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
