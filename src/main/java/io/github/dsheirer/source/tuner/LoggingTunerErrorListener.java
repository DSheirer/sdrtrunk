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

package io.github.dsheirer.source.tuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner error listener that logs any received tuner error messages
 */
public class LoggingTunerErrorListener implements ITunerErrorListener
{
    private Logger mLog = LoggerFactory.getLogger(LoggingTunerErrorListener.class);

    @Override
    public void setErrorMessage(String errorMessage)
    {
        mLog.error("Tuner Error: " + errorMessage);
    }

    @Override
    public void tunerRemoved()
    {
        mLog.warn("Tuner removal detected");
    }
}
