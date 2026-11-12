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

package io.github.dsheirer.source.tuner.signalhound;

/**
 * Signal Hound device details for display.
 * @param model such as BB60D, SM200B or SP145
 * @param serial number
 * @param firmware version
 * @param library name of the Signal Hound API library, such as bb_api
 * @param libraryVersion of the Signal Hound API library
 * @param libraryLocation where the Signal Hound API library was loaded from
 */
public record SignalHoundDeviceInformation(String model, int serial, String firmware, String library,
                                           String libraryVersion, String libraryLocation)
{
}
