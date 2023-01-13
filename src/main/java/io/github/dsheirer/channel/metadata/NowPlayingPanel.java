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
package io.github.dsheirer.channel.metadata;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.channel.details.ChannelDetailPanel;
import io.github.dsheirer.gui.power.ChannelPowerPanel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.decode.event.DecodeEventPanel;
import io.github.dsheirer.module.decode.event.MessageActivityPanel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import java.awt.Color;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;

public class NowPlayingPanel extends JPanel
{
    private ChannelMetadataPanel mChannelMetadataPanel;
    private ChannelDetailPanel mChannelDetailPanel;
    private DecodeEventPanel mDecodeEventPanel;
    private MessageActivityPanel mMessageActivityPanel;
    private ChannelPowerPanel mChannelPowerPanel;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public NowPlayingPanel(PlaylistManager playlistManager, IconModel iconModel, UserPreferences userPreferences,
                           SettingsManager settingsManager)
    {
        mChannelDetailPanel = new ChannelDetailPanel(playlistManager.getChannelProcessingManager());
        mDecodeEventPanel = new DecodeEventPanel(iconModel, userPreferences, playlistManager.getAliasModel());
        mMessageActivityPanel = new MessageActivityPanel(userPreferences);
        mChannelMetadataPanel = new ChannelMetadataPanel(playlistManager, iconModel, userPreferences);
        mChannelPowerPanel = new ChannelPowerPanel(playlistManager, settingsManager);

        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );

        JideTabbedPane tabbedPane = new JideTabbedPane();
        tabbedPane.addTab("Details", mChannelDetailPanel);
        tabbedPane.addTab("Events", mDecodeEventPanel);
        tabbedPane.addTab("Messages", mMessageActivityPanel);
        tabbedPane.addTab("Channel", mChannelPowerPanel);
        tabbedPane.setFont(this.getFont());
        tabbedPane.setForeground(Color.BLACK);

        //Register state change listener to toggle visibility state for channel tab to turn-on/off FFT processing
        tabbedPane.addChangeListener(e -> mChannelPowerPanel.setPanelVisible(tabbedPane.getSelectedIndex() == tabbedPane
                .indexOfComponent(mChannelPowerPanel)));

        JideSplitPane splitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        splitPane.setShowGripper(true);
        splitPane.add(mChannelMetadataPanel);
        splitPane.add(tabbedPane);
        add(splitPane);

        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelDetailPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mDecodeEventPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mMessageActivityPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelPowerPanel);
    }
}
