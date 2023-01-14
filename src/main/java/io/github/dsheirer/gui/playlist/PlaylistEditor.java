/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.alias.AliasEditor;
import io.github.dsheirer.gui.playlist.alias.AliasTabRequest;
import io.github.dsheirer.gui.playlist.channel.ChannelEditor;
import io.github.dsheirer.gui.playlist.channel.ChannelTabRequest;
import io.github.dsheirer.gui.playlist.manager.PlaylistManagerEditor;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceEditor;
import io.github.dsheirer.gui.playlist.streaming.StreamingEditor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * JavaFX playlist, channels, aliases, streaming and radioreference.com import editor
 */
public class PlaylistEditor extends BorderPane
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistEditor.class);

    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private MenuBar mMenuBar;
    private TabPane mTabPane;
    private Tab mPlaylistsTab;
    private Tab mChannelsTab;
    private Tab mAliasesTab;
    private Tab mRadioReferenceTab;
    private Tab mStreamingTab;
    private AliasEditor mAliasEditor;
    private ChannelEditor mChannelEditor;

    /**
     * Constructs an instance
     * @param playlistManager for alias and channel models
     * @param tunerManager for tuners
     * @param userPreferences for settings
     */
    public PlaylistEditor(PlaylistManager playlistManager, TunerManager tunerManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mUserPreferences = userPreferences;

        //Throw a new runnable back onto the FX thread to lazy load the editor content after the editor has been
        //constructed and shown.
        Platform.runLater(() -> {
            setTop(getMenuBar());
            setCenter(getTabPane());
        });
    }

    /**
     * Process requests for sub-editor actions like view an alias or view a channel.
     *
     * Note: this method must be invoked on the JavaFX platform thread
     * @param request to process
     */
    public void process(PlaylistEditorRequest request)
    {
        switch(request.getTabName())
        {
            case ALIAS:
                if(request instanceof AliasTabRequest)
                {
                    getTabPane().getSelectionModel().select(getAliasesTab());
                    getAliasEditor().process((AliasTabRequest)request);
                }
                break;
            case CHANNEL:
                if(request instanceof ChannelTabRequest)
                {
                    getTabPane().getSelectionModel().select(getChannelsTab());
                    getChannelEditor().process((ChannelTabRequest)request);
                }
                break;
            case PLAYLIST:
                //Ignore - this is a request to simply show te playlist editor
                break;
            default:
                mLog.warn("Unrecognized playlist editor request: " + request.getClass());
                break;
        }
    }

    private MenuBar getMenuBar()
    {
        if(mMenuBar == null)
        {
            mMenuBar = new MenuBar();

            //File Menu
            Menu fileMenu = new Menu("_File");
            fileMenu.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.ALT_ANY));

            MenuItem closeItem = new MenuItem("_Close");
            closeItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_ANY));
            closeItem.setOnAction(event -> getMenuBar().getParent().getScene().getWindow().hide());
            fileMenu.getItems().add(closeItem);
            mMenuBar.getMenus().add(fileMenu);

            Menu viewMenu = new Menu("_View");
            viewMenu.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.ALT_ANY));

            MenuItem iconManagerItem = new MenuItem("_Icon Manager");
            iconManagerItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.ALT_ANY));
            iconManagerItem.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));
            viewMenu.getItems().add(iconManagerItem);

            MenuItem userPreferenceItem = new MenuItem("_User Preferences");
            userPreferenceItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.ALT_ANY));
            userPreferenceItem.setOnAction(event -> MyEventBus.getGlobalEventBus()
                .post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.TALKGROUP_FORMAT)));
            viewMenu.getItems().add(userPreferenceItem);

            mMenuBar.getMenus().add(viewMenu);

            Menu screenShot = new Menu("_Screenshot");
            IconNode cameraNode = new IconNode(FontAwesome.CAMERA);
            cameraNode.setFill(Color.DARKGRAY);
            screenShot.setGraphic(cameraNode);
            MenuItem menuItem = new MenuItem();
            screenShot.getItems().add(menuItem);
            screenShot.setOnShowing(event -> screenShot.hide());
            screenShot.setOnShown(event -> menuItem.fire());
            menuItem.setOnAction(event -> {
                WritableImage image = getMenuBar().getScene().snapshot(null);
                final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";
                final Path captureFile = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().resolve(filename);

                ThreadPool.CACHED.submit(() -> {
                    try
                    {
                        ImageIO.write(bufferedImage, "png", captureFile.toFile());
                    }
                    catch(IOException e)
                    {
                        mLog.error("Couldn't write screen capture to file [" + captureFile.toString() + "]", e);
                    }
                });
            });
            mMenuBar.getMenus().add(screenShot);

        }

        return mMenuBar;
    }

    private TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            mTabPane.getTabs().addAll(getPlaylistsTab(), getChannelsTab(), getAliasesTab(), getStreamingTab(),
                getRadioReferenceTab());
        }

        return mTabPane;
    }

    private Tab getAliasesTab()
    {
        if(mAliasesTab == null)
        {
            mAliasesTab = new Tab("Aliases");
            mAliasesTab.setContent(getAliasEditor());
        }

        return mAliasesTab;
    }

    private AliasEditor getAliasEditor()
    {
        if(mAliasEditor == null)
        {
            mAliasEditor = new AliasEditor(mPlaylistManager, mUserPreferences);
        }

        return mAliasEditor;
    }

    private Tab getChannelsTab()
    {
        if(mChannelsTab == null)
        {
            mChannelsTab = new Tab("Channels");
            mChannelsTab.setContent(getChannelEditor());
        }

        return mChannelsTab;
    }

    private ChannelEditor getChannelEditor()
    {
        if(mChannelEditor == null)
        {
            mChannelEditor = new ChannelEditor(mPlaylistManager, mTunerManager, mUserPreferences);
        }

        return mChannelEditor;
    }

    private Tab getPlaylistsTab()
    {
        if(mPlaylistsTab == null)
        {
            mPlaylistsTab = new Tab("Playlists");
            mPlaylistsTab.setContent(new PlaylistManagerEditor(mPlaylistManager, mUserPreferences));
        }

        return mPlaylistsTab;
    }

    private Tab getRadioReferenceTab()
    {
        if(mRadioReferenceTab == null)
        {
            mRadioReferenceTab = new Tab("Radio Reference");
            mRadioReferenceTab.setContent(new RadioReferenceEditor(mUserPreferences, mPlaylistManager));
        }

        return mRadioReferenceTab;
    }

    private Tab getStreamingTab()
    {
        if(mStreamingTab == null)
        {
            mStreamingTab = new Tab("Streaming");
            mStreamingTab.setContent(new StreamingEditor(mPlaylistManager));
        }

        return mStreamingTab;
    }
}
