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

package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.SimpleStringIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.analog.AnalogDecoderState;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSMessage;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSMessage;
import io.github.dsheirer.module.decode.squelchDecoder.squelchDecoderConfig;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NBFM decoder state - tracks channel tone filtering configuration and detected tones
 */
public class NBFMDecoderState extends AnalogDecoderState
{
    private String mChannelName;
    private Identifier mChannelNameIdentifier;
    private Identifier mTalkgroupIdentifier;

    // Tone filter configuration (from DecodeConfigNBFM)
    private boolean mSquelchDecoderEnabled = false;
    private List<squelchDecoderConfig> mConfiguredSquelchDecoders = new ArrayList<>();

    // Current status
    private volatile String mToneStatus = "No tone detected";

    // Per-tone detection counts: key = display string, value = [acceptCount, rejectCount]
    private final Map<String, int[]> mToneCounts = new LinkedHashMap<>();

    /**
     * Constructs an instance
     * @param channelName to use for this channel
     * @param decodeConfig with talkgroup identifier and tone filter settings
     */
    public NBFMDecoderState(String channelName, DecodeConfigNBFM decodeConfig)
    {
        mChannelName = (channelName != null && !channelName.isEmpty()) ? channelName : "NBFM CHANNEL";
        mChannelNameIdentifier = new SimpleStringIdentifier(mChannelName, IdentifierClass.CONFIGURATION, Form.CHANNEL_NAME, Role.ANY);
        mTalkgroupIdentifier = new NBFMTalkgroup(decodeConfig.getTalkgroup());

        mSquelchDecoderEnabled = decodeConfig.isSquelchDecoderEnabled();
        if(mSquelchDecoderEnabled && decodeConfig.getSquelchDecoders() != null)
        {
            mConfiguredSquelchDecoders.addAll(decodeConfig.getSquelchDecoders());
        }
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    @Override
    protected Identifier getChannelNameIdentifier()
    {
        return mChannelNameIdentifier;
    }

    @Override
    protected Identifier getTalkgroupIdentifier()
    {
        return mTalkgroupIdentifier;
    }

    /**
     * Called by IMessage receive when a matching CTCSS tone is detected (allowed)
     */
    public void setAcceptedCTCSS(CTCSSCode code)
    {
        if(code != null)
        {
            mToneStatus = "CTCSS: " + code.getDisplayString() + " [ALLOWED]";
            incrementCount(code.getDisplayString(), true);
        }
    }

    /**
     * Called by IMessage receive when a matching DCS code is detected (allowed)
     */
    public void setAcceptedDCS(DCSCode code)
    {
        if(code != null)
        {
            mToneStatus = "DCS: " + code.toString() + " [ALLOWED]";
            incrementCount("DCS " + code.toString(), true);
        }
    }

    /**
     * Called by IMessage receive when a non-matching CTCSS tone is detected (rejected)
     */
    public void setRejectedCTCSS(CTCSSCode code)
    {
        if(code != null)
        {
            mToneStatus = "CTCSS: " + code.getDisplayString() + " [REJECTED]";
            incrementCount(code.getDisplayString(), false);
        }
    }

    /**
     * Called by IMessage receive when a non-matching DCS code is detected (rejected)
     */
    public void setRejectedDCS(DCSCode code)
    {
        if(code != null)
        {
            mToneStatus = "DCS: " + code.toString() + " [REJECTED]";
            incrementCount("DCS " + code.toString(), false);
        }
    }

    /**
     * Called by IMessage receive when tone is lost
     */
    public void setToneLost()
    {
        mToneStatus = "No tone detected";
        getIdentifierCollection().remove(Form.TONE);
    }

    private void incrementCount(String toneLabel, boolean accepted)
    {
        synchronized(mToneCounts)
        {
            int[] counts = mToneCounts.computeIfAbsent(toneLabel, k -> new int[]{0, 0});
            if(accepted)
            {
                counts[0]++;
            }
            else
            {
                counts[1]++;
            }
        }
    }

    /**
     *  Provides information for UI display in Now Playing panel.
     */
    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Activity Summary - Decoder:NBFM\n");

         sb.append("\n\nSquelch Decoder: ");
        if(mSquelchDecoderEnabled)
        {
            sb.append("ENABLED\n");
            sb.append("Configured decoder: ");
            if(!mConfiguredSquelchDecoders.isEmpty())
            {
                sb.append(mConfiguredSquelchDecoders.get(0).getDisplayString());
            }
            sb.append("\n");
        }
        else
        {
            sb.append("Disabled\n");
        }

        synchronized(mToneCounts)
        {
            if(!mToneCounts.isEmpty())
            {
                sb.append("\nDetected codes:\n");
                for(Map.Entry<String, int[]> entry : mToneCounts.entrySet())
                {
                    String status;
                    int accepted = entry.getValue()[0];
                    int rejected = entry.getValue()[1];
                    if(accepted > 0)
                    {
                        status = "ALLOWED (" + accepted + ")";
                    }
                    else
                    {
                        status = "REJECTED (" + rejected + ")";
                    }
                    sb.append("\t").append(entry.getKey()).append(" - ").append(status).append("\n");
                }
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Listener for IMessages originating in NBFM decoder. If the squelch decoders are enabled,
     * the message contains information on decoded squelch code (CTCSS/DCS) which is added to the
     * identifier collection. A tally is collected for codes accepted and rejected for use
     * in getActivitySummary.
     *
     */
    @Override
    public void receive(IMessage message)
    {
        if(message instanceof CTCSSMessage ctcssMessage)
        {
            getIdentifierCollection().update(ctcssMessage.getIdentifiers());
            if (ctcssMessage.getCodeState() != null)
            {
                switch (ctcssMessage.getCodeState())
                {
                    case ACCEPTED -> setAcceptedCTCSS(ctcssMessage.getCTCSSCode());
                    case REJECTED -> setRejectedCTCSS(ctcssMessage.getCTCSSCode());
                    case LOST -> setToneLost();
                }
            }
        }
        if(message instanceof DCSMessage dcsMessage)
        {
            getIdentifierCollection().update(dcsMessage.getIdentifiers());
            if (dcsMessage.getCodeState() != null)
            {
                switch (dcsMessage.getCodeState())
                {
                    case ACCEPTED -> setAcceptedDCS(dcsMessage.getDCSCode());
                    case REJECTED -> setRejectedDCS(dcsMessage.getDCSCode());
                    case LOST -> setToneLost();
                }
            }
        }
    }
}
