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

package io.github.dsheirer.gui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.Test;

class ColorContrastTest
{
    private static final Color DARK_BACKGROUND = new Color(0x2B2B2B);
    private static final Color SELECTION_BACKGROUND = new Color(0x214283);

    @Test
    void blackBecomesWhite()
    {
        assertEquals(Color.WHITE, ColorContrast.getReadable(Color.BLACK, DARK_BACKGROUND));
    }

    @Test
    void lowContrastColorBecomesComplement()
    {
        assertEquals(Color.YELLOW, ColorContrast.getReadable(Color.BLUE, DARK_BACKGROUND));
    }

    @Test
    void readableColorsAreUnchanged()
    {
        assertEquals(Color.RED, ColorContrast.getReadable(Color.RED, DARK_BACKGROUND));
        assertEquals(Color.ORANGE, ColorContrast.getReadable(Color.ORANGE, DARK_BACKGROUND));
    }

    @Test
    void everyGrayIsReadable()
    {
        for(int value = 0; value < 256; value++)
        {
            Color gray = new Color(value, value, value);

            for(Color background: new Color[]{DARK_BACKGROUND, SELECTION_BACKGROUND, new Color(127, 127, 127)})
            {
                assertTrue(ColorContrast.getContrastRatio(ColorContrast.getReadable(gray, background), background) >=
                    ColorContrast.MINIMUM_CONTRAST_RATIO, "Gray " + value + " on " + background);
            }
        }
    }
}
