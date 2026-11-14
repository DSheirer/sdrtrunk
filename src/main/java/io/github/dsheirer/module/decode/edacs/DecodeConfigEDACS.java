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

package io.github.dsheirer.module.decode.edacs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

/**
 * EDACS decode configuration with per-LCN frequency list.
 * Frequencies are stored as a comma-separated string in XML, index = LCN-1.
 * Example: "851175000,851225000,851425000,..." for MBTA.
 *
 * <p>Tested scope: this decoder has been developed and tested against one system only, an EDACS Extended Addressing
 * (EA) wideband (9600 baud) system carrying ProVoice digital voice.  Assignments by grant type (voice mode AUTO) work
 * on that system.  EDACS Standard/Networked addressing, analog voice traffic and EDACS narrowband have not been
 * tested, and narrowband is not implemented.</p>
 *
 * <p>The ProVoice voice marker default is the value seen on that one system.  Other systems may use another
 * value: see {@link #getProVoiceVoiceMarker()}.</p>
 */
public class DecodeConfigEDACS extends DecodeConfiguration
{
    private static final int MAX_LCN = 25;
    public static final int TRAFFIC_CHANNEL_LIMIT_DEFAULT_EDACS = 4;

    /**
     * Control channel rotation dwell (ms) when multiple control channel frequencies are configured.  Defaults to the
     * maximum so that a fading control channel isn't abandoned too quickly.
     */
    public static final int CHANNEL_ROTATION_DELAY_MINIMUM_MS = 400;
    public static final int CHANNEL_ROTATION_DELAY_DEFAULT_MS = 2000;
    public static final int CHANNEL_ROTATION_DELAY_MAXIMUM_MS = 2000;
    private String mLcnFrequencies = "";
    private VoiceMode mVoiceMode = VoiceMode.ANALOG;
    private int mTrafficChannelPoolSize = TRAFFIC_CHANNEL_LIMIT_DEFAULT_EDACS;

    /**
     * ProVoice voice frame marker.  This is the value observed on the one system this decoder was tested against, and
     * the value DSD-FME provoice.c calls usual.  It has not been confirmed on any other system.
     */
    public static final String PROVOICE_VOICE_MARKER_DEFAULT = "2175";

    /** Value of the voice marker when frames are not checked for it */
    public static final int PROVOICE_VOICE_MARKER_NONE = -1;
    private String mProVoiceVoiceMarker = PROVOICE_VOICE_MARKER_DEFAULT;

    public enum VoiceMode
    {
        /** Analog FM only (NBFM) */
        ANALOG("Analog (NBFM)"),
        /** ProVoice digital (IMBE 7100 via JMBE) */
        PROVOICE("ProVoice (Digital)"),
        /** Auto: ANALOG for MT1=0x06 grants, PROVOICE for MT1=0x03 grants */
        AUTO("Auto (Analog/Digital by grant)");

        private final String mLabel;
        VoiceMode(String label) { mLabel = label; }
        @Override public String toString() { return mLabel; }
    }

    public DecodeConfigEDACS()
    {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "lcnFrequencies")
    public String getLcnFrequencies()
    {
        return mLcnFrequencies;
    }

    public void setLcnFrequencies(String frequencies)
    {
        mLcnFrequencies = frequencies != null ? frequencies : "";
    }

    @JacksonXmlProperty(isAttribute = false, localName = "voiceMode")
    public VoiceMode getVoiceMode()
    {
        return mVoiceMode;
    }

    public void setVoiceMode(VoiceMode mode)
    {
        mVoiceMode = mode != null ? mode : VoiceMode.ANALOG;
    }

    /**
     * Gets the frequency in Hz for the given LCN (1-based).
     * @param lcn logical channel number (1-25)
     * @return frequency in Hz or 0 if not set
     */
    public long getFrequency(int lcn)
    {
        if(lcn < 1 || lcn > MAX_LCN || mLcnFrequencies == null || mLcnFrequencies.isEmpty())
        {
            return 0;
        }

        String[] parts = mLcnFrequencies.split(",");
        if(lcn - 1 < parts.length)
        {
            try
            {
                return Long.parseLong(parts[lcn - 1].trim());
            }
            catch(NumberFormatException e)
            {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Gets all LCN frequencies as an array of Hz values (25 elements).
     * Unset LCNs will have 0.
     */
    public long[] getFrequencies()
    {
        long[] freqs = new long[MAX_LCN];
        if(mLcnFrequencies != null && !mLcnFrequencies.isEmpty())
        {
            String[] parts = mLcnFrequencies.split(",");
            for(int i = 0; i < Math.min(parts.length, MAX_LCN); i++)
            {
                try
                {
                    freqs[i] = Long.parseLong(parts[i].trim());
                }
                catch(NumberFormatException e)
                {
                    freqs[i] = 0;
                }
            }
        }
        return freqs;
    }

    /**
     * Resolves the effective voice mode for a single channel grant. In
     * AUTO mode this is selected by the digital flag on the grant
     * (MT1=0x03 -> ProVoice, MT1=0x06 -> analog).
     */
    public VoiceMode resolveVoiceMode(boolean isDigitalGrant)
    {
        if(mVoiceMode == VoiceMode.AUTO)
        {
            return isDigitalGrant ? VoiceMode.PROVOICE : VoiceMode.ANALOG;
        }
        return mVoiceMode;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "traffic_channel_pool_size")
    public int getTrafficChannelPoolSize()
    {
        return mTrafficChannelPoolSize;
    }

    /**
     * Maximum simultaneous traffic channels to allocate. EDACS ProVoice is
     * CPU-heavy, so keep the default conservative for busy sites.
     */
    public void setTrafficChannelPoolSize(int size)
    {
        mTrafficChannelPoolSize = Math.max(0, size);
    }

    /**
     * The 16-bit marker, as 4 hex digits, that every ProVoice voice frame on this system carries.  Frames without it
     * (the signalling at the start and end of each over) are not treated as voice.  Empty to treat every frame as
     * voice.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "provoice_voice_marker")
    public String getProVoiceVoiceMarker()
    {
        return mProVoiceVoiceMarker;
    }

    /**
     * Sets the voice marker.  A value that is not 1-4 hex digits is stored as empty (no check).  A null value, as
     * when loading a playlist saved before this setting existed, leaves the default in place.
     */
    public void setProVoiceVoiceMarker(String marker)
    {
        if(marker == null)
        {
            return;
        }

        String trimmed = marker.trim().toUpperCase();
        mProVoiceVoiceMarker = trimmed.matches("[0-9A-F]{1,4}") ? trimmed : "";
    }

    /**
     * Voice marker as a number, or PROVOICE_VOICE_MARKER_NONE when frames are not to be checked.
     */
    @JsonIgnore
    public int getProVoiceVoiceMarkerValue()
    {
        return mProVoiceVoiceMarker.isEmpty() ? PROVOICE_VOICE_MARKER_NONE : Integer.parseInt(mProVoiceVoiceMarker, 16);
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(50000.0, 12500, 6000.0, 7000.0);
    }
}
