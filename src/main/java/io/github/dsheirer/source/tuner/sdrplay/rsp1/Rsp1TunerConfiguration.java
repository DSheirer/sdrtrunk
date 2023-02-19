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

package io.github.dsheirer.source.tuner.sdrplay.rsp1;

import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;

/**
 * RSP1 tuner configuration
 */
public class Rsp1TunerConfiguration extends RspTunerConfiguration
{
    /**
     * Constructs an instance
      * @param uniqueId for the tuner
     */
    public Rsp1TunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public Rsp1TunerConfiguration()
    {
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_1;
    }
}
