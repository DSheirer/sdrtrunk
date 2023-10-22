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
package io.github.dsheirer.source.tuner.rtl.r8x.r820t;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;

/**
 * RTL-2832 with embedded R820T tuner configuration
 */
public class R820TTunerConfiguration extends R8xTunerConfiguration
{
    /**
     * Empty constructor for Jackson serialization
     */
    public R820TTunerConfiguration()
    {
    }

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public R820TTunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.RAFAELMICRO_R820T;
    }
}
