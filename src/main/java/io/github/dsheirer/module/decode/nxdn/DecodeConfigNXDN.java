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
package io.github.dsheirer.module.decode.nxdn;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * NXDN decoder configuration
 */
public class DecodeConfigNXDN extends DecodeConfiguration
{
    private static final ChannelSpecification CHANNEL_4800 = new ChannelSpecification(12500.0, 6250, 3000.0, 3125.0);
    private static final ChannelSpecification CHANNEL_9600 = new ChannelSpecification(25000.0, 12500, 5750.0, 6250.0);
    private TransmissionMode mTransmissionMode;

    /**
     * Default constructor for Jackson.
     */
    public DecodeConfigNXDN()
    {
        this(TransmissionMode.M9600);
    }

    /**
     * Constructs an instance
     * @param transmissionMode for the system
     */
    public DecodeConfigNXDN(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;
    }

    /**
     * Transmission mode for this system.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "mode")
    public TransmissionMode getTransmissionMode()
    {
        return mTransmissionMode;
    }

    /**
     * Sets/changes the transmission mode
     * @param transmissionMode to use
     */
    public void setTransmissionMode(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;
    }

    @JsonIgnore
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NXDN;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return getTransmissionMode() == TransmissionMode.M4800 ? CHANNEL_4800 : CHANNEL_9600;
    }
}
