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

// Reference: mbelib imbe7100x4400.c, sdrtrunk P25/DMR MBE call sequence recorders.
package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.audio.codec.mbe.MBECallSequence;
import io.github.dsheirer.audio.codec.mbe.MBECallSequenceRecorder;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;
import io.github.dsheirer.preference.UserPreferences;

/**
 * Records EDACS ProVoice packed 7x24 IMBE7100 grids as MBE call sequence artifacts.
 */
public class EDACSProVoiceCallSequenceRecorder extends MBECallSequenceRecorder
{
    public static final String PROTOCOL = "EDACS-PROVOICE-IMBE7100";
    private MBECallSequence mCallSequence;

    public EDACSProVoiceCallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
    {
        super(userPreferences, channelFrequency, system, site);
    }

    @Override
    public void stop()
    {
        flush();
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof EDACSProVoiceMessage proVoiceMessage && proVoiceMessage.isValid())
        {
            process(proVoiceMessage);
        }
    }

    private void process(EDACSProVoiceMessage message)
    {
        if(mCallSequence == null)
        {
            mCallSequence = new MBECallSequence(PROTOCOL);
            mCallSequence.setCallType(CALL_TYPE_GROUP);
            applyIdentifiers(message);
        }

        long timestamp = message.getTimestamp();
        for(byte[] frame : message.getPackedImbe7100Frames())
        {
            mCallSequence.addVoiceFrame(timestamp, toHex(frame));
            timestamp += 20;
        }
    }

    private void applyIdentifiers(EDACSProVoiceMessage message)
    {
        for(Identifier identifier : message.getIdentifiers())
        {
            if(identifier.getRole() == Role.FROM)
            {
                mCallSequence.setFromIdentifier(identifier);
            }
            else if(identifier.getRole() == Role.TO)
            {
                mCallSequence.setToIdentifier(identifier);
            }
        }
    }

    private void flush()
    {
        if(mCallSequence != null)
        {
            writeCallSequence(mCallSequence, "PROVOICE");
            mCallSequence = null;
        }
    }

    private static String toHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(byte b : bytes)
        {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
