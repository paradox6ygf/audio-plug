package com.kyronixstudio.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.kyronixstudio.audio.config.PluginConfig;
import com.kyronixstudio.audio.source.gaana.GaanaAudioSourceManager;
import com.kyronixstudio.audio.source.amazon.AmazonMusicAudioSourceManager;
import com.kyronixstudio.audio.source.audiomack.AudiomackAudioSourceManager;

@Service
public class KyronixStudioPlugin implements AudioPlayerManagerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(KyronixStudioPlugin.class);
    private final PluginConfig config;

    public KyronixStudioPlugin(PluginConfig config) {
        this.config = config;
        log.info("KyronixStudioPlugin loaded. Gaana: {}, Amazon: {}, Audiomack: {}",
                config.getSources().isGaanaEnabled(), 
                config.getSources().isAmazonEnabled(),
                config.getSources().isAudiomackEnabled());
    }

    @Override
    public AudioPlayerManager configure(AudioPlayerManager manager) {
        if (config.getSources().isGaanaEnabled()) {
            manager.registerSourceManager(new GaanaAudioSourceManager(config));
        }
        if (config.getSources().isAmazonEnabled()) {
            manager.registerSourceManager(new AmazonMusicAudioSourceManager(config));
        }
        if (config.getSources().isAudiomackEnabled()) {
            manager.registerSourceManager(new AudiomackAudioSourceManager(config));
        }
        return manager;
    }
}
