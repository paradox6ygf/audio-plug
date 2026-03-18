package com.kyronixstudio.audio.source.gaana;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class GaanaAudioTrack extends DelegatedAudioTrack {
    private final GaanaAudioSourceManager sourceManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public GaanaAudioTrack(AudioTrackInfo trackInfo, GaanaAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String playbackUrl = resolvePlaybackUrl(httpInterface);
            if (playbackUrl == null || playbackUrl.isEmpty()) {
                throw new PluginException("Failed to find playback URL on Gaana for track: " + trackInfo.identifier, FriendlyException.Severity.SUSPICIOUS);
            }

            HttpGet get = new HttpGet(playbackUrl);
            try (var response = httpInterface.execute(get)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Non-200 status fetching Gaana stream: " + statusCode);
                }
                processDelegate(new com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack(trackInfo, response.getEntity().getContent()), executor);
            }
        } catch (Exception e) {
            throw new PluginException("Error playing Gaana track", FriendlyException.Severity.FAULT, e);
        }
    }

    private String resolvePlaybackUrl(HttpInterface httpInterface) throws IOException {
        String trackUrl = "https://gaana.com/song/" + trackInfo.identifier;
        HttpGet get = new HttpGet(trackUrl);
        get.setHeader("User-Agent", "Mozilla/5.0");

        try (var response = httpInterface.execute(get)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Non-200 status fetching metadata: " + response.getStatusLine().getStatusCode());
            }

            Document doc = Jsoup.parse(response.getEntity().getContent(), "UTF-8", trackUrl);
            String scriptJson = doc.select("script#__NEXT_DATA__").html();
            if (scriptJson.isEmpty()) return null;

            JsonNode root = mapper.readTree(scriptJson);
            JsonNode songData = root.path("props").path("pageProps").path("songDetails");
            return songData.path("path").asText();
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new GaanaAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
