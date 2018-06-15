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
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import javafx.scene.text.Text;

public class ComplexDecoderPane extends AbstractDecoderPane<ReusableComplexBuffer>
{
    private ReusableBufferBroadcaster mComplexBufferBroadcaster = new ReusableBufferBroadcaster();
    private DecoderType mDecoderType;

    public ComplexDecoderPane(DecoderType decoderType)
    {
        mDecoderType = decoderType;
    }

    /**
     * Package private - used by factory class to create an empty pane
     */
    ComplexDecoderPane()
    {
        getChildren().add(new Text("Please select a decoder from the menu"));
    }

    @Override
    SampleType getSampleType()
    {
        return SampleType.COMPLEX;
    }

    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        mComplexBufferBroadcaster.broadcast(reusableComplexBuffer);
    }

    public void addListener(Listener<ReusableComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mComplexBufferBroadcaster.removeListener(listener);
    }
}
