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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.EventType;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.GainCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.PowerOverloadCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.event.RspDuoModeCallbackParameters;

/**
 * Device event listener.  Receives events for the device, impacting one or both (if equipped) tuners on the device.
 */
public interface IDeviceEventListener
{
    /**
     * Process a device event
     * @param eventType identifies the type of event
     * @param tunerSelect identifies which tuner(s) are included in the event (A, B, BOTH, or NEITHER)
     */
    void processEvent(EventType eventType, TunerSelect tunerSelect);

    /**
     * Process a gain change event
     * @param tunerSelect identifies which tuner(s) are included in the event (A, B, BOTH, or NEITHER)
     * @param gainCallbackParameters containing event details
     */
    void processGainChange(TunerSelect tunerSelect, GainCallbackParameters gainCallbackParameters);

    /**
     * Process a power overload event
     * @param tunerSelect identifies which tuner(s) are included in the event (A, B, BOTH, or NEITHER)
     * @param parameters containing event details
     */
    void processPowerOverload(TunerSelect tunerSelect, PowerOverloadCallbackParameters parameters);

    /**
     * Process an RSP-Duo mode change event
     * @param tunerSelect identifies which tuner(s) are included in the event (A, B, BOTH, or NEITHER)
     * @param parameters containing event details
     */
    void processRspDuoModeChange(TunerSelect tunerSelect, RspDuoModeCallbackParameters parameters);

    /**
     * Process a device removed (ie unplugged) event
     * @param tunerSelect identifies which tuner(s) are included in the event (A, B, BOTH, or NEITHER)
     */
    void processDeviceRemoval(TunerSelect tunerSelect);

}
