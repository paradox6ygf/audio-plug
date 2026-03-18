package com.kyronixstudio.audio.source.audiomack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.kyronixstudio.audio.config.PluginConfig;
import com.kyronixstudio.audio.error.PluginException;
import com.kyronixstudio.audio.http.HttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AudiomackAudioSourceManager implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(AudiomackAudioSourceManager.class);
    private static final String SEARCH_PREFIX = "amacksearch:";
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(www\\.)?audiomack\\.com/([^/]+)/(song|album|playlist)/([^/?]+)/?$");

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpInterfaceManager httpInterfaceManager;

    public AudiomackAudioSourceManager(PluginConfig config) {
        this.httpClient = new HttpClient(config);
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "audiomack";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return getSearch(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
            }

            Matcher matcher = URL_PATTERN.matcher(reference.identifier);
            if (matcher.matches()) {
                String artist = matcher.group(3);
                String type = matcher.group(4);
                String id = matcher.group(5);
                return getURLInfo(reference.identifier, artist, type, id);
            }
        } catch (Exception e) {
            log.error("Failed to load Audiomack item: {}", reference.identifier, e);
            throw new PluginException("Failed to resolve Audiomack source: " + e.getMessage(), FriendlyException.Severity.SUSPICIOUS, e);
        }
        return null;
    }

    private AudioItem getSearch(String query) throws IOException {
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        String url = "https://audiomack.com/api/music/search?q=" + encodedQuery;
        String json = httpClient.get(url, null);
        
        JsonNode root = mapper.readTree(json);
        List<AudioTrack> tracks = new ArrayList<>();
        
        if (root.has("results")) {
            for (JsonNode item : root.path("results")) {
                if ("song".equals(item.path("type").asText())) {
                    String title = item.path("title").asText();
                    String artistName = item.path("artist").asText();
                    String identifier = item.path("id").asText();
                    String urlSlug = item.path("url_slug").asText();
                    if (!identifier.isEmpty()) {
                        tracks.add(buildTrack(identifier, title, artistName, 0L, urlSlug));
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist("Audiomack Search: " + query, tracks, tracks.get(0), true);
    }

    private AudioItem getURLInfo(String url, String artist, String type, String id) throws IOException {
        String html = httpClient.get(url, null);
        Document doc = Jsoup.parse(html);
        
        String jsData = doc.select("script#__NEXT_DATA__").html();
        if (jsData == null || jsData.isEmpty()) {
            throw new PluginException("Could not extract metadata from Audiomack URL", FriendlyException.Severity.SUSPICIOUS);
        }

        JsonNode root = mapper.readTree(jsData);
        JsonNode musicData = root.path("props").path("pageProps").path("music");

        if ("song".equals(type)) {
            return buildTrack(
                    musicData.path("id").asText(),
                    musicData.path("title").asText("Unknown Title"),
                    musicData.path("artist").asText(artist),
                    musicData.path("duration").asLong(0) * 1000L,
                    musicData.path("url_slug").asText()
            );
        } else if ("album".equals(type) || "playlist".equals(type)) {
            String title = musicData.path("title").asText("Unknown Collection");
            List<AudioTrack> tracks = new ArrayList<>();
            for (JsonNode trackNode : musicData.path("tracks")) {
                tracks.add(buildTrack(
                        trackNode.path("id").asText(),
                        trackNode.path("title").asText(),
                        trackNode.path("artist").asText(),
                        trackNode.path("duration").asLong(0) * 1000L,
                        trackNode.path("url_slug").asText()
                ));
            }
            return new BasicAudioPlaylist(title, tracks, null, false);
        }

        return AudioReference.NO_TRACK;
    }

    private AudioTrack buildTrack(String identifier, String title, String author, long duration, String urlSlug) {
        AudioTrackInfo info = new AudioTrackInfo(title, author, duration, identifier, false, "https://audiomack.com/" + author + "/song/" + urlSlug);
        return new AudiomackAudioTrack(info, this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new AudiomackAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        httpInterfaceManager.close();
    }
    
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}
