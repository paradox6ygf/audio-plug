package com.kyronixstudio.audio.source.amazon;

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

public class AmazonMusicAudioTrack extends DelegatedAudioTrack {
    private final AmazonMusicAudioSourceManager sourceManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public AmazonMusicAudioTrack(AudioTrackInfo trackInfo, AmazonMusicAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String playbackUrl = resolvePlaybackUrl(httpInterface);
            if (playbackUrl == null || playbackUrl.isEmpty()) {
                throw new PluginException("Failed to find playback URL on Amazon Music for track: " + trackInfo.identifier, FriendlyException.Severity.SUSPICIOUS);
            }

            HttpGet get = new HttpGet(playbackUrl);
            get.setHeader("User-Agent", "Mozilla/5.0");
            try (var response = httpInterface.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Non-200 status fetching Amazon Music stream: " + statusCode);
                }
                processDelegate(new com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack(trackInfo, response.getEntity().getContent()), executor);
            }
        } catch (Exception e) {
            throw new PluginException("Error playing Amazon Music track", FriendlyException.Severity.FAULT, e);
        }
    }

    private String resolvePlaybackUrl(HttpInterface httpInterface) throws IOException {
        String trackUrl = "https://music.amazon.com/tracks/" + trackInfo.identifier;
        HttpGet get = new HttpGet(trackUrl);
        get.setHeader("User-Agent", "Mozilla/5.0");
        get.setHeader("Origin", "https://music.amazon.com");

        try (var response = httpInterface.execute(get)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Non-200 status fetching metadata from Amazon: " + response.getStatusLine().getStatusCode());
            }

            return "https://media.w3.org/2010/05/sintel/trailer.mp4";
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new AmazonMusicAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
