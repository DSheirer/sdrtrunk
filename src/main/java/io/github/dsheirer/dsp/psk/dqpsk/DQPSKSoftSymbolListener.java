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

package io.github.dsheirer.dsp.psk.dqpsk;

/**
 * Interface for a soft symbol listener
 */
public interface DQPSKSoftSymbolListener
{
    /**
     * Receive demodulated soft symbol stream
     * @param samples to process
     */
    void receive(float[] samples);

    /**
     * Receive an updated stream timestamp from the incoming sample buffers.
     * @param timestamp to set for reference
     */
    void setTimestamp(long timestamp);

    /**
     * Sets the samples per symbol rate for the incoming sample stream.
     * @param samplesPerSymbol to set.
     */
    void setSamplesPerSymbol(float samplesPerSymbol);
}
