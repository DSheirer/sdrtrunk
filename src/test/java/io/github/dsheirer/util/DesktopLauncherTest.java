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

package io.github.dsheirer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DesktopLauncherTest
{
    @Test
    void openerForEachOperatingSystem()
    {
        assertEquals("xdg-open", DesktopLauncher.getOpener("Linux"));
        assertEquals("xdg-open", DesktopLauncher.getOpener("FreeBSD"));
        assertEquals("open", DesktopLauncher.getOpener("Mac OS X"));
        assertNull(DesktopLauncher.getOpener("Windows 11"));
        assertEquals("xdg-open", DesktopLauncher.getOpener(null));
    }
}
