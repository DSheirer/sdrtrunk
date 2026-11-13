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

package io.github.dsheirer.gui.spectrum;

import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Separate window with a tuner spectral display.  The window starts with the saved spectral display settings, but
 * changes made in the window are not saved.
 */
public class SpectralDisplayWindow
{
    private SpectralDisplayWindow()
    {
    }

    /**
     * Opens a window that shows the tuner.  Can be called from any thread once the JavaFX toolkit is running.
     */
    public static void show(PlaylistManager playlistManager, DiscoveredTunerModel discoveredTunerModel, Tuner tuner)
    {
        Platform.runLater(() -> {
            TunerSpectralDisplay display = new TunerSpectralDisplay(playlistManager, discoveredTunerModel, false);
            Scene scene = new Scene(display, 1280, 600);
            ThemeManager.getInstance().register(scene);

            Stage stage = new Stage();
            stage.setScene(scene);
            String application = SystemProperties.getInstance().getApplicationName();
            stage.titleProperty().bind(Bindings.createStringBinding(() -> {
                Tuner displayed = display.displayedTunerProperty().get();
                return application + " - " + (displayed != null ? displayed.getPreferredName() : "Spectral Display");
            }, display.displayedTunerProperty()));
            stage.setOnHidden(e -> ThreadPool.CACHED.submit(display::dispose));
            stage.show();

            ThreadPool.CACHED.submit(() -> display.showTuner(tuner));
        });
    }
}
