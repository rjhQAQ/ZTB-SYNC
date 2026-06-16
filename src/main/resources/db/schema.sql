-- 文件异步处理任务表：记录上传受理后的后台处理状态、错误信息和抽取结果摘要。
CREATE TABLE ztb_file_processing_task (
    task_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message CLOB,
    llm_raw_json CLOB,
    result_summary_json CLOB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

COMMENT ON TABLE ztb_file_processing_task IS '文件异步处理任务表';
COMMENT ON COLUMN ztb_file_processing_task.task_id IS '任务ID';
COMMENT ON COLUMN ztb_file_processing_task.project_id IS '项目ID';
COMMENT ON COLUMN ztb_file_processing_task.file_id IS '文件ID';
COMMENT ON COLUMN ztb_file_processing_task.file_name IS '文件名称';
COMMENT ON COLUMN ztb_file_processing_task.file_type IS '文件类型：TENDER 招标文件，BID 投标文件';
COMMENT ON COLUMN ztb_file_processing_task.status IS '处理状态：PENDING、PROCESSING、SUCCESS、FAILED、SUPERSEDED';
COMMENT ON COLUMN ztb_file_processing_task.error_message IS '失败错误信息';
COMMENT ON COLUMN ztb_file_processing_task.llm_raw_json IS 'LLM 原始抽取 JSON';
COMMENT ON COLUMN ztb_file_processing_task.result_summary_json IS '抽取结果摘要 JSON';
COMMENT ON COLUMN ztb_file_processing_task.created_at IS '创建时间';
COMMENT ON COLUMN ztb_file_processing_task.updated_at IS '更新时间';
COMMENT ON COLUMN ztb_file_processing_task.started_at IS '开始处理时间';
COMMENT ON COLUMN ztb_file_processing_task.finished_at IS '处理完成时间';

-- 按业务键和创建时间查询最新任务。
CREATE INDEX idx_ztb_task_business
    ON ztb_file_processing_task (project_id, file_id, file_type, created_at);

-- 项目信息表：保存招标文件抽取出的项目基础信息和项目相关时间点。
CREATE TABLE ztb_project_info (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    tender_company_name VARCHAR(512),
    agency_name VARCHAR(512),
    project_name VARCHAR(512),
    bid_submit_start_time TIMESTAMP,
    bid_submit_end_time TIMESTAMP,
    range_start_time TIMESTAMP,
    range_end_time TIMESTAMP,
    all_time_points_json CLOB,
    task_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_ztb_project_info_file UNIQUE (project_id, file_id)
);

COMMENT ON TABLE ztb_project_info IS '项目信息表，保存招标文件抽取结果';
COMMENT ON COLUMN ztb_project_info.id IS 'UUID 主键';
COMMENT ON COLUMN ztb_project_info.project_id IS '项目ID';
COMMENT ON COLUMN ztb_project_info.file_id IS '招标文件ID';
COMMENT ON COLUMN ztb_project_info.file_name IS '招标文件名称';
COMMENT ON COLUMN ztb_project_info.tender_company_name IS '招标企业名称';
COMMENT ON COLUMN ztb_project_info.agency_name IS '代理机构名称';
COMMENT ON COLUMN ztb_project_info.project_name IS '项目名称';
COMMENT ON COLUMN ztb_project_info.bid_submit_start_time IS '投标文件递交开始时间';
COMMENT ON COLUMN ztb_project_info.bid_submit_end_time IS '投标文件递交结束时间';
COMMENT ON COLUMN ztb_project_info.range_start_time IS '项目整体时间范围开始时间';
COMMENT ON COLUMN ztb_project_info.range_end_time IS '项目整体时间范围结束时间';
COMMENT ON COLUMN ztb_project_info.all_time_points_json IS '项目相关所有时间点 JSON';
COMMENT ON COLUMN ztb_project_info.task_id IS '来源处理任务ID';
COMMENT ON COLUMN ztb_project_info.created_at IS '创建时间';
COMMENT ON COLUMN ztb_project_info.updated_at IS '更新时间';

-- 项目投标企业表：保存投标文件抽取出的企业、联系方式、地址和项目管理人员信息。
CREATE TABLE ztb_project_bidder_company (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    bid_company_name VARCHAR(512),
    bidder_contact_phone VARCHAR(128),
    registered_address VARCHAR(1024),
    mailing_address VARCHAR(1024),
    project_management_personnel_json CLOB,
    task_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_ztb_bidder_file UNIQUE (project_id, file_id)
);

COMMENT ON TABLE ztb_project_bidder_company IS '项目投标企业表，保存投标文件抽取结果';
COMMENT ON COLUMN ztb_project_bidder_company.id IS 'UUID 主键';
COMMENT ON COLUMN ztb_project_bidder_company.project_id IS '项目ID';
COMMENT ON COLUMN ztb_project_bidder_company.file_id IS '投标文件ID';
COMMENT ON COLUMN ztb_project_bidder_company.file_name IS '投标文件名称';
COMMENT ON COLUMN ztb_project_bidder_company.bid_company_name IS '投标公司名称';
COMMENT ON COLUMN ztb_project_bidder_company.bidder_contact_phone IS '投标人联系电话';
COMMENT ON COLUMN ztb_project_bidder_company.registered_address IS '注册地址';
COMMENT ON COLUMN ztb_project_bidder_company.mailing_address IS '通信地址';
COMMENT ON COLUMN ztb_project_bidder_company.project_management_personnel_json IS '项目管理人员信息 JSON';
COMMENT ON COLUMN ztb_project_bidder_company.task_id IS '来源处理任务ID';
COMMENT ON COLUMN ztb_project_bidder_company.created_at IS '创建时间';
COMMENT ON COLUMN ztb_project_bidder_company.updated_at IS '更新时间';

-- 投标文件雷同分析结果表：保存同项目下两份投标文件的相似度、风险等级和命中片段。
CREATE TABLE ztb_bid_similarity_analysis (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    tender_file_id VARCHAR(128),
    tender_file_name VARCHAR(512),
    left_file_id VARCHAR(128) NOT NULL,
    left_file_name VARCHAR(512) NOT NULL,
    left_company_name VARCHAR(512),
    right_file_id VARCHAR(128) NOT NULL,
    right_file_name VARCHAR(512) NOT NULL,
    right_company_name VARCHAR(512),
    score DECIMAL(6, 2),
    risk_level VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    hit_fragments_json CLOB,
    llm_review_json CLOB,
    error_message CLOB,
    task_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_ztb_bid_similarity_pair UNIQUE (project_id, left_file_id, right_file_id)
);

COMMENT ON TABLE ztb_bid_similarity_analysis IS '投标文件雷同分析结果表';
COMMENT ON COLUMN ztb_bid_similarity_analysis.id IS 'UUID 主键';
COMMENT ON COLUMN ztb_bid_similarity_analysis.project_id IS '项目ID';
COMMENT ON COLUMN ztb_bid_similarity_analysis.tender_file_id IS '参与降权的招标文件ID';
COMMENT ON COLUMN ztb_bid_similarity_analysis.tender_file_name IS '参与降权的招标文件名称';
COMMENT ON COLUMN ztb_bid_similarity_analysis.left_file_id IS '排序后左侧投标文件ID';
COMMENT ON COLUMN ztb_bid_similarity_analysis.left_file_name IS '排序后左侧投标文件名称';
COMMENT ON COLUMN ztb_bid_similarity_analysis.left_company_name IS '排序后左侧投标公司名称';
COMMENT ON COLUMN ztb_bid_similarity_analysis.right_file_id IS '排序后右侧投标文件ID';
COMMENT ON COLUMN ztb_bid_similarity_analysis.right_file_name IS '排序后右侧投标文件名称';
COMMENT ON COLUMN ztb_bid_similarity_analysis.right_company_name IS '排序后右侧投标公司名称';
COMMENT ON COLUMN ztb_bid_similarity_analysis.score IS '雷同分析得分，0 到 100';
COMMENT ON COLUMN ztb_bid_similarity_analysis.risk_level IS '风险等级：LOW、SUSPECTED、HIGH';
COMMENT ON COLUMN ztb_bid_similarity_analysis.status IS '分析状态：SUCCESS、FAILED';
COMMENT ON COLUMN ztb_bid_similarity_analysis.hit_fragments_json IS '命中的原文片段 JSON';
COMMENT ON COLUMN ztb_bid_similarity_analysis.llm_review_json IS 'LLM 复核结果 JSON';
COMMENT ON COLUMN ztb_bid_similarity_analysis.error_message IS '分析失败错误信息';
COMMENT ON COLUMN ztb_bid_similarity_analysis.task_id IS '触发本次分析的处理任务ID';
COMMENT ON COLUMN ztb_bid_similarity_analysis.created_at IS '创建时间';
COMMENT ON COLUMN ztb_bid_similarity_analysis.updated_at IS '更新时间';

-- 按项目、分数、风险等级和状态查询雷同分析结果。
CREATE INDEX idx_ztb_bid_similarity_project
    ON ztb_bid_similarity_analysis (project_id, score, risk_level, status);
