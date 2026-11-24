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

package io.github.dsheirer.gui.settings;

/**
 * View calibration setting and execute calibration request
 */
public class CalibrateRequest extends ViewSettingsEditorRequest
{
    private final Runnable mOnComplete;

    /**
     * Constructs an instance
     */
    public CalibrateRequest()
    {
        this(null);
    }

    /**
     * Constructs an instance
     * @param onComplete runs on the JavaFX thread once calibration finishes, or immediately when calibration doesn't
     * start because there is nothing to calibrate, channels are processing, or a calibration is already running.
     * May be null.
     */
    public CalibrateRequest(Runnable onComplete)
    {
        super(SettingsPageType.PERFORMANCE);
        mOnComplete = onComplete;
    }

    /**
     * Action to run once calibration finishes, or null.
     */
    public Runnable getOnComplete()
    {
        return mOnComplete;
    }
}
