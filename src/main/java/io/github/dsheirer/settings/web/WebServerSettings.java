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

package io.github.dsheirer.settings.web;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingsType;
import java.util.UUID;

/**
 * Web server and web API settings.
 *
 * The server is off by default and listens only on the loopback address when first turned on, so exposing it to the
 * network is a deliberate change.  A headless install can't use the settings dialog, so the launcher options
 * (--web, --web-bind, --web-port) override the stored values for the current run without changing them.
 *
 * Note: the TLS keystore password is stored in plain text in the Java preferences store, as is the database password.
 */
public class WebServerSettings extends AbstractSettings
{
    public static final boolean DEFAULT_ENABLED = false;
    public static final String LOOPBACK_ADDRESS = "127.0.0.1";
    public static final String ALL_ADDRESSES = "0.0.0.0";
    public static final String DEFAULT_BIND_ADDRESS = LOOPBACK_ADDRESS;
    public static final int DEFAULT_PORT = 8080;
    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65535;
    public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 60;
    public static final boolean DEFAULT_REGISTRATION_ENABLED = true;
    public static final int MIN_SESSION_TIMEOUT_MINUTES = 5;
    public static final int MAX_SESSION_TIMEOUT_MINUTES = 7 * 24 * 60;

    private Boolean mEnabledOverride;
    private String mBindAddressOverride;
    private Integer mPortOverride;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications whenever these settings change
     */
    public WebServerSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.WEB;
    }

    /**
     * Applies the launcher options for this run.  Null leaves the stored value in effect.
     * @param enabled override, or null
     * @param bindAddress override, or null
     * @param port override, or null
     */
    public void setOverrides(Boolean enabled, String bindAddress, Integer port)
    {
        mEnabledOverride = enabled;
        mBindAddressOverride = bindAddress;
        mPortOverride = port;
    }

    /**
     * Indicates if a launcher option overrides any of the stored values for this run.
     */
    public boolean hasOverrides()
    {
        return mEnabledOverride != null || mBindAddressOverride != null || mPortOverride != null;
    }

    /**
     * Indicates if the web server is enabled, including the launcher override.
     */
    public boolean isEnabled()
    {
        return mEnabledOverride != null ? mEnabledOverride : isStoredEnabled();
    }

    /**
     * Stored enabled value, ignoring the launcher override.
     */
    public boolean isStoredEnabled()
    {
        return Setting.WEB_ENABLED.getBoolean(DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled)
    {
        Setting.WEB_ENABLED.putBoolean(enabled);
        mEnabledOverride = null;
        notifySettingsUpdated();
    }

    /**
     * Address that the server listens on, including the launcher override.  127.0.0.1 is the local computer only and
     * 0.0.0.0 is every network interface.
     */
    public String getBindAddress()
    {
        return mBindAddressOverride != null ? mBindAddressOverride : getStoredBindAddress();
    }

    /**
     * Stored bind address, ignoring the launcher override.
     */
    public String getStoredBindAddress()
    {
        String address = Setting.WEB_BIND_ADDRESS.get(DEFAULT_BIND_ADDRESS);
        return address == null || address.isBlank() ? DEFAULT_BIND_ADDRESS : address.trim();
    }

    public void setBindAddress(String address)
    {
        Setting.WEB_BIND_ADDRESS.put(address == null || address.isBlank() ? DEFAULT_BIND_ADDRESS : address.trim());
        mBindAddressOverride = null;
        notifySettingsUpdated();
    }

    /**
     * TCP port that the server listens on, including the launcher override.
     */
    public int getPort()
    {
        return mPortOverride != null ? mPortOverride : getStoredPort();
    }

    /**
     * Stored port, ignoring the launcher override.
     */
    public int getStoredPort()
    {
        return Math.clamp(Setting.WEB_PORT.getInt(DEFAULT_PORT), MIN_PORT, MAX_PORT);
    }

    public void setPort(int port)
    {
        Setting.WEB_PORT.putInt(Math.clamp(port, MIN_PORT, MAX_PORT));
        mPortOverride = null;
        notifySettingsUpdated();
    }

    /**
     * Indicates if the server uses HTTPS with the keystore.
     */
    public boolean isTlsEnabled()
    {
        return Setting.WEB_TLS_ENABLED.getBoolean(false);
    }

    public void setTlsEnabled(boolean enabled)
    {
        Setting.WEB_TLS_ENABLED.putBoolean(enabled);
        notifySettingsUpdated();
    }

    /**
     * Path of the PKCS12 keystore with the server certificate and private key, or an empty string.
     */
    public String getTlsKeystorePath()
    {
        return Setting.WEB_TLS_KEYSTORE_PATH.get("");
    }

    public void setTlsKeystorePath(String path)
    {
        Setting.WEB_TLS_KEYSTORE_PATH.put(path == null ? "" : path.trim());
        notifySettingsUpdated();
    }

    /**
     * Password for the PKCS12 keystore, or an empty string.
     */
    public String getTlsKeystorePassword()
    {
        return Setting.WEB_TLS_KEYSTORE_PASSWORD.get("");
    }

    public void setTlsKeystorePassword(String password)
    {
        Setting.WEB_TLS_KEYSTORE_PASSWORD.put(password == null ? "" : password);
        notifySettingsUpdated();
    }

    /**
     * Minutes without a request after which a browser session ends.
     */
    public int getSessionTimeoutMinutes()
    {
        return Math.clamp(Setting.WEB_SESSION_TIMEOUT_MINUTES.getInt(DEFAULT_SESSION_TIMEOUT_MINUTES),
            MIN_SESSION_TIMEOUT_MINUTES, MAX_SESSION_TIMEOUT_MINUTES);
    }

    public void setSessionTimeoutMinutes(int minutes)
    {
        Setting.WEB_SESSION_TIMEOUT_MINUTES.putInt(Math.clamp(minutes, MIN_SESSION_TIMEOUT_MINUTES,
            MAX_SESSION_TIMEOUT_MINUTES));
        notifySettingsUpdated();
    }

    /**
     * Indicates if users may request accounts from the web sign-in page.  Requested accounts wait for an
     * administrator's approval.  The web server reads this on each request, so a change doesn't restart it.
     */
    public boolean isRegistrationEnabled()
    {
        return Setting.WEB_REGISTRATION_ENABLED.getBoolean(DEFAULT_REGISTRATION_ENABLED);
    }

    public void setRegistrationEnabled(boolean enabled)
    {
        Setting.WEB_REGISTRATION_ENABLED.putBoolean(enabled);
    }

    /**
     * Persistent identity of this sdrtrunk instance, so a web client that connects to several instances can tell them
     * apart.  Created on first use.
     */
    public String getInstanceId()
    {
        String id = Setting.WEB_INSTANCE_ID.get(null);

        if(id == null || id.isBlank())
        {
            id = UUID.randomUUID().toString();
            Setting.WEB_INSTANCE_ID.put(id);
        }

        return id;
    }
}
