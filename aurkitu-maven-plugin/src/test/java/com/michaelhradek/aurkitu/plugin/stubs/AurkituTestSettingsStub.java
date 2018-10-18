package com.michaelhradek.aurkitu.plugin.stubs;

import org.apache.maven.settings.Settings;

public class AurkituTestSettingsStub extends Settings {

    /**
     * @see org.apache.maven.settings.Settings#isOffline()
     */
    public boolean isOffline() {
        return false;
    }

    /**
     * @see org.apache.maven.settings.Settings#isInteractiveMode()
     */
    public boolean isInteractiveMode() {
        return true;
    }
}
