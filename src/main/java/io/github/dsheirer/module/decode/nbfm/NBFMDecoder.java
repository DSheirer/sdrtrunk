/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.dsp.fm.SquelchingFMDemodulator;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.analog.SquelchingAnalogDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Narrow Band FM Decoder module with integrated squelch control.
 */
public class NBFMDecoder extends SquelchingAnalogDecoder
{
    private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder.class);
    private static final float SQUELCH_ALPHA_DECAY = 0.0004f;

    /**
     * Constructs an instance
     *
     * @param config to setup the decoder
     */
    public NBFMDecoder(DecodeConfigNBFM config)
    {
        super(config, new SquelchingFMDemodulator(SQUELCH_ALPHA_DECAY, config.getSquelchThreshold(),
                config.isSquelchAutoTrack()));
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }
}
