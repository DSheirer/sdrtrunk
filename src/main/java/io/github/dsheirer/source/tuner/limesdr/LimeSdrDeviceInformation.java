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

package io.github.dsheirer.source.tuner.limesdr;

/**
 * LimeSDR device details for display.
 * @param serial board serial number, 16 hex digits
 * @param board name, such as LimeSDR-USB or LimeSDR-Mini
 * @param firmware version
 * @param hardware version
 * @param gateware (FPGA) version
 * @param connection USB link, such as USB3.0
 * @param library version of LimeSuiteNG
 */
public record LimeSdrDeviceInformation(String serial, String board, String firmware, String hardware, String gateware,
                                       String connection, String library)
{
}
