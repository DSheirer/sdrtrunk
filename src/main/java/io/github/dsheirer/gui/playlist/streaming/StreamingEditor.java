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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFactory;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.rrapi.type.UserFeedBroadcast;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for broadcast audio stream configurations
 */
public class StreamingEditor extends SplitPane
{
    private final static Logger mLog = LoggerFactory.getLogger(StreamingEditor.class);

    private final PlaylistManager mPlaylistManager;
    private TableView<ConfiguredBroadcast> mConfiguredBroadcastTableView;
    private MenuButton mNewButton;
    private Button mDeleteButton;
    private Button mRefreshButton;
    private TabPane mTabPane;
    private Tab mConfigurationTab;
    private Tab mAliasTab;
    private Label mRadioReferenceLoginLabel;
    private AbstractBroadcastEditor<?> mCurrentEditor;
    private final UnknownStreamEditor mUnknownEditor;
    private Map<BroadcastServerType, AbstractBroadcastEditor<?>> mEditorMap = new EnumMap<>(BroadcastServerType.class);
    private final List<UserFeedBroadcast> mBroadcastifyFeeds = new ArrayList<>();
    private ScrollPane mEditorScrollPane;
    private StreamAliasSelectionEditor mStreamAliasSelectionEditor;
    private final StreamConfigurationEditorModificationListener mStreamConfigurationEditorModificationListener =
        new StreamConfigurationEditorModificationListener();

    /**
     * Constructs an instance
     * @param playlistManager for accessing streaming model
     */
    public StreamingEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        mUnknownEditor = new UnknownStreamEditor(mPlaylistManager);
        mPlaylistManager.getRadioReference().availableProperty()
            .addListener((observable, oldValue, newValue) -> refreshBroadcastifyStreams());
        refreshBroadcastifyStreams();

        VBox buttonsBox = new VBox();
        buttonsBox.getChildren().addAll(getNewButton(), getDeleteButton(), getRefreshButton());
        buttonsBox.setPadding(new Insets(0, 0, 0, 10));
        buttonsBox.setSpacing(10);

        VBox tableAndLabelBox = new VBox();
        VBox.setVgrow(getConfiguredBroadcastTableView(), Priority.ALWAYS);
        tableAndLabelBox.getChildren().addAll(getConfiguredBroadcastTableView(), getRadioReferenceLoginLabel());

        HBox editorBox = new HBox();
        editorBox.setPadding(new Insets(10, 10, 10, 10));
        HBox.setHgrow(tableAndLabelBox, Priority.ALWAYS);
        editorBox.getChildren().addAll(tableAndLabelBox, buttonsBox);
        editorBox.setPrefHeight(50);

