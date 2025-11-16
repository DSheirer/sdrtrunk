/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;

/**
 * Decoder that provides feedback to signal source(s) via SourceEvent broadcasts and supports registering source
 * event listeners.
 */
public abstract class FeedbackDecoder extends PrimaryDecoder implements ISourceEventProvider
{
    private Listener<SourceEvent> mSourceEventListener;
    private Listener<Float> mSymbolListener;

    /**
     * Protocol description suitable for display in the user interface
     * @return description
     */
    public abstract String getProtocolDescription();

    /**
     * Registers the listener to receive source events from this decoder
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Deregisters the listener from receiving source events from this decoder.
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    /**
     * Broadcasts the specified source event to the optional registered source event listener
     */
    public void broadcast(SourceEvent sourceEvent)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Processes the current phase-locked loop (PLL) measurement and sends to the parent tuner for self correction.
     * @param pllError as measured in the decoder.
     */
    public void processPLLError(float pllError, int baudRate)
    {
        long frequencyError = (long)(baudRate / 2.0 * (pllError / Math.PI));
        broadcast(SourceEvent.frequencyErrorMeasurementSyncLocked(frequencyError, "Decoder measured error sync locked"));
    }

    /**
     * Registers the listener to receive demodulated symbols.
     * @param listener to receive symbols.
     */
    public void setSymbolListener(Listener<Float> listener)
    {
        mSymbolListener = listener;
    }

    /**
     * Removes any registered symbol listener
     */
    public void removeSymbolListener()
    {
        mSymbolListener = null;
    }

    /**
     * Broadcasts the demodulated symbol to a symbol listener.
     * @param symbol in range 0 to +/- PI radians
     */
    public void broadcast(float symbol)
    {
        if(mSymbolListener != null)
        {
            mSymbolListener.receive(symbol);
        }
    }
}
