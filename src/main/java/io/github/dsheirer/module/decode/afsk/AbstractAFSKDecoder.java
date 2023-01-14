/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.afsk;

import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;

/**
 * Abstract class for Audio Frequency Shift Keying (AFSK) 1200-baud decoder based decoder modules.
 *
 * This class handles the AFSK 1200 decoder and incoming sample stream buffer management.
 *
 * Sub-class implementations should invoke getDecoder().setListener() to receive decoded symbol stream.
 */
public abstract class AbstractAFSKDecoder extends Decoder implements IRealBufferListener, Listener<float[]>
{
    private AFSK1200Decoder mAFSK1200Decoder;

    public AbstractAFSKDecoder(AFSK1200Decoder decoder)
    {
        mAFSK1200Decoder = decoder;
    }

    public AbstractAFSKDecoder(AFSK1200Decoder.Output output)
    {
        mAFSK1200Decoder = new AFSK1200Decoder(output);
    }

    protected AFSK1200Decoder getDecoder()
    {
        return mAFSK1200Decoder;
    }

    @Override
    public void receive(float[] realBuffer)
    {
        mAFSK1200Decoder.receive(realBuffer);
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return this;
    }
}
