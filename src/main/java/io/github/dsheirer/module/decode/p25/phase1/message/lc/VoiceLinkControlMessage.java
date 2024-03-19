/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

/**
 * Base voice call link control word
 */
public abstract class VoiceLinkControlMessage extends LinkControlWord implements IServiceOptionsProvider
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_2_BIT_16);
    private VoiceServiceOptions mVoiceServiceOptions;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public VoiceLinkControlMessage(CorrectedBinaryMessage message)
    {
        super(message);
    }

    /**
     * Service Options for this channel
     */
    public VoiceServiceOptions getServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getInt(SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }
}
