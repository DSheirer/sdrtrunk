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

package io.github.dsheirer.module.decode.dmr.sync.visualizer;

import java.util.concurrent.CountDownLatch;

/**
 * Interface for receiving sync detection results from a demodulated I/Q stream
 */
public interface ISyncResultsListener
{
    /**
     * Receive results
     * @param symbols for the detected sync
     * @param sync pattern symbols (ideal)
     * @param samples demodulated samples
     * @param syncIntervals for timing of each symbol in the samples array
     * @param label to display in the UI
     * @param release that pauses execution until the user acknowledges the displayed sync results.  Decrement the latch to release.
     */
    void receive(float[] symbols, float[] sync, float[] samples, float[] syncIntervals, String label, CountDownLatch release);
}
