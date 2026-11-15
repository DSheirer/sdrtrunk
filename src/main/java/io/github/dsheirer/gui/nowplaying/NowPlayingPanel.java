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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Swing panel that holds the JavaFX Now Playing view for the main application window.  The JavaFX view is created
 * on the JavaFX application thread after construction.
 */
public class NowPlayingPanel extends JFXPanel
{
    private NowPlayingView mNowPlayingView;
    private boolean mDetailTabsVisible;

    /**
     * Constructs an instance
     * @param playlistManager for the channels
     * @param iconModel for alias icons
     * @param userPreferences for display formats and column settings
     * @param detailTabsVisible to show the channel detail tabs
     */
    public NowPlayingPanel(PlaylistManager playlistManager, IconModel iconModel, UserPreferences userPreferences,
                           boolean detailTabsVisible)
    {
        mDetailTabsVisible = detailTabsVisible;
        Platform.setImplicitExit(false);
        ThemeManager.getInstance().registerSwing(this);

        Platform.runLater(() -> {
            NowPlayingView view;

            synchronized(NowPlayingPanel.this)
            {
                view = new NowPlayingView(playlistManager, iconModel, userPreferences, mDetailTabsVisible);
                mNowPlayingView = view;
            }

            Scene scene = new Scene(view);
            ThemeManager.getInstance().register(scene);
            setScene(scene);
        });
    }

    /**
     * Changes the visibility of the channel details tabs.  Can be called from any thread.
     * @param visible true to show or false to hide.
     */
    public synchronized void setDetailTabsVisible(boolean visible)
    {
        mDetailTabsVisible = visible;

        if(mNowPlayingView != null)
        {
            NowPlayingView view = mNowPlayingView;
            Platform.runLater(() -> view.setDetailTabsVisible(visible));
        }
    }
}
