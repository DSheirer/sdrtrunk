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
import io.github.dsheirer.module.decode.event.DecodeEventPanel;
import io.github.dsheirer.module.decode.event.MessageActivityPanel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.awt.Color;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;

/**
 * Swing panel for Now Playing channels table and channel details tab set.
 */
public class NowPlayingPanel extends JPanel
{
    @Resource
    private ChannelDetailPanel mChannelDetailPanel;
    @Resource
    private ChannelMetadataPanel mChannelMetadataPanel;
    @Resource
    private ChannelPowerPanel mChannelPowerPanel;
    @Resource
    private DecodeEventPanel mDecodeEventPanel;
    private final MessageActivityPanel mMessageActivityPanel = new MessageActivityPanel();
    private JideTabbedPane mTabbedPane;
    private JideSplitPane mSplitPane;
    private boolean mDetailTabsVisible = true;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public NowPlayingPanel()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );
        getSplitPane().add(mChannelMetadataPanel);

        if(mDetailTabsVisible)
        {
            getSplitPane().add(getTabbedPane());
        }

        add(getSplitPane());
        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelDetailPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mDecodeEventPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mMessageActivityPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelPowerPanel);
    }

    /**
     * Change the visibility of the channel details tabs panel.
     * @param visible true to show or false to hide.
     */
    public void setDetailTabsVisible(boolean visible)
    {
        //Only adjust if there is a change in state
        if(visible ^ mDetailTabsVisible)
        {
            mDetailTabsVisible = visible;

            if(mDetailTabsVisible)
            {
                getSplitPane().add(getTabbedPane());
            }
            else
            {
                getSplitPane().remove(getTabbedPane());
            }

            revalidate();
        }
    }

    private JideTabbedPane getTabbedPane()
    {
        if(mTabbedPane == null)
        {
            mTabbedPane = new JideTabbedPane();
            mTabbedPane.addTab("Details", mChannelDetailPanel);
            mTabbedPane.addTab("Events", mDecodeEventPanel);
            mTabbedPane.addTab("Messages", mMessageActivityPanel);
            mTabbedPane.addTab("Channel", mChannelPowerPanel);
            mTabbedPane.setFont(this.getFont());
            mTabbedPane.setForeground(Color.BLACK);
            //Register state change listener to toggle visibility state for channel tab to turn-on/off FFT processing
            mTabbedPane.addChangeListener(e -> mChannelPowerPanel.setPanelVisible(getTabbedPane().getSelectedIndex() == getTabbedPane()
                    .indexOfComponent(mChannelPowerPanel)));
        }

        return mTabbedPane;
    }

    /**
     * Split pane for channels table and channel details tabs.
     */
    private JideSplitPane getSplitPane()
    {
        if(mSplitPane == null)
        {
            mSplitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
            mSplitPane.setShowGripper(true);
        }

        return mSplitPane;
    }
}
