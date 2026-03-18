package com.kyronixstudio.audio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "plugins.kyronixstudio")
public class PluginConfig {
    private Sources sources = new Sources();
    private Http http = new Http();
    private Cache cache = new Cache();

    public Sources getSources() { return sources; }
    public void setSources(Sources sources) { this.sources = sources; }
    public Http getHttp() { return http; }
    public void setHttp(Http http) { this.http = http; }
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public static class Sources {
        private boolean gaanaEnabled = true;
        private boolean amazonEnabled = true;
        private boolean audiomackEnabled = true;

        public boolean isGaanaEnabled() { return gaanaEnabled; }
        public void setGaanaEnabled(boolean gaanaEnabled) { this.gaanaEnabled = gaanaEnabled; }
        public boolean isAmazonEnabled() { return amazonEnabled; }
        public void setAmazonEnabled(boolean amazonEnabled) { this.amazonEnabled = amazonEnabled; }
        public boolean isAudiomackEnabled() { return audiomackEnabled; }
        public void setAudiomackEnabled(boolean audiomackEnabled) { this.audiomackEnabled = audiomackEnabled; }
    }

    public static class Http {
        private int timeout = 15000;
        private int retries = 3;

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
    }

    public static class Cache {
        private int maxSize = 500;

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }
}
