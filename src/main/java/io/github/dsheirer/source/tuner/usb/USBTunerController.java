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
package io.github.dsheirer.source.tuner.usb;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.TunerController;

public abstract class USBTunerController extends TunerController
{
    /**
     * USB tuner controller class. Provides auto-start and auto-stop function when complex buffer listeners are added
     * or removed from this tuner controller.
     *
     * @param minimumFrequency minimum uncorrected tunable frequency
     * @param maximumFrequency maximum uncorrected tunable frequency
     * @param middleUnusable is the +/- value center DC spike to avoid for channels
     * @param usableBandwidth percentage of usable bandwidth relative to space at the extreme ends of the spectrum
     */
    public USBTunerController(long minimumFrequency, long maximumFrequency, int middleUnusable, double usableBandwidth)
    {
        super(minimumFrequency, maximumFrequency, middleUnusable, usableBandwidth);
    }

    protected abstract USBTransferProcessor getUSBTransferProcessor();

    /**
     * Adds the IQ buffer listener and automatically starts buffer transfer processing, if not already started.
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        boolean hasExistingListeners = hasBufferListeners();

        super.addBufferListener(listener);

        if(!hasExistingListeners)
        {
            getUSBTransferProcessor().setListener(this);
        }
    }

    /**
     * Removes the IQ buffer listener and stops buffer transfer processing if there are no more listeners.
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        super.removeBufferListener(listener);

        if(!hasBufferListeners())
        {
            getUSBTransferProcessor().removeListener();
        }
    }
}
