/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.properties.SystemProperties;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.JMenuItem;

/**
 * Menu item to disable the spectrum and waterfall and no longer monitor a tuner.
 */
public class DisableSpectrumWaterfallMenuItem extends JMenuItem
{
    private SpectralDisplayPanel mSpectralDisplayPanel;

    /**
     * Construct an instance
     * @param spectralDisplayPanel to disable.
     */
    public DisableSpectrumWaterfallMenuItem(SpectralDisplayPanel spectralDisplayPanel)
    {
        super("Disable Spectrum & Waterfall");
        setIcon(IconFontSwing.buildIcon(FontAwesome.EYE_SLASH, 12));

        mSpectralDisplayPanel = spectralDisplayPanel;

        addActionListener(e -> {
            SystemProperties properties = SystemProperties.getInstance();
            properties.set(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, false);
            mSpectralDisplayPanel.clearTuner();
        });
    }
}

