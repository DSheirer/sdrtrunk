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
package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.fsk.LTRDecoderInstrumented;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import javafx.beans.property.SimpleIntegerProperty;

public class LTRNetDecoderInstrumented extends LTRNetDecoder
{
    public SimpleIntegerProperty bufferCount = new SimpleIntegerProperty();

    public LTRNetDecoderInstrumented(DecodeConfigLTRNet config, AliasList aliasList)
    {
        super(config, aliasList, new LTRDecoderInstrumented(LTR_NET_MESSAGE_LENGTH));
    }

    public LTRDecoderInstrumented getLTRDecoder()
    {
        return (LTRDecoderInstrumented)mLTRDecoder;
    }

    @Override
    public void receive(ReusableBuffer reusableBuffer)
    {
        super.receive(reusableBuffer);

        bufferCount.setValue(bufferCount.intValue() + 1);
    }
}
