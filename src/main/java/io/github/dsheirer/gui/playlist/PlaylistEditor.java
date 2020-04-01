/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.gui.playlist.alias.AliasEditor;
import io.github.dsheirer.gui.playlist.channel.ChannelEditor;
import io.github.dsheirer.gui.playlist.manager.PlaylistManagerEditor;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceEditor;
import io.github.dsheirer.gui.playlist.streaming.StreamingEditor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.PreferenceEditorViewRequest;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceManager;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX playlist (channels, aliases, etc) editor
 */
public class PlaylistEditor extends Application
{
    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private JavaFxWindowManager mJavaFxWindowManager;
    private Stage mStage;
    private BorderPane mContent;
    private MenuBar mMenuBar;
    private TabPane mTabPane;
    private Tab mPlaylistsTab;
    private Tab mChannelsTab;
    private Tab mAliasesTab;
    private Tab mRadioReferenceTab;
    private Tab mStreamingTab;

    public PlaylistEditor(PlaylistManager playlistManager, UserPreferences userPreferences, JavaFxWindowManager manager)
    {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;
        mJavaFxWindowManager = manager;
    }

    /**
     * Test constructor - do not use!!
     */
    public PlaylistEditor()
    {
        mUserPreferences = new UserPreferences();

        AliasModel aliasModel = new AliasModel();
        BroadcastModel broadcastModel = new BroadcastModel(aliasModel, new IconManager(), mUserPreferences);
        ChannelMapModel channelMapModel = new ChannelMapModel();
        TunerConfigurationModel tunerConfigurationModel = new TunerConfigurationModel();
        TunerModel tunerModel = new TunerModel(tunerConfigurationModel);
        mPlaylistManager = new PlaylistManager(aliasModel, broadcastModel, new ChannelModel(), channelMapModel,
            tunerModel, mUserPreferences, new ChannelProcessingManager(channelMapModel,
            new EventLogManager(aliasModel, mUserPreferences),
            new SourceManager(tunerModel, new SettingsManager(tunerConfigurationModel), mUserPreferences),
            aliasModel, mUserPreferences), new IconManager());

        mPlaylistManager.init();

        mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, channelMapModel);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        mStage.setTitle("Playlist Editor");
        Scene scene = new Scene(getContent(), 1000, 750);
        mStage.setScene(scene);
        mStage.show();
    }

    private Parent getContent()
    {
        if(mContent == null)
        {
            mContent = new BorderPane();
            mContent.setTop(getMenuBar());
            mContent.setCenter(getTabPane());
        }

        return mContent;
    }

    private MenuBar getMenuBar()
    {
        if(mMenuBar == null)
        {
            mMenuBar = new MenuBar();

            //File Menu
            Menu fileMenu = new Menu("File");

            MenuItem closeItem = new MenuItem("Close");
            closeItem.setOnAction(event -> mStage.close());
            fileMenu.getItems().add(closeItem);

            mMenuBar.getMenus().add(fileMenu);

            Menu editMenu = new Menu("Edit");
            MenuItem userPreferenceItem = new MenuItem("User Preferences");
            userPreferenceItem.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    MyEventBus.getEventBus()
                        .post(new PreferenceEditorViewRequest(PreferenceEditorType.TALKGROUP_FORMAT));
                }
            });
            editMenu.getItems().add(userPreferenceItem);
            mMenuBar.getMenus().add(editMenu);
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
            mAliasesTab.setContent(new AliasEditor(mPlaylistManager, mUserPreferences));
        }

        return mAliasesTab;
    }

    private Tab getChannelsTab()
    {
        if(mChannelsTab == null)
        {
            mChannelsTab = new Tab("Channels");
            mChannelsTab.setContent(new ChannelEditor(mPlaylistManager));
        }

        return mChannelsTab;
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
            mRadioReferenceTab.setContent(new RadioReferenceEditor(mUserPreferences,
                mPlaylistManager.getRadioReference(), mJavaFxWindowManager));
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

    public static void main(String[] args)
    {
        launch(args);
    }
}
