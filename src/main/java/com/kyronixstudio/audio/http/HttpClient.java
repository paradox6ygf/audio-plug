package com.kyronixstudio.audio.http;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.kyronixstudio.audio.config.PluginConfig;
import com.kyronixstudio.audio.error.PluginException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
    private final OkHttpClient client;
    private final int maxRetries;

    public HttpClient(PluginConfig config) {
        this.maxRetries = config.getHttp().getRetries();
        int timeout = config.getHttp().getTimeout();
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public String get(String url, Headers headers) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        if (headers != null) {
            requestBuilder.headers(headers);
        } else {
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        }

        Request request = requestBuilder.build();
        return executeWithRetry(request);
    }

    private String executeWithRetry(Request request) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    log.warn("HTTP request failed with status code {}: {}", response.code(), request.url());
                }
            } catch (IOException e) {
                log.warn("HTTP connection error on attempt {}: {}", attempts + 1, e.getMessage());
            }
            attempts++;
            try {
                Thread.sleep(1000L * attempts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PluginException("HTTP thread interrupted", FriendlyException.Severity.SUSPICIOUS, e);
            }
        }
        throw new PluginException("Failed to fetch data from " + request.url() + " after " + maxRetries + " attempts.", FriendlyException.Severity.FAULT);
    }
}
