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

import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Swing host for the JavaFX Tuners tab.
 */
public class TunerViewPanel extends JFXPanel
{
    /**
     * Constructs an instance
     * @param tunerManager for tuners
     * @param userPreferences for making recordings in the tuner editor
     */
    public TunerViewPanel(TunerManager tunerManager, UserPreferences userPreferences)
    {
        Platform.setImplicitExit(false);
        ThemeManager.getInstance().registerSwing(this);

        Platform.runLater(() -> {
            Scene scene = new Scene(new TunerView(tunerManager, userPreferences));
            ThemeManager.getInstance().register(scene);
            setScene(scene);
        });
    }
}
