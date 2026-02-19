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
package io.github.dsheirer.controller;

import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapPanel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import java.awt.Color;
import java.awt.Dimension;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControllerPanel extends JPanel
{
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);
    private static final long serialVersionUID = 1L;
    private int mSettingsTabIndex = -1;

    private AudioPanel mAudioPanel;
    private NowPlayingPanel mNowPlayingPanel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;

    private JideTabbedPane mTabbedPane;

    public ControllerPanel(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible)
    {
        mAudioPanel = new AudioPanel(iconModel, userPreferences, settingsManager, audioPlaybackManager,
            playlistManager.getAliasModel());
        mNowPlayingPanel = new NowPlayingPanel(playlistManager, iconModel, userPreferences, settingsManager, detailTabsVisible);
        mMapPanel = new MapPanel(mapService, playlistManager.getAliasModel(), iconModel, settingsManager, userPreferences);
        mTunerManagerPanel = new TunerViewPanel(tunerManager, userPreferences);

        init();
    }

    /**
     * Now playing panel.
     */
    public NowPlayingPanel getNowPlayingPanel()
    {
        return mNowPlayingPanel;
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[]0[grow,fill]0[]"));

        add(mAudioPanel, "wrap");

        mTabbedPane = new JideTabbedPane()
        {
            @Override
            public void setSelectedIndex(int index)
            {
                if(index == mSettingsTabIndex)
                {
                    MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest());
                }
                else
                {
                    super.setSelectedIndex(index);
                }
            }
        };
        mTabbedPane.setFont(this.getFont());
        mTabbedPane.setForeground(Color.BLACK);
        mTabbedPane.addTab("Now Playing", mNowPlayingPanel);
        mTabbedPane.addTab("Map", mMapPanel);
        mTabbedPane.addTab("Tuners", mTunerManagerPanel);

        Icon playIcon = IconFontSwing.buildIcon(FontAwesome.PLAY_CIRCLE_O, 20, Color.DARK_GRAY);
        mTabbedPane.addTab("Playlist Editor", playIcon, new JLabel("Show Playlist Manager"));
        mSettingsTabIndex = mTabbedPane.getTabCount() - 1;

        //Set preferred size to influence the split between these panels
        mTabbedPane.setPreferredSize(new Dimension(880, 500));

        add(mTabbedPane, "wrap");
    }
}
