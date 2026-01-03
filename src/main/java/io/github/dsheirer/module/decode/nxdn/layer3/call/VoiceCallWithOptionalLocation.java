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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.VoiceCallOption;

/**
 * Voice call with optional location ID field for fully qualified source/destination.
 */
public abstract class VoiceCallWithOptionalLocation extends CallWithOptionalLocation
{
    private VoiceCallOption mVoiceCallOption;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public VoiceCallWithOptionalLocation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran,
                                         LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Voice call options for the call.
     * @return options
     */
    public VoiceCallOption getCallOption()
    {
        if(mVoiceCallOption == null)
        {
            mVoiceCallOption = new VoiceCallOption(getMessage().getInt(CALL_OPTION));
        }

        return mVoiceCallOption;
    }
}
