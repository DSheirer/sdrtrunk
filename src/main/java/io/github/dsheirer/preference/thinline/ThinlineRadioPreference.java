/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.preference.thinline;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;

/**
 * Site-wide Thinline Radio health reporting. Call-upload streams keep their own API keys.
 */
public class ThinlineRadioPreference extends Preference
{
    private static final String ENABLED = "thinline.health.enabled";
    private static final String URL = "thinline.health.url";
    private static final String API_KEY = "thinline.health.api.key";
    private static final String ALERT_TUNER = "thinline.health.alert.tuner";
    private static final String ALERT_CONTROL = "thinline.health.alert.control";
    private static final String ALERT_UPLOADER = "thinline.health.alert.uploader";
    private static final String ALERT_OVERFLOW = "thinline.health.alert.overflow";
    private static final String ALERT_OFFLINE = "thinline.health.alert.offline";
    private static final String REMOTE = "thinline.remote.";
    private static final String REMOTE_SAVED = "thinline.remote.saved";

    /**
     * What the Thinline Radio admin page may do on this computer.  Viewing status is always allowed.
     */
    public enum Remote
    {
        DESKTOP("Remote desktop (see and control this screen, hear audio)"),
        SPECTRUM("Spectrum and waterfall"),
        TUNERS("Change tuner settings, frequency, PPM, enable or disable tuners"),
        CHANNELS("Start, stop, add, edit, and delete channels"),
        ALIASES("Edit aliases, talkgroups, and channel maps"),
        STREAMS("Edit streams and which talkgroups they upload"),
        PREFERENCES("Change user preferences");

        private final String mLabel;

        Remote(String label)
        {
            mLabel = label;
        }

        public String key()
        {
            return name().toLowerCase();
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    private final Preferences mPreferences = Preferences.userNodeForPackage(ThinlineRadioPreference.class);

    public ThinlineRadioPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
        keepEarlierRemoteDefaults(mPreferences);
    }

    /**
     * Remote control is off until the site owner allows it. Sites set up before that default existed had every
     * remote permission on, so the first run after an update saves those permissions as on and an update never
     * takes remote control away from a site that already had it.
     */
    static void keepEarlierRemoteDefaults(Preferences preferences)
    {
        if(preferences.getBoolean(REMOTE_SAVED, false))
        {
            return;
        }

        if(preferences.get(ENABLED, null) != null)
        {
            for(Remote remote: Remote.values())
            {
                if(preferences.get(REMOTE + remote.key(), null) == null)
                {
                    preferences.putBoolean(REMOTE + remote.key(), true);
                }
            }
        }

        preferences.putBoolean(REMOTE_SAVED, true);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.THINLINE_RADIO;
    }

    public boolean isEnabled()
    {
        return mPreferences.getBoolean(ENABLED, false);
    }

    public void setEnabled(boolean enabled)
    {
        mPreferences.putBoolean(ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public String getUrl()
    {
        return mPreferences.get(URL, "");
    }

    public void setUrl(String url)
    {
        mPreferences.put(URL, url == null ? "" : url.trim());
        notifyPreferenceUpdated();
    }

    public String getApiKey()
    {
        return mPreferences.get(API_KEY, "");
    }

    public void setApiKey(String apiKey)
    {
        mPreferences.put(API_KEY, apiKey == null ? "" : apiKey.trim());
        notifyPreferenceUpdated();
    }

    public boolean isAlertTuner()
    {
        return mPreferences.getBoolean(ALERT_TUNER, true);
    }

    public void setAlertTuner(boolean alert)
    {
        mPreferences.putBoolean(ALERT_TUNER, alert);
        notifyPreferenceUpdated();
    }

    public boolean isAlertControl()
    {
        return mPreferences.getBoolean(ALERT_CONTROL, true);
    }

    public void setAlertControl(boolean alert)
    {
        mPreferences.putBoolean(ALERT_CONTROL, alert);
        notifyPreferenceUpdated();
    }

    public boolean isAlertUploader()
    {
        return mPreferences.getBoolean(ALERT_UPLOADER, true);
    }

    public void setAlertUploader(boolean alert)
    {
        mPreferences.putBoolean(ALERT_UPLOADER, alert);
        notifyPreferenceUpdated();
    }

    public boolean isAlertOverflow()
    {
        return mPreferences.getBoolean(ALERT_OVERFLOW, true);
    }

    public void setAlertOverflow(boolean alert)
    {
        mPreferences.putBoolean(ALERT_OVERFLOW, alert);
        notifyPreferenceUpdated();
    }

    public boolean isAlertOffline()
    {
        return mPreferences.getBoolean(ALERT_OFFLINE, true);
    }

    public void setAlertOffline(boolean alert)
    {
        mPreferences.putBoolean(ALERT_OFFLINE, alert);
        notifyPreferenceUpdated();
    }

    public boolean isRemoteAllowed(Remote remote)
    {
        return isRemoteAllowed(mPreferences, remote);
    }

    static boolean isRemoteAllowed(Preferences preferences, Remote remote)
    {
        return preferences.getBoolean(REMOTE + remote.key(), false);
    }

    public void setRemoteAllowed(Remote remote, boolean allowed)
    {
        mPreferences.putBoolean(REMOTE + remote.key(), allowed);
        notifyPreferenceUpdated();
    }
}
