package com.kyronixstudio.audio.source.gaana;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.*;
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

public class GaanaAudioSourceManager implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(GaanaAudioSourceManager.class);
    private static final String SEARCH_PREFIX = "gaanasearch:";
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(www\\.)?gaana\\.com/(song|album|playlist)/([a-zA-Z0-9-]+)/?$");

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpInterfaceManager httpInterfaceManager;

    public GaanaAudioSourceManager(PluginConfig config) {
        this.httpClient = new HttpClient(config);
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "gaana";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return getSearch(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
            }

            Matcher matcher = URL_PATTERN.matcher(reference.identifier);
            if (matcher.matches()) {
                String type = matcher.group(3);
                String id = matcher.group(4);
                return getURLInfo(reference.identifier, type, id);
            }
        } catch (Exception e) {
            log.error("Failed to load Gaana item: {}", reference.identifier, e);
            throw new PluginException("Failed to resolve Gaana source: " + e.getMessage(), FriendlyException.Severity.SUSPICIOUS, e);
        }
        return null;
    }

    private AudioItem getSearch(String query) throws IOException {
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        String url = "https://gsearch.gaana.com/prod/autocomplete/" + encodedQuery;
        String json = httpClient.get(url, null);
        
        JsonNode root = mapper.readTree(json);
        List<AudioTrack> tracks = new ArrayList<>();
        
        if (root.has("gr")) {
            for (JsonNode node : root.get("gr")) {
                if ("Track".equals(node.path("t").asText())) {
                    for (JsonNode item : node.path("rr")) {
                        String title = item.path("tn").asText();
                        String artist = item.path("an").asText();
                        String identifier = item.path("tl").asText();
                        if (!identifier.isEmpty()) {
                            tracks.add(buildTrack(identifier, title, artist, 0L));
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist("Gaana Search Results for: " + query, tracks, tracks.get(0), true);
    }

    private AudioItem getURLInfo(String url, String type, String id) throws IOException {
        String html = httpClient.get(url, null);
        Document doc = Jsoup.parse(html);
        
        String scriptJson = doc.select("script#__NEXT_DATA__").html();
        if (scriptJson == null || scriptJson.isEmpty()) {
            throw new PluginException("Could not find metadata on Gaana page", FriendlyException.Severity.SUSPICIOUS);
        }

        JsonNode root = mapper.readTree(scriptJson);
        JsonNode pageProps = root.path("props").path("pageProps");

        if ("song".equals(type)) {
            JsonNode songData = pageProps.path("songDetails");
            return buildTrack(
                    songData.path("seokey").asText(id),
                    songData.path("title").asText("Unknown Title"),
                    songData.path("artist").get(0).path("name").asText("Unknown Artist"),
                    songData.path("duration").asLong(0) * 1000L
            );
        } else if ("album".equals(type) || "playlist".equals(type)) {
            JsonNode listData = pageProps.path("albumDetails");
            if (listData.isMissingNode()) listData = pageProps.path("playlistDetails");
            
            String title = listData.path("title").asText("Unknown Collection");
            List<AudioTrack> tracks = new ArrayList<>();
            for (JsonNode trackNode : listData.path("tracks")) {
                tracks.add(buildTrack(
                        trackNode.path("seokey").asText(),
                        trackNode.path("title").asText(),
                        trackNode.path("artist").get(0).path("name").asText(),
                        trackNode.path("duration").asLong(0) * 1000L
                ));
            }
            return new BasicAudioPlaylist(title, tracks, null, false);
        }

        return AudioReference.NO_TRACK;
    }

    private AudioTrack buildTrack(String identifier, String title, String author, long duration) {
        AudioTrackInfo info = new AudioTrackInfo(title, author, duration, identifier, false, "https://gaana.com/song/" + identifier);
        return new GaanaAudioTrack(info, this);
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
        return new GaanaAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        httpInterfaceManager.close();
    }
    
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}
