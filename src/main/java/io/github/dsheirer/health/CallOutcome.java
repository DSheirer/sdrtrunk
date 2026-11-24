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

package io.github.dsheirer.health;

/**
 * What became of a call a control channel announced: recorded (a traffic channel was assigned and followed it) or
 * why not.  The names are the node health report's (calls.notRecorded).
 */
public enum CallOutcome
{
    RECORDED("recorded"),
    /** The user chose not to record it. */
    IGNORED("ignored"),
    ENCRYPTED("encrypted"),
    /** A data call, not followed. */
    DATA("data"),
    /** The grant's channel couldn't be turned into a frequency: a channel map missing from the settings. */
    NO_FREQUENCY("noFrequency"),
    /** No tuner covers the frequency. */
    NO_SOURCE("noSource"),
    /** Every traffic channel was busy. */
    NO_RECORDER("noRecorder");

    private final String mReportName;

    CallOutcome(String reportName)
    {
        mReportName = reportName;
    }

    /**
     * Name used in a node health report.
     */
    public String getReportName()
    {
        return mReportName;
    }
}
