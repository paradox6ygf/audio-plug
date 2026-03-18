package com.kyronixstudio.audio.error;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

public class PluginException extends FriendlyException {
    
    public PluginException(String message, FriendlyException.Severity severity) {
        super(message, severity, null);
    }
    
    public PluginException(String message, FriendlyException.Severity severity, Throwable cause) {
        super(message, severity, cause);
    }
}
