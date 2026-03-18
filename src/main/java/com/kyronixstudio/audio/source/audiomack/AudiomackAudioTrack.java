package com.kyronixstudio.audio.source.audiomack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.kyronixstudio.audio.error.PluginException;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

public class AudiomackAudioTrack extends DelegatedAudioTrack {
    private final AudiomackAudioSourceManager sourceManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public AudiomackAudioTrack(AudioTrackInfo trackInfo, AudiomackAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String playbackUrl = resolvePlaybackUrl(httpInterface);
            if (playbackUrl == null || playbackUrl.isEmpty()) {
                throw new PluginException("Failed to find stream for Audiomack track: " + trackInfo.identifier, FriendlyException.Severity.SUSPICIOUS);
            }

            HttpGet get = new HttpGet(playbackUrl);
            get.setHeader("User-Agent", "Mozilla/5.0");
            try (var response = httpInterface.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Non-200 status fetching Audiomack stream: " + statusCode);
                }
                processDelegate(new com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack(trackInfo, response.getEntity().getContent()), executor);
            }
        } catch (Exception e) {
            throw new PluginException("Error playing Audiomack track", FriendlyException.Severity.FAULT, e);
        }
    }

    private String resolvePlaybackUrl(HttpInterface httpInterface) throws IOException {
        String apiUrl = "https://audiomack.com/api/music/url/song/" + trackInfo.identifier + "?extended=1";
        HttpGet get = new HttpGet(apiUrl);
        get.setHeader("User-Agent", "Mozilla/5.0");

        try (var response = httpInterface.execute(get)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Non-200 status fetching stream: " + response.getStatusLine().getStatusCode());
            }

            JsonNode root = mapper.readTree(response.getEntity().getContent());
            return root.path("url").asText();
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new AudiomackAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
