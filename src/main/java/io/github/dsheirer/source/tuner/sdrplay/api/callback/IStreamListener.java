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

package io.github.dsheirer.source.tuner.sdrplay.api.callback;

import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;

/**
 * Stream Listener interface.
 */
public interface IStreamListener
{
    /**
     * Process samples from a single stream of I/Q samples
     * @param xi array of Inphase samples
     * @param xq array of Quadrature samples
     * @param streamCallbackParameters stream callback parameters
     * @param reset indicates if a re-initialization has occurred within the API and that local buffering should be reset
     */
    void processStream(short[] xi, short[] xq, StreamCallbackParameters streamCallbackParameters,
                       boolean reset);

    /**
     * Indicates which tuner this stream listener is for.  Note: this is used to manage asynchronous Updates to ensure
     * that submitted updates get correctly mapped and resolved in the Device update manager.
     * @return
     */
    TunerSelect getTunerSelect();
}
