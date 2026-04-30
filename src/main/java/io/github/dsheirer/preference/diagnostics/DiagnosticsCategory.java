/*
 * *****************************************************************************
 * sdrtrunk
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

package io.github.dsheirer.preference.diagnostics;

/**
 * Named groups of Java loggers that the user can toggle to DEBUG at runtime from the
 * Diagnostics preferences panel (ap-14.6).
 */
public enum DiagnosticsCategory
{
    ZELLO("Zello streaming", "io.github.dsheirer.audio.broadcast.zello"),
    THINLINE("ThinLine Radio streaming", "io.github.dsheirer.audio.broadcast.thinlineradio"),
    RDIO("Rdio Scanner streaming", "io.github.dsheirer.audio.broadcast.rdioscanner"),
    SDRPLAY("SDRPlay / RSP tuners", "io.github.dsheirer.source.tuner.sdrplay"),
    RTLSDR("Nooelec / RTL-SDR tuners", "io.github.dsheirer.source.tuner.rtl"),
    CHANNELIZER("Channelizer / DDC", "io.github.dsheirer.dsp.filter.channelizer"),
    TUNER_POOL("Tuner manager / pool", "io.github.dsheirer.source.tuner.manager"),
    P25("P25 decoder", "io.github.dsheirer.module.decode.p25"),
    NBFM_AUDIO("NBFM / audio output", "io.github.dsheirer.audio"),
    CTCSS_DCS("CTCSS / DCS squelch codes", "io.github.dsheirer.module.decode.nbfm");

    private final String mDisplayName;
    private final String mLoggerName;

    DiagnosticsCategory(String displayName, String loggerName)
    {
        mDisplayName = displayName;
        mLoggerName = loggerName;
    }

    public String getDisplayName()
    {
        return mDisplayName;
    }

    /**
     * Root logger name to toggle. When a category is enabled, this logger and every logger
     * whose name starts with it are switched to DEBUG.
     */
    public String getLoggerName()
    {
        return mLoggerName;
    }
}
