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

package io.github.dsheirer.source.tuner.configuration;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistable instance of tuner configurations and disabled tuners
 */
@JsonSerialize
public class TunerConfigurationState
{
    private List<DisabledTuner> mDisabledTuners = new ArrayList();
    private List<TunerConfiguration> mTunerConfigurations = new ArrayList<>();

    public TunerConfigurationState()
    {
    }

    /**
     * List of currently disabled tuners
     */
    public List<DisabledTuner> getDisabledTuners()
    {
        return mDisabledTuners;
    }

    /**
     * Sets the list of disabled tuners
     */
    public void setDisabledTuners(List<DisabledTuner> disabledTuners)
    {
        mDisabledTuners = disabledTuners;
    }

    /**
     * Get list of tuner configurations
     */
    public List<TunerConfiguration> getTunerConfigurations()
    {
        return mTunerConfigurations;
    }

    /**
     * Sets the list of tuner configurations
     */
    public void setTunerConfigurations(List<TunerConfiguration> tunerConfigurations)
    {
        mTunerConfigurations = tunerConfigurations;
    }
}
