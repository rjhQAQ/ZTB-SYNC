package org.example.ztbsync.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ztb")
public class ZtbProperties {

    private final FileService fileService = new FileService();
    private final Llm llm = new Llm();
    private final Async async = new Async();
    private final Similarity similarity = new Similarity();
    private final Embedding embedding = new Embedding();
    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final RagSearch ragSearch = new RagSearch();
    private final Rerank rerank = new Rerank();

    public FileService getFileService() {
        return fileService;
    }

    public Llm getLlm() {
        return llm;
    }

    public Async getAsync() {
        return async;
    }

    public Similarity getSimilarity() {
        return similarity;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public RagSearch getRagSearch() {
        return ragSearch;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public static class FileService {
        private String downloadUrlTemplate = "http://127.0.0.1:8081/files/download";
        private boolean trustAllSsl;

        public String getDownloadUrlTemplate() {
            return downloadUrlTemplate;
        }

        public void setDownloadUrlTemplate(String downloadUrlTemplate) {
            this.downloadUrlTemplate = downloadUrlTemplate;
        }

        public boolean isTrustAllSsl() {
            return trustAllSsl;
        }

        public void setTrustAllSsl(boolean trustAllSsl) {
            this.trustAllSsl = trustAllSsl;
        }
    }

    public static class Llm {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String model = "gpt-4o-mini";
        private Duration timeout = Duration.ofSeconds(30);
        private int maxInputChars = 20_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxInputChars() {
            return maxInputChars;
        }

        public void setMaxInputChars(int maxInputChars) {
            this.maxInputChars = maxInputChars;
        }
    }

    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 200;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Similarity {
        private boolean enabled = true;
        private double segmentMatchThreshold = 0.82;
        private double tenderMatchThreshold = 0.85;
        private double tenderDerivedWeight = 0.2;
        private double suspectedThreshold = 70;
        private double highRiskThreshold = 85;
        private int topHitLimit = 20;
        private int minSegmentChars = 30;
        private boolean llmReviewEnabled;
        private double llmReviewMinScore = 70;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSegmentMatchThreshold() {
            return segmentMatchThreshold;
        }

        public void setSegmentMatchThreshold(double segmentMatchThreshold) {
            this.segmentMatchThreshold = segmentMatchThreshold;
        }

        public double getTenderMatchThreshold() {
            return tenderMatchThreshold;
        }

        public void setTenderMatchThreshold(double tenderMatchThreshold) {
            this.tenderMatchThreshold = tenderMatchThreshold;
        }

        public double getTenderDerivedWeight() {
            return tenderDerivedWeight;
        }

        public void setTenderDerivedWeight(double tenderDerivedWeight) {
            this.tenderDerivedWeight = tenderDerivedWeight;
        }

        public double getSuspectedThreshold() {
            return suspectedThreshold;
        }

        public void setSuspectedThreshold(double suspectedThreshold) {
            this.suspectedThreshold = suspectedThreshold;
        }

        public double getHighRiskThreshold() {
            return highRiskThreshold;
        }

        public void setHighRiskThreshold(double highRiskThreshold) {
            this.highRiskThreshold = highRiskThreshold;
        }

        public int getTopHitLimit() {
            return topHitLimit;
        }

        public void setTopHitLimit(int topHitLimit) {
            this.topHitLimit = topHitLimit;
        }

        public int getMinSegmentChars() {
            return minSegmentChars;
        }

        public void setMinSegmentChars(int minSegmentChars) {
            this.minSegmentChars = minSegmentChars;
        }

        public boolean isLlmReviewEnabled() {
            return llmReviewEnabled;
        }

        public void setLlmReviewEnabled(boolean llmReviewEnabled) {
            this.llmReviewEnabled = llmReviewEnabled;
        }

        public double getLlmReviewMinScore() {
            return llmReviewMinScore;
        }

        public void setLlmReviewMinScore(double llmReviewMinScore) {
            this.llmReviewMinScore = llmReviewMinScore;
        }
    }

    public static class Embedding {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String model = "bge-base-zh-v1.5";
        private String endpoint = "/v1/embeddings";
        private int batchSize = 16;
        private int vectorDims = 768;
        private int targetChunkChars = 500;
        private int maxChunkChars = 800;
        private int minChunkChars = 80;
        private int overlapChars = 120;
        private boolean filterBoilerplate = true;
        private boolean keepBoilerplate = true;
        private Duration timeout = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getVectorDims() {
            return vectorDims;
        }

        public void setVectorDims(int vectorDims) {
            this.vectorDims = vectorDims;
        }

        public int getTargetChunkChars() {
            return targetChunkChars;
        }

        public void setTargetChunkChars(int targetChunkChars) {
            this.targetChunkChars = targetChunkChars;
        }

        public int getMaxChunkChars() {
            return maxChunkChars;
        }

        public void setMaxChunkChars(int maxChunkChars) {
            this.maxChunkChars = maxChunkChars;
        }

        public int getMinChunkChars() {
            return minChunkChars;
        }

        public void setMinChunkChars(int minChunkChars) {
            this.minChunkChars = minChunkChars;
        }

        public int getOverlapChars() {
            return overlapChars;
        }

        public void setOverlapChars(int overlapChars) {
            this.overlapChars = overlapChars;
        }

        public boolean isFilterBoilerplate() {
            return filterBoilerplate;
        }

        public void setFilterBoilerplate(boolean filterBoilerplate) {
            this.filterBoilerplate = filterBoilerplate;
        }

        public boolean isKeepBoilerplate() {
            return keepBoilerplate;
        }

        public void setKeepBoilerplate(boolean keepBoilerplate) {
            this.keepBoilerplate = keepBoilerplate;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class RagSearch {
        private String mode = "HYBRID";
        private int defaultTopK = 3;
        private int maxTopK = 20;
        private boolean includeBoilerplate;
        private boolean filterModel = true;
        private double minScore;
        private int vectorCandidateSize = 50;
        private int keywordCandidateSize = 50;
        private int rrfRankConstant = 60;
        private double vectorWeight = 0.6;
        private double keywordWeight = 0.4;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public int getMaxTopK() {
            return maxTopK;
        }

        public void setMaxTopK(int maxTopK) {
            this.maxTopK = maxTopK;
        }

        public boolean isIncludeBoilerplate() {
            return includeBoilerplate;
        }

        public void setIncludeBoilerplate(boolean includeBoilerplate) {
            this.includeBoilerplate = includeBoilerplate;
        }

        public boolean isFilterModel() {
            return filterModel;
        }

        public void setFilterModel(boolean filterModel) {
            this.filterModel = filterModel;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        public int getVectorCandidateSize() {
            return vectorCandidateSize;
        }

        public void setVectorCandidateSize(int vectorCandidateSize) {
            this.vectorCandidateSize = vectorCandidateSize;
        }

        public int getKeywordCandidateSize() {
            return keywordCandidateSize;
        }

        public void setKeywordCandidateSize(int keywordCandidateSize) {
            this.keywordCandidateSize = keywordCandidateSize;
        }

        public int getRrfRankConstant() {
            return rrfRankConstant;
        }

        public void setRrfRankConstant(int rrfRankConstant) {
            this.rrfRankConstant = rrfRankConstant;
        }

        public double getVectorWeight() {
            return vectorWeight;
        }

        public void setVectorWeight(double vectorWeight) {
            this.vectorWeight = vectorWeight;
        }

        public double getKeywordWeight() {
            return keywordWeight;
        }

        public void setKeywordWeight(double keywordWeight) {
            this.keywordWeight = keywordWeight;
        }
    }

    public static class Rerank {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private String model = "bge-rerank";
        private String endpoint = "/v1/rerank";
        private Duration timeout = Duration.ofSeconds(30);
        private int candidateSize = 50;
        private boolean fallbackToHybrid = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getCandidateSize() {
            return candidateSize;
        }

        public void setCandidateSize(int candidateSize) {
            this.candidateSize = candidateSize;
        }

        public boolean isFallbackToHybrid() {
            return fallbackToHybrid;
        }

        public void setFallbackToHybrid(boolean fallbackToHybrid) {
            this.fallbackToHybrid = fallbackToHybrid;
        }
    }

    public static class Elasticsearch {
        private String baseUrl;
        private String username;
        private String password;
        private String indexName = "ztb_file_embedding_v2";
        private String projectNameIndexName = "ztb_tender_project_name";
        private String projectNameAnalyzer = "ik_max_word";
        private String projectNameSearchAnalyzer = "ik_smart";
        private String chunkAnalyzer = "ik_max_word";
        private String chunkSearchAnalyzer = "ik_smart";
        private boolean autoCreateIndex = true;
        private Duration timeout = Duration.ofSeconds(30);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public String getProjectNameIndexName() {
            return projectNameIndexName;
        }

        public void setProjectNameIndexName(String projectNameIndexName) {
            this.projectNameIndexName = projectNameIndexName;
        }

        public String getProjectNameAnalyzer() {
            return projectNameAnalyzer;
        }

        public void setProjectNameAnalyzer(String projectNameAnalyzer) {
            this.projectNameAnalyzer = projectNameAnalyzer;
        }

        public String getProjectNameSearchAnalyzer() {
            return projectNameSearchAnalyzer;
        }

        public void setProjectNameSearchAnalyzer(String projectNameSearchAnalyzer) {
            this.projectNameSearchAnalyzer = projectNameSearchAnalyzer;
        }

        public String getChunkAnalyzer() {
            return chunkAnalyzer;
        }

        public void setChunkAnalyzer(String chunkAnalyzer) {
            this.chunkAnalyzer = chunkAnalyzer;
        }

        public String getChunkSearchAnalyzer() {
            return chunkSearchAnalyzer;
        }

        public void setChunkSearchAnalyzer(String chunkSearchAnalyzer) {
            this.chunkSearchAnalyzer = chunkSearchAnalyzer;
        }

        public boolean isAutoCreateIndex() {
            return autoCreateIndex;
        }

        public void setAutoCreateIndex(boolean autoCreateIndex) {
            this.autoCreateIndex = autoCreateIndex;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
