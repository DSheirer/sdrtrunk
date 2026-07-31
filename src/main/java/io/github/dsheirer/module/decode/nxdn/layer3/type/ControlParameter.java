/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Remote control command parameters
 * @param value of the field
 */
public record ControlParameter(int value)
{
    private static final int FLAG_STUN_INHIBIT = 0x8000;
    private static final int FLAG_REMOTE_MONITOR_MODE = 0x8000;
    private static final int MASK_TRANSMIT_DURATION = 0x00FF;

    /**
     * Indicates if stun command is inhibit both TX/RX (true) or just TX (false).
     */
    public boolean isStunInhibitBothTxAndRx()
    {
        return (value & FLAG_STUN_INHIBIT) == FLAG_STUN_INHIBIT;
    }

    /**
     * Indicates if remote monitor is silence mode (true) or normal mode (false).
     */
    public boolean isRemoteMonitorSilenceMode()
    {
        return (value & FLAG_REMOTE_MONITOR_MODE) == FLAG_REMOTE_MONITOR_MODE;
    }

    /**
     * Remote monitor transmit duration
     * @return duration seconds.
     */
    public int getRemoteMonitorDurationSeconds()
    {
        return value & MASK_TRANSMIT_DURATION;
    }

    /**
     * Interprets the command and appends any optional parameter values.
     * @param command to interpret
     * @return fully interpreted command.
     */
    public String interpret(ControlCommand command)
    {
        return switch(command)
        {
            case STUN -> "STUN INHIBIT " + (isStunInhibitBothTxAndRx() ? "TX & RX" : "TX ONLY");
            case REVIVAL -> "REVIVE";
            case KILL -> "KILL";
            case REMOTE_MONITOR -> "REMOTE MONITOR " + (isRemoteMonitorSilenceMode() ? "SILENT" : "NORMAL") +
                    " DURATION:" + getRemoteMonitorDurationSeconds() + " SECONDS";
            default -> "RESERVED";
        };
    }
}
