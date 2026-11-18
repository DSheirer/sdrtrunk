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

package io.github.dsheirer.gui.map;

import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Swing panel that holds the JavaFX map view for the main application window.  The JavaFX view is created on the
 * JavaFX application thread after construction.
 */
public class MapPanel extends JFXPanel
{
    /**
     * Constructs an instance
     * @param mapService for the entities to plot
     * @param iconModel for alias icons
     * @param userPreferences for the map view and the user's location
     * @param frequencyIndex of the radio reference frequencies near the user, for the trunked system sites, or null
     * @param playlistActions to add sites to the playlist or enable their channels, or null
     */
    public MapPanel(MapService mapService, IconModel iconModel, UserPreferences userPreferences,
                    RadioReferenceFrequencyIndex frequencyIndex, RadioReferencePlaylistActions playlistActions)
    {
        Platform.setImplicitExit(false);
        ThemeManager.getInstance().registerSwing(this);

        Platform.runLater(() -> {
            MapView view = new MapView(mapService, iconModel, userPreferences, frequencyIndex, playlistActions, true);
            Scene scene = new Scene(view);
            ThemeManager.getInstance().register(scene);
            setScene(scene);
        });
    }
}
