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
package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.fsk.AFSK1200Decoder;
import io.github.dsheirer.dsp.fsk.AFSK1200DecoderInstrumented;
import io.github.dsheirer.dsp.fsk.SampleBufferInstrumented;
import io.github.dsheirer.dsp.fsk.ZeroCrossingErrorDetectorInstrumented;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import javafx.beans.property.SimpleIntegerProperty;

public class Fleetsync2DecoderInstrumented extends Fleetsync2Decoder
{
    public SimpleIntegerProperty bufferCount = new SimpleIntegerProperty();

    public Fleetsync2DecoderInstrumented(AliasList aliasList)
    {
        super(new AFSK1200DecoderInstrumented(MESSAGE_LENGTH,
            new SampleBufferInstrumented(AFSK1200Decoder.SAMPLES_PER_SYMBOL, AFSK1200Decoder.COARSE_TIMING_GAIN),
            new ZeroCrossingErrorDetectorInstrumented(AFSK1200Decoder.SAMPLES_PER_SYMBOL), false), aliasList);
    }

    public AFSK1200DecoderInstrumented getAFSK1200Decoder()
    {
        return (AFSK1200DecoderInstrumented)mAFSK1200Decoder;
    }

    @Override
    public void receive(ReusableBuffer reusableBuffer)
    {
        super.receive(reusableBuffer);

        bufferCount.setValue(bufferCount.intValue() + 1);
    }
}
