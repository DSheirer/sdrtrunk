/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer.decoder;

import io.github.dsheirer.module.decode.DecoderType;

import java.util.EnumSet;

public class DecoderPaneFactory
{
    private static final EnumSet<DecoderType> SUPPORTED_DECODER_TYPES = EnumSet.of(DecoderType.P25_PHASE1);

    /**
     * Creates a decoder pane for the decoder type
     */
    public static DecoderPane getDecoderPane(DecoderType decoderType)
    {
        switch(decoderType)
        {
            case P25_PHASE1:
                return new P25Phase1Pane();
        }

        return getDefaultPane();
    }

    /**
     * Creates an empty decoder pane
     */
    public static DecoderPane getDefaultPane()
    {
        return new DecoderPane();
    }

    /**
     * Indicates if the decoder type is supported by this factory
     */
    public static boolean isSupported(DecoderType decoderType)
    {
        return SUPPORTED_DECODER_TYPES.contains(decoderType);
    }
}
