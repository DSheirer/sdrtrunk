/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.configuration;

import io.github.dsheirer.gui.editor.Editor;

public abstract class TunerConfigurationEditor extends Editor<TunerConfiguration>
{
    private static final long serialVersionUID = 1L;

    private TunerConfigurationModel mTunerConfigurationModel;

    public TunerConfigurationEditor(TunerConfigurationModel model)
    {
        mTunerConfigurationModel = model;
    }

    public TunerConfigurationModel getTunerConfigurationModel()
    {
        return mTunerConfigurationModel;
    }

    /**
     * Sets the lock state for the tuner so that the frequency and sample rate controls can be
     * enabled/disabled.
     *
     * @param locked true if the tuner is locked.
     */
    public abstract void setTunerLockState(boolean locked);
}
