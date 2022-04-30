/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast.broadcastify;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Streaming configuration for Broadcastify Calls API.
 *
 * Note: this API is not a streaming audio service, rather a completed call push service.  However, it fits nicely
 * with the structure of the audio streaming subsystem in sdrtrunk
 */
public class BroadcastifyCallConfiguration extends BroadcastConfiguration
{
    public static final String DEVELOPMENT_ENDPOINT = "https://api.broadcastify.com/call-upload-dev";
    public static final String PRODUCTION_ENDPOINT = "https://api.broadcastify.com/call-upload";

    private IntegerProperty mSystemID = new SimpleIntegerProperty();
    private StringProperty mApiKey = new SimpleStringProperty();
    private BooleanProperty mTestEnabled= new SimpleBooleanProperty(false);
    private IntegerProperty mTestInterval = new SimpleIntegerProperty(15);

    /**
     * Constructor for faster jackson
     */
    public BroadcastifyCallConfiguration()
    {
        this(BroadcastFormat.MP3);
    }

    /**
     * Public constructor.
     * @param format to use for audio recording (MP3)
     */
    public BroadcastifyCallConfiguration(BroadcastFormat format)
    {
        super(format);

        if(getHost() == null)
        {
            setHost(PRODUCTION_ENDPOINT);
        }

        //The parent class binds this property, so we unbind it and rebind it here
        mValid.unbind();
        mValid.bind(Bindings.and(Bindings.and(Bindings.greaterThan(mSystemID, 0), Bindings.isNotNull(mApiKey)),
            Bindings.isNotNull(mHost)));
    }

    /**
     * System ID as a property
     */
    public IntegerProperty systemIDProperty()
    {
        return mSystemID;
    }

    /**
     * API key as a property
     */
    public StringProperty apiKeyProperty()
    {
        return mApiKey;
    }

    /**
     * API Key
     */
    @JacksonXmlProperty(isAttribute = true, localName = "api_key")
    public String getApiKey()
    {
        return mApiKey.get();
    }

    /**
     * Sets the api key
     * @param apiKey
     */
    public void setApiKey(String apiKey)
    {
        mApiKey.setValue(apiKey);
    }

    /**
     * System ID as provided by broadcastify.com
     */
    @JacksonXmlProperty(isAttribute = true, localName = "system_id")
    public int getSystemID()
    {
        return mSystemID.get();
    }

    /**
     * Sets the system ID provided by broadcastify.com
     */
    public void setSystemID(int systemID)
    {
        mSystemID.set(systemID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.BROADCASTIFY_CALL;
    }

    /**
     * Indicates if this instance should schedule tests after connecting
     */
    @JacksonXmlProperty(isAttribute = true, localName = "test_enabled")
    public boolean isTestEnabled()
    {
        return mTestEnabled.get();
    }

    public void setTestEnabled(boolean enabled)
    {
        mTestEnabled.set(enabled);
    }

    /**
     * Indicates the interval at which tests should be performed while enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "test_interval")
    public int getTestInterval()
    {
        return mTestInterval.get();
    }

    public void setTestInterval(int interval)
    {
        mTestInterval.set(interval);
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        BroadcastifyCallConfiguration copy = new BroadcastifyCallConfiguration();
        copy.setSystemID(getSystemID());
        return copy;
    }
}
