/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast.rdioscanner;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Streaming configuration for Rdio Scanner API.
 *
 * Note: this API is not a streaming audio service, rather a completed call push service.  However, it fits nicely
 * with the structure of the audio streaming subsystem in sdrtrunk
 */
public class RdioScannerConfiguration extends BroadcastConfiguration
{
    private IntegerProperty mSystemID = new SimpleIntegerProperty();
    private StringProperty mApiKey = new SimpleStringProperty();

    /**
     * Constructor for faster jackson
     */
    public RdioScannerConfiguration()
    {
        this(BroadcastFormat.MP3);
    }

    /**
     * Public constructor.
     * @param format to use for audio recording (MP3)
     */
    public RdioScannerConfiguration(BroadcastFormat format)
    {
        super(format);

        //The parent class binds this property, so we unbind it and rebind it here
        mValid.unbind();
        mValid.bind(Bindings.and(Bindings.and(Bindings.greaterThan(mSystemID, 0), Bindings.isNotNull(mApiKey)),
            Bindings.isNotNull(mHost)));

        if(mHost.getValue() == null || mHost.getValue().isEmpty())
        {
            mHost.set("http://localhost");
        }
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
     * System ID as provided by RdioScanner.com
     */
    @JacksonXmlProperty(isAttribute = true, localName = "system_id")
    public int getSystemID()
    {
        return mSystemID.get();
    }

    /**
     * Sets the system ID provided by RdioScanner.com
     */
    public void setSystemID(int systemID)
    {
        mSystemID.set(systemID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.RDIOSCANNER_CALL;
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        RdioScannerConfiguration copy = new RdioScannerConfiguration();
        copy.setSystemID(getSystemID());
        return copy;
    }
}
