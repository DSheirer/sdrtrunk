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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import javafx.scene.text.Text;

public class RealDecoderPane extends AbstractDecoderPane<ReusableBuffer>
{
    private ReusableBufferBroadcaster mBufferBroadcaster = new ReusableBufferBroadcaster();
    private DecoderType mDecoderType;

    public RealDecoderPane(DecoderType decoderType)
    {
        mDecoderType = decoderType;
    }

    /**
     * Package private - used by factory class to create an empty pane
     */
    RealDecoderPane()
    {
        getChildren().add(new Text("Please select a decoder from the menu"));
    }

    @Override
    SampleType getSampleType()
    {
        return SampleType.COMPLEX;
    }

    @Override
    public void receive(ReusableBuffer reusableBuffer)
    {
        mBufferBroadcaster.broadcast(reusableBuffer);
    }

    public void addListener(Listener<ReusableBuffer> listener)
    {
        mBufferBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<ReusableBuffer> listener)
    {
        mBufferBroadcaster.removeListener(listener);
    }

    public void setSampleRate(double sampleRate)
    {
    }
}
