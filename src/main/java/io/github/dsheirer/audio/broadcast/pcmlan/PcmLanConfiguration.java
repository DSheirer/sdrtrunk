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
package io.github.dsheirer.audio.broadcast.pcmlan;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Configuration for the PCM-over-LAN broadcaster.  Streams raw 16-bit signed little-endian mono PCM samples
 * to a remote TCP listener (e.g. OpenToneDetect's lan_stream input source).  SDRTrunk acts as the TCP client
 * and the remote process is the TCP server.
 */
public class PcmLanConfiguration extends BroadcastConfiguration
{
    private static final int NATIVE_SAMPLE_RATE = 8000;
    private final IntegerProperty mOutputSampleRate = new SimpleIntegerProperty(NATIVE_SAMPLE_RATE);

    public PcmLanConfiguration()
    {
        this(BroadcastFormat.PCM_S16LE);
    }

    public PcmLanConfiguration(BroadcastFormat format)
    {
        super(BroadcastFormat.PCM_S16LE);

        mPort.set(9876);
        mValid.bind(Bindings.and(Bindings.isNotNull(mHost), Bindings.greaterThan(mPort, 0)));
    }

    @JacksonXmlProperty(isAttribute = true, localName = "output_sample_rate")
    public int getOutputSampleRate()
    {
        return mOutputSampleRate.get();
    }

    public void setOutputSampleRate(int rate)
    {
        mOutputSampleRate.set(rate);
    }

    public IntegerProperty outputSampleRateProperty()
    {
        return mOutputSampleRate;
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        PcmLanConfiguration copy = new PcmLanConfiguration(getBroadcastFormat());
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setDelay(getDelay());
        copy.setMaximumRecordingAge(getMaximumRecordingAge());
        copy.setOutputSampleRate(getOutputSampleRate());
        copy.setEnabled(false);
        return copy;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.PCM_LAN;
    }
}
