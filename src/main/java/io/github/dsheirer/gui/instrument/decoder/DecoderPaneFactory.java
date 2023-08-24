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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Decoder;
import java.util.EnumSet;

public class DecoderPaneFactory
{
    private static final EnumSet<DecoderType> SUPPORTED_DECODER_TYPES = EnumSet.of(
        DecoderType.P25_PHASE1,
        DecoderType.P25_PHASE2,
        DecoderType.FLEETSYNC2,
        DecoderType.LJ_1200,
        DecoderType.LTR_NET,
        DecoderType.MDC1200,
        DecoderType.MPT1327,
        DecoderType.TAIT_1200);

    /**
     * Creates a decoder pane for the decoder type
     */
    public static AbstractDecoderPane getDecoderPane(DecoderType decoderType)
    {
        switch(decoderType)
        {
            case FLEETSYNC2:
                return new Fleetsync2Pane();
            case LJ_1200:
                return new LJ1200Pane();
            case LTR_NET:
                return new LTRNetPane();
            case MDC1200:
                return new MDC1200Pane();
            case MPT1327:
                return new MPT1327Pane();
            case TAIT_1200:
                return new Tait1200Pane();
            case P25_PHASE1:
                throw new IllegalArgumentException("Use the getP25P1DecoderPane() method for P25 decoder type");
            case P25_PHASE2:
                return new P25Phase2HDQPSKPane();
        }

        return getDefaultPane();
    }

    /**
     * Creates a decoder pane for the P25 decoder type
     */
    public static AbstractDecoderPane getP25P1DecoderPane(P25P1Decoder.Modulation modulation)
    {
        switch(modulation)
        {
            case C4FM:
                return new P25Phase1C4FMPane();
            case CQPSK:
                return new P25Phase1LSMPane();
        }

        return getDefaultPane();
    }

    /**
     * Creates an empty decoder pane
     */
    public static AbstractDecoderPane getDefaultPane()
    {
        return new ComplexDecoderPane();
    }

    /**
     * Indicates if the decoder type is supported by this factory
     */
    public static boolean isSupported(DecoderType decoderType)
    {
        return SUPPORTED_DECODER_TYPES.contains(decoderType);
    }
}
