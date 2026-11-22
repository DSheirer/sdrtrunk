/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.rsp1b;

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.ControlRsp1a;

/**
 * Control wrapper for an RSP1B Device.  The SDRplay API controls the RSP1B the same as the RSP1A.
 */
public class ControlRsp1b extends ControlRsp1a implements IControlRsp1b
{
    /**
     * Constructs an instance
     * @param deviceInfo for the device
     */
    public ControlRsp1b(DeviceInfo deviceInfo)
    {
        super(deviceInfo);
    }

    @Override
    protected long getLowBandLimit()
    {
        return 50_000_000;
    }
}
