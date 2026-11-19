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

import io.github.dsheirer.gui.channel.ChannelSpectrumView;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.Settings;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Now Playing view: the table of currently processing channels above the details, events, messages and channel tabs
 * for the selected channel.
 *
 * Use this class on the JavaFX application thread.
 */
public class NowPlayingView extends SplitPane
{
    private static final double DIVIDER_POSITION = 0.5;
    private final ChannelMetadataView mChannelMetadataView;
    private final ChannelDetailView mChannelDetailView;
    private final DecodeEventView mDecodeEventView;
    private final MessageActivityView mMessageActivityView;
    private final ChannelSpectrumView mChannelSpectrumView;
    private final TabPane mTabPane = new TabPane();
    private boolean mDetailTabsVisible;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param playlistManager for the channels
     * @param iconModel for alias icons
     * @param settings for display formats and column settings
     * @param detailTabsVisible to show the detail tabs
     */
    public NowPlayingView(PlaylistManager playlistManager, IconModel iconModel, Settings settings,
                          boolean detailTabsVisible)
    {
        setOrientation(Orientation.VERTICAL);

        mChannelMetadataView = new ChannelMetadataView(playlistManager, iconModel, settings);
        mChannelDetailView = new ChannelDetailView(playlistManager.getChannelProcessingManager());
        mDecodeEventView = new DecodeEventView(iconModel, settings, playlistManager.getAliasModel());
        mMessageActivityView = new MessageActivityView(settings);
        mChannelSpectrumView = new ChannelSpectrumView(playlistManager);

        Tab detailsTab = new Tab("Details", mChannelDetailView);
        Tab channelTab = new Tab("Channel", mChannelSpectrumView);
        mTabPane.getTabs().addAll(detailsTab, new Tab("Events", mDecodeEventView),
            new Tab("Messages", mMessageActivityView), channelTab);
        mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        //Turn the channel spectrum processing on only while the channel tab is selected
        mTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            mChannelSpectrumView.setPanelVisible(newTab == channelTab);

            //Show current details each time the user opens the details tab
            if(newTab == detailsTab)
            {
                mChannelDetailView.refresh();
            }
        });

        mChannelMetadataView.addProcessingChainSelectionListener(mChannelDetailView);
        mChannelMetadataView.addProcessingChainSelectionListener(mDecodeEventView);
        mChannelMetadataView.addProcessingChainSelectionListener(mMessageActivityView);
        mChannelMetadataView.addProcessingChainSelectionListener(mChannelSpectrumView);

        getItems().add(mChannelMetadataView);
        setDetailTabsVisible(detailTabsVisible);
    }

    /**
     * Changes the visibility of the channel details tabs.
     * @param visible true to show or false to hide.
     */
    public void setDetailTabsVisible(boolean visible)
    {
        if(visible && !getItems().contains(mTabPane))
        {
            getItems().add(mTabPane);
            setDividerPositions(DIVIDER_POSITION);
        }
        else if(!visible)
        {
            getItems().remove(mTabPane);
        }

        mDetailTabsVisible = visible;
        //The channel tab is hidden with the detail tabs, so stop its spectrum processing
        mChannelSpectrumView.setPanelVisible(mDetailTabsVisible &&
            mTabPane.getSelectionModel().getSelectedIndex() == mTabPane.getTabs().size() - 1);
    }

    /**
     * Prepares this view for disposal.
     */
    public void dispose()
    {
        mChannelMetadataView.dispose();
        mDecodeEventView.dispose();
    }
}
