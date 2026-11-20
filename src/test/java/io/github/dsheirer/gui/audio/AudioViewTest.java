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

package io.github.dsheirer.gui.audio;

import javafx.scene.paint.Color;
import javax.sound.sampled.FloatControl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audio display volume mapping and alias accent color.
 */
public class AudioViewTest
{
    private static final FloatControl GAIN = new FloatControl(FloatControl.Type.MASTER_GAIN, -80.0f, 6.0f, 0.1f,
        0, 0.0f, "dB") {};

    @Test
    public void volumeMidpointIsUnityGain()
    {
        assertEquals(0.0f, AudioView.toGain(GAIN, 50), 0.0f);
        assertEquals(-80.0f, AudioView.toGain(GAIN, 0), 1e-4f);
        assertEquals(6.0f, AudioView.toGain(GAIN, 100), 1e-4f);
        assertEquals(-40.0f, AudioView.toGain(GAIN, 25), 1e-4f);
        assertEquals(3.0f, AudioView.toGain(GAIN, 75), 1e-4f);
    }

    @Test
    public void darkAliasColorIsLightenedForTheDisplay()
    {
        Color black = AudioChannelView.toReadable(0x000000);
        Color navy = AudioChannelView.toReadable(0x1E3A8A);
        assertTrue(contrast(black) >= 3.0, "black accent contrast " + contrast(black));
        assertTrue(contrast(navy) >= 3.0, "navy accent contrast " + contrast(navy));
    }

    @Test
    public void brightAliasColorIsUnchanged()
    {
        Color red = AudioChannelView.toReadable(0xD9381E);
        assertEquals(Color.rgb(0xD9, 0x38, 0x1E), red);
    }

    private static double contrast(Color color)
    {
        return (luminance(color) + 0.05) / (luminance(Color.web("#161b22")) + 0.05);
    }

    private static double luminance(Color color)
    {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double channel)
    {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
