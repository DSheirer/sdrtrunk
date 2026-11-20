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

package io.github.dsheirer.source.tuner.ui;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests parsing the simple HTML tuner information that tuner editors provide.
 */
public class TunerInfoDialogTest
{
    @Test
    public void parsesHeadingAndKeyValueLines()
    {
        TunerInfoDialog.Info info = TunerInfoDialog.parse("<html><h3>RTL-2832 with R820T Tuner</h3>" +
                "<b>USB ID: </b>0bda:2838<br><b>Serial: </b>00000001<br>No EEPROM Descriptor Available");

        assertEquals("RTL-2832 with R820T Tuner", info.heading());
        assertEquals(List.of(new TunerInfoDialog.Line("USB ID", "0bda:2838"),
                new TunerInfoDialog.Line("Serial", "00000001"),
                new TunerInfoDialog.Line(null, "No EEPROM Descriptor Available")), info.lines());
        assertEquals("RTL-2832 with R820T Tuner\nUSB ID: 0bda:2838\nSerial: 00000001\nNo EEPROM Descriptor Available\n",
                info.toText());
    }

    @Test
    public void handlesMissingHeading()
    {
        TunerInfoDialog.Info info = TunerInfoDialog.parse("<b>Host: </b>localhost<br>");
        assertEquals(null, info.heading());
        assertEquals(List.of(new TunerInfoDialog.Line("Host", "localhost")), info.lines());
    }
}
