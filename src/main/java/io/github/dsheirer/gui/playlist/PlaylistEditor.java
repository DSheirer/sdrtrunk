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
import io.github.dsheirer.gui.playlist.radioreference.ViewRadioReferenceRequest;
import io.github.dsheirer.gui.playlist.streaming.StreamingEditor;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * JavaFX playlist, channels, aliases, streaming and radioreference.com import editor
 */
public class PlaylistEditor extends BorderPane
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistEditor.class);

    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private Settings mSettings;
    private MenuBar mMenuBar;
    private TabPane mTabPane;
    private Tab mPlaylistsTab;
    private Tab mChannelsTab;
    private Tab mAliasesTab;
    private Tab mRadioReferenceTab;
    private Tab mStreamingTab;
    private AliasEditor mAliasEditor;
    private ChannelEditor mChannelEditor;
    private RadioReferenceEditor mRadioReferenceEditor;

    /**
     * Constructs an instance
     * @param playlistManager for alias and channel models
     * @param tunerManager for tuners
     * @param settings for settings
     */
    public PlaylistEditor(PlaylistManager playlistManager, TunerManager tunerManager, Settings settings)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mSettings = settings;

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
            case RADIOREFERENCE:
                if(request instanceof ViewRadioReferenceRequest viewRequest)
                {
                    getTabPane().getSelectionModel().select(getRadioReferenceTab());
                    getRadioReferenceEditor().view(viewRequest);
                }
                break;
            case STREAM:
                getTabPane().getSelectionModel().select(getStreamingTab());
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

            //File
            MenuItem screenCaptureItem = new MenuItem("Screen Capture");
            screenCaptureItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN));
            screenCaptureItem.setOnAction(event -> captureScreen());

            MenuItem closeItem = new MenuItem("Close");
            closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
            closeItem.setOnAction(event -> getMenuBar().getScene().getWindow().hide());

            mMenuBar.getMenus().add(new Menu("File", null, screenCaptureItem, new SeparatorMenuItem(), closeItem));

            //Tools: the same windows as the main window's Tools menu
            MenuItem iconManagerItem = new MenuItem("Icon Manager");
            iconManagerItem.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));

            MenuItem settingsItem = new MenuItem("Settings...");
            settingsItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
            settingsItem.setOnAction(event -> MyEventBus.getGlobalEventBus()
                .post(new ViewSettingsEditorRequest(SettingsPageType.FORMATS)));

            mMenuBar.getMenus().add(new Menu("Tools", null, iconManagerItem, settingsItem));
        }

        return mMenuBar;
    }

    /**
     * Saves an image of the playlist editor window to the screen captures folder.
     */
    private void captureScreen()
    {
        WritableImage image = getMenuBar().getScene().snapshot(null);
        final BufferedImage bufferedImage = io.github.dsheirer.gui.FxToolkit.toBufferedImage(image);
        String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";

        ThreadPool.CACHED.submit(() -> {
            Path captureFile = null;

            try
            {
                captureFile = mSettings.getDirectorySettings().getDirectoryScreenCapture().resolve(filename);
                ImageIO.write(bufferedImage, "png", captureFile.toFile());
            }
            catch(Exception e)
            {
                mLog.error("Couldn't write screen capture to file [" + captureFile + "]", e);
            }
        });
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
            mAliasEditor = new AliasEditor(mPlaylistManager, mSettings);
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
            mChannelEditor = new ChannelEditor(mPlaylistManager, mTunerManager, mSettings);
        }

        return mChannelEditor;
    }

    private Tab getPlaylistsTab()
    {
        if(mPlaylistsTab == null)
        {
            mPlaylistsTab = new Tab("Playlists");
            mPlaylistsTab.setContent(new PlaylistManagerEditor(mPlaylistManager, mSettings));
        }

        return mPlaylistsTab;
    }

    private Tab getRadioReferenceTab()
    {
        if(mRadioReferenceTab == null)
        {
            mRadioReferenceTab = new Tab("Radio Reference");
            mRadioReferenceTab.setContent(getRadioReferenceEditor());
        }

        return mRadioReferenceTab;
    }

    private RadioReferenceEditor getRadioReferenceEditor()
    {
        if(mRadioReferenceEditor == null)
        {
            mRadioReferenceEditor = new RadioReferenceEditor(mSettings, mPlaylistManager);
        }

        return mRadioReferenceEditor;
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
