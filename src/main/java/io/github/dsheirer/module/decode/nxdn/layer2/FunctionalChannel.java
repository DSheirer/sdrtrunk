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

package io.github.dsheirer.module.decode.nxdn.layer2;

/**
 * Functional channel enumeration
 */
public enum FunctionalChannel
{
    CAC,
    CAC_LONG,
    CAC_SHORT,
    SACCH_NON_SUPER_FRAME,
    SACCH_SUPER_FRAME,
    SACCH_SUPER_FRAME_IDLE,
    UDCH,
    UNKNOWN;
}
