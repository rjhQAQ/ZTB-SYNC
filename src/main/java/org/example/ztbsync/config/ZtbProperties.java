package org.example.ztbsync.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ztb")
public class ZtbProperties {

    private final FileService fileService = new FileService();
    private final Llm llm = new Llm();
    private final Async async = new Async();

    public FileService getFileService() {
        return fileService;
    }

    public Llm getLlm() {
        return llm;
    }

    public Async getAsync() {
        return async;
    }

    public static class FileService {
        private String downloadUrlTemplate = "http://127.0.0.1:8081/files/{fileId}";

        public String getDownloadUrlTemplate() {
            return downloadUrlTemplate;
        }

        public void setDownloadUrlTemplate(String downloadUrlTemplate) {
            this.downloadUrlTemplate = downloadUrlTemplate;
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
}
