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

package io.github.dsheirer.module.decode.moto;

import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoderState;
import io.github.dsheirer.protocol.Protocol;

/**
 * Decoder state for a Motorola Type II analog traffic channel.
 *
 * An analog voice channel carries no talkgroup or radio identifiers, so the identifiers from the control channel grant
 * are the only ones the channel has.  They are loaded when the traffic channel starts and apply until it is torn
 * down.  The analog decoder state removes the user identifiers each time the squelch closes, which would blank the
 * TO and FROM for the rest of the call, so they are kept here instead.
 */
public class MotorolaTypeIITrafficDecoderState extends NBFMDecoderState
{
    public MotorolaTypeIITrafficDecoderState(String channelName, DecodeConfigNBFM decodeConfig)
    {
        super(channelName, decodeConfig, false, Protocol.MOTOROLA_TYPE_II);
    }

    /**
     * Keeps the user identifiers from the grant.  A new grant gets a new traffic channel and decoder state.
     */
    @Override
    protected void resetState()
    {
    }
}