        setOrientation(Orientation.VERTICAL);
        getItems().addAll(editorBox, getTabPane());
    }

    private void setEditor(AbstractBroadcastEditor<?> editor)
    {
        if(editor != getCurrentEditor())
        {
            if(mCurrentEditor != null)
            {
                mCurrentEditor.modifiedProperty().removeListener(mStreamConfigurationEditorModificationListener);
            }

            mCurrentEditor = editor;

            //Register a listener on the editor modified property to detect configuration changes and refresh the
            //aliases tab
            mCurrentEditor.modifiedProperty().addListener(mStreamConfigurationEditorModificationListener);

            getEditorScrollPane().setContent(getCurrentEditor());
        }
    }

    private ScrollPane getEditorScrollPane()
    {
        if(mEditorScrollPane == null)
        {
            mEditorScrollPane = new ScrollPane();
            mEditorScrollPane.setMaxWidth(Double.MAX_VALUE);
            mEditorScrollPane.setFitToWidth(true);
            mEditorScrollPane.setContent(getCurrentEditor());
        }

        return mEditorScrollPane;
    }

    private void setBroadcastConfiguration(ConfiguredBroadcast configuredBroadcast)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getCurrentEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Streaming configuration has been modified");
            alert.setContentText("Do you want to save these changes?");
            alert.initOwner((getNewButton()).getScene().getWindow());

            //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
            alert.setResizable(true);
            alert.onShownProperty().addListener(e -> Platform.runLater(() -> alert.setResizable(false)));
            alert.showAndWait().ifPresent(buttonType -> {
                if(buttonType == ButtonType.YES)
                {
                    getCurrentEditor().save();
                }
            });
        }

        getDeleteButton().setDisable(configuredBroadcast == null);

        if(configuredBroadcast == null)
        {
            setEditor(mUnknownEditor);
        }
        else
        {
            BroadcastServerType configType = configuredBroadcast.getBroadcastServerType();

            if(configType == null)
            {
                setEditor(mUnknownEditor);
            }
            else
            {
                BroadcastServerType editorType = getCurrentEditor().getBroadcastServerType();

                if(editorType == null || editorType != configType)
                {
                    AbstractBroadcastEditor editor = mEditorMap.get(configType);

                    if(editor == null)
                    {
                        editor = StreamEditorFactory.getEditor(configType, mPlaylistManager);

                        if(editor != null)
                        {
                            mEditorMap.put(configType, editor);
                        }
                    }

                    if(editor == null)
                    {
                        editor = mUnknownEditor;
                    }

                    setEditor(editor);
                }
            }
        }

        BroadcastConfiguration broadcastConfiguration = configuredBroadcast != null ?
            configuredBroadcast.getBroadcastConfiguration() : null;

        getCurrentEditor().setItem(broadcastConfiguration);
        getStreamAliasSelectionEditor().setBroadcastConfiguration(broadcastConfiguration);
    }

    /**
     * Updates the list of broadcastify stream configurations if the service is logged in.
     */
    private void refreshBroadcastifyStreams()
    {
        if(mPlaylistManager.getRadioReference().availableProperty().get())
        {
            ThreadPool.CACHED.submit(() -> {
                try
                {
                    List<UserFeedBroadcast> feeds = mPlaylistManager.getRadioReference().getService().getUserFeeds();
                    mBroadcastifyFeeds.clear();
                    mBroadcastifyFeeds.addAll(feeds);
                }
                catch(Throwable t)
                {
                    mLog.error("Unable to refresh broadcastify stream configuration(s)");
                }
            });
        }
    }

    private StreamAliasSelectionEditor getStreamAliasSelectionEditor()
    {
        if(mStreamAliasSelectionEditor == null)
        {
            mStreamAliasSelectionEditor = new StreamAliasSelectionEditor(mPlaylistManager);
        }

        return mStreamAliasSelectionEditor;
    }

    private Label getRadioReferenceLoginLabel()
    {
        if(mRadioReferenceLoginLabel == null)
        {
            mRadioReferenceLoginLabel = new Label("Note: use Radio Reference tab to login and access Broadcastify stream configuration(s)");
            mRadioReferenceLoginLabel.visibleProperty().bind(mPlaylistManager.getRadioReference().availableProperty().not());
        }

        return mRadioReferenceLoginLabel;
    }

    private AbstractBroadcastEditor getCurrentEditor()
    {
        if(mCurrentEditor == null)
        {
            mCurrentEditor = mUnknownEditor;
        }

        return mCurrentEditor;
    }

    private TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setMaxHeight(Double.MAX_VALUE);
            mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            mTabPane.getTabs().addAll(getConfigurationTab(), getAliasTab());
        }

        return mTabPane;
    }

    private Tab getConfigurationTab()
    {
        if(mConfigurationTab == null)
        {
            mConfigurationTab = new Tab("Configuration");
            mConfigurationTab.setContent(getEditorScrollPane());
        }

        return mConfigurationTab;
    }

    private Tab getAliasTab()
    {
        if(mAliasTab == null)
        {
            mAliasTab = new Tab("Aliases");
            mAliasTab.setContent(getStreamAliasSelectionEditor());
        }

        return mAliasTab;
    }

    private MenuButton getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new MenuButton("New");
            mNewButton.setMaxWidth(Double.MAX_VALUE);
            mNewButton.setOnShowing(event -> {
                mNewButton.getItems().clear();

                for(UserFeedBroadcast feed: mBroadcastifyFeeds)
                {
                    //Only show a menu item for the feed if it's not already defined
                    if(mPlaylistManager.getBroadcastModel().getBroadcastConfiguration(feed.getDescription()) == null)
                    {
                        mNewButton.getItems().add(new CreateBroadcastifyMenuItem(feed));
                    }
                }

                for(BroadcastServerType type: BroadcastServerType.values())
                {
                    if(type != BroadcastServerType.BROADCASTIFY && type != BroadcastServerType.UNKNOWN)
                    {
                        mNewButton.getItems().add(new CreateBroadcastConfigurationMenuItem(type));
                    }
                }
            });
        }

        return mNewButton;
    }

    /**
     * Refresh broadcastify feeds.
     * @return button to refresh.
     */
    private Button getRefreshButton()
    {
        if(mRefreshButton == null)
        {
            mRefreshButton = new Button("Refresh");
            mRefreshButton.setTooltip(new Tooltip("Refresh streams available from Broadcastify"));
            mRefreshButton.setOnAction(event -> refreshBroadcastifyStreams());
        }

        return mRefreshButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                BroadcastConfiguration config = getConfiguredBroadcastTableView().getSelectionModel()
                    .getSelectedItem().getBroadcastConfiguration();

                if(config != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete the selected stream?", ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Delete Stream Configuration");
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteButton()).getScene().getWindow());

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        mPlaylistManager.getBroadcastModel().removeBroadcastConfiguration(config);
                    }
                }
            });
        }

        return mDeleteButton;
    }

    private TableView<ConfiguredBroadcast> getConfiguredBroadcastTableView()
    {
        if(mConfiguredBroadcastTableView == null)
        {
            mConfiguredBroadcastTableView = new TableView<>();
            mConfiguredBroadcastTableView.setPlaceholder(new Label("Click the New button to create a new " +
                "audio streaming configuration"));
            mConfiguredBroadcastTableView.setItems(mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts());

            TableColumn<ConfiguredBroadcast,Boolean> enabledColumn = new TableColumn("Enabled");
            enabledColumn.setCellValueFactory(new PropertyValueFactory<>("enabled"));
            enabledColumn.setCellFactory(param -> {
                TableCell<ConfiguredBroadcast,Boolean> tableCell = new TableCell<>()
                {
                    @Override
                    protected void updateItem(Boolean item, boolean empty)
                    {
                        setAlignment(Pos.CENTER);
                        setText(null);

                        if(empty || item == null || !item)
                        {
                            setGraphic(null);
                        }
                        else
                        {
                            IconNode iconNode = new IconNode(FontAwesome.CHECK);
                            iconNode.setFill(Color.GREEN);
                            setGraphic(iconNode);
                        }
                    }
                };

                return tableCell;
            });

            TableColumn nameColumn = new TableColumn("Name");
            nameColumn.setPrefWidth(300);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn typeColumn = new TableColumn();
            typeColumn.setPrefWidth(125);
            typeColumn.setText("Format");
            typeColumn.setCellValueFactory(new PropertyValueFactory<>("broadcastServerType"));


            TableColumn stateColumn = new TableColumn("Stream Status");
            stateColumn.setCellValueFactory(new PropertyValueFactory<>("broadcastState"));

            TableColumn errorColumn = new TableColumn("Last Error");
            errorColumn.setCellValueFactory(new PropertyValueFactory<>("lastBadBroadcastState"));

            mConfiguredBroadcastTableView.getColumns().addAll(enabledColumn, nameColumn, typeColumn, stateColumn, errorColumn);

            mConfiguredBroadcastTableView.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> setBroadcastConfiguration(newValue));
        }

        return mConfiguredBroadcastTableView;
    }

    /**
     * Menu item to create a broadcastify configuration
     */
    public class CreateBroadcastifyMenuItem extends MenuItem
    {
        private UserFeedBroadcast mUserFeedBroadcast;

        public CreateBroadcastifyMenuItem(UserFeedBroadcast userFeedBroadcast)
        {
            mUserFeedBroadcast = userFeedBroadcast;
            setText("Broadcastify Feed: " + mUserFeedBroadcast.getDescription());
            setOnAction(event -> {
                BroadcastConfiguration configuration = BroadcastifyFeedConfiguration.from(mUserFeedBroadcast);

                if(configuration != null)
                {
                    ConfiguredBroadcast configuredBroadcast = mPlaylistManager.getBroadcastModel()
                        .addBroadcastConfiguration(configuration);
                    getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
                }
            });
        }
    }

    /**
     * Menu item to create a new broadcast configuration
     */
    public class CreateBroadcastConfigurationMenuItem extends MenuItem
    {
        private BroadcastServerType mBroadcastServerType;

        public CreateBroadcastConfigurationMenuItem(BroadcastServerType type)
        {
            setText(type.toString());
            mBroadcastServerType = type;

            setOnAction(event -> {
                BroadcastConfiguration config = BroadcastFactory.getConfiguration(mBroadcastServerType, BroadcastFormat.MP3);
                ConfiguredBroadcast configuredBroadcast = mPlaylistManager.getBroadcastModel()
                    .addBroadcastConfiguration(config);
                getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
            });
        }
    }

    public class StreamConfigurationEditorModificationListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            //Only fire when the modification property changes from true to false.  Set the selection to null and then
            //reselect the broadcast to get the streams tab to refresh
            if(oldValue != null && newValue != null && oldValue && !newValue)
            {
                ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcastTableView().getSelectionModel().getSelectedItem();

                if(configuredBroadcast != null)
                {
                    getConfiguredBroadcastTableView().getSelectionModel().select(null);
                    getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
                }
            }
        }
    }
}
