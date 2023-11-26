/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.audio.call.CallView;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playbackfx.AudioPlaybackChannelsView;
import io.github.dsheirer.audio.playbackfx.IPanelRevalidationRequestListener;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.map.MapPanel;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControllerPanel extends JPanel implements IPanelRevalidationRequestListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);
    private static final long serialVersionUID = 1L;

    @Resource
    private AudioPanel mAudioPanel;
    @Resource
    private CallView mCallView;
    @Resource
    private MapPanel mMapPanel;
    @Resource
    private TunerViewPanel mTunerManagerPanel;
    @Resource
    private NowPlayingPanel mNowPlayingPanel;
    @Resource
    private AudioPlaybackChannelsView mAudioPlaybackChannelsView;
    private JideTabbedPane mTabbedPane;
    private JFXPanel mJFXCallViewPanel;
    private JFXPanel mJFXAudioPlaybackViewPanel;
    private int mPlaylistEditorTabIndex = -1;

    /**
     * Constructs an instance
     */
    public ControllerPanel()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[shrink 0]0[grow,fill]0[]"));

//        add(mAudioPanel, "wrap");
        add(getAudioPlaybackViewPanel(), "wrap");
        mAudioPlaybackChannelsView.setPanelRevalidationRequestListener(this);

        mTabbedPane = new JideTabbedPane()
        {
            @Override
            public void setSelectedIndex(int index)
            {
                if(index == mPlaylistEditorTabIndex)
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
        mTabbedPane.addTab("Now Playing", getNowPlayingPanel());
        mTabbedPane.addTab("Calls", getCallViewPanel());
        mTabbedPane.addTab("Map", mMapPanel);
        mTabbedPane.addTab("Tuners", mTunerManagerPanel);

        Icon playIcon = IconFontSwing.buildIcon(FontAwesome.PLAY_CIRCLE_O, 20, Color.DARK_GRAY);
        mTabbedPane.addTab("Playlist Editor", playIcon, new JLabel("Show Playlist Manager"));
        mPlaylistEditorTabIndex = mTabbedPane.getTabCount() - 1;

        //Set preferred size to influence the split between these panels
        mTabbedPane.setPreferredSize(new Dimension(880, 500));

        add(mTabbedPane, "wrap");
    }

    /**
     * Implements listener interface to revalidate JavaFX panel when the audio channel view style has changed.
     */
    @Override
    public void revalidatePanel()
    {
        Platform.runLater(() ->
        {
            //Hack: this is very heavy-handed, however I couldn't find a way to get the JFXPanel to resize the height
            //when the child content changes with the style of the audio playback panels.  Nonetheless, this works.
            Scene scene = getAudioPlaybackViewPanel().getScene();
            getAudioPlaybackViewPanel().setScene(null);
            getAudioPlaybackViewPanel().setScene(scene);
        });
    }

    /**
     * Now playing panel.
     */
    public NowPlayingPanel getNowPlayingPanel()
    {
        return mNowPlayingPanel;
    }

    /**
     * Call view panel wrapped in an JFXPanel.  This is lazy constructed on the JavaFX platform thread.
     * @return constructed JFXPanel.
     */
    private JFXPanel getCallViewPanel()
    {
        createJFXPanels();
        return mJFXCallViewPanel;
    }

    private JFXPanel getAudioPlaybackViewPanel()
    {
        createJFXPanels();
        return mJFXAudioPlaybackViewPanel;
    }

    private void createJFXPanels()
    {
        if(mJFXCallViewPanel == null || mJFXAudioPlaybackViewPanel == null)
        {
            mJFXAudioPlaybackViewPanel = new JFXPanel();
            mJFXCallViewPanel = new JFXPanel();

            Platform.runLater(() -> {
                mJFXCallViewPanel.setScene(new Scene(mCallView));
                mJFXAudioPlaybackViewPanel.setScene(new Scene(mAudioPlaybackChannelsView));
            });
        }
    }
}
