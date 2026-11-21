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
package io.github.dsheirer.gui.playlist.manager;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistStore.PlaylistStoreException;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.util.ThreadPool;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for managing the playlists that are stored in the database: select the playlist to use, create, copy,
 * rename and delete playlists, import playlist files and export a playlist to a playlist file that older versions of
 * sdrtrunk can load.
 */
public class PlaylistManagerEditor extends HBox
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistManagerEditor.class);

    private static final FileChooser.ExtensionFilter PLAYLIST_FILE_FILTER =
        new FileChooser.ExtensionFilter("Playlist Files (*.xml)", "*.xml");
    private static final FileChooser.ExtensionFilter ALL_FILES_FILE_FILTER =
        new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");
    private static final DateTimeFormatter MODIFIED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PlaylistManager mPlaylistManager;
    private Settings mSettings;
    private TableView<PlaylistRecord> mPlaylistTableView;
    private Label mPlaceholderLabel;
    private VBox mButtonBox;
    private Button mSelectButton;
    private Button mNewButton;
    private Button mCloneButton;
    private Button mRenameButton;
    private Button mImportButton;
    private Button mExportButton;
    private Button mDeleteButton;
    private boolean mBusy;

    /**
     * Constructs an instance
     * @param playlistManager for managing playlist
     * @param settings for accessing preferences
     */
    public PlaylistManagerEditor(PlaylistManager playlistManager, Settings settings)
    {
        mPlaylistManager = playlistManager;
        mSettings = settings;

        //Register to receive preferences updates
        MyEventBus.getGlobalEventBus().register(this);

        setPadding(new Insets(5, 5, 5, 5));
        HBox.setHgrow(getPlaylistTableView(), Priority.ALWAYS);
        getChildren().addAll(getPlaylistTableView(), getButtonBox());
        refresh(null);
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Indicates if the playlist is the loaded (active) playlist
     */
    private boolean isCurrent(PlaylistRecord playlist)
    {
        return playlist != null && playlist.name().equals(mPlaylistManager.getActivePlaylistName());
    }

    private PlaylistRecord getSelected()
    {
        return getPlaylistTableView().getSelectionModel().getSelectedItem();
    }

    private Window getWindow()
    {
        return getScene() != null ? getScene().getWindow() : null;
    }

    /**
     * Reloads the list of playlists from the database in the background.
     * @param select name of the playlist to select after the reload, or null to keep the current selection
     */
    private void refresh(String select)
    {
        PlaylistRecord selected = getSelected();
        String toSelect = select != null ? select : (selected != null ? selected.name() : null);

        ThreadPool.CACHED.submit(() -> {
            try
            {
                List<PlaylistRecord> playlists = mPlaylistManager.getPlaylists();
                Platform.runLater(() -> {
                    getPlaylistTableView().getItems().setAll(playlists);
                    getPlaceholderLabel().setText("No playlists");

                    for(PlaylistRecord playlist: playlists)
                    {
                        if(playlist.name().equals(toSelect))
                        {
                            getPlaylistTableView().getSelectionModel().select(playlist);
                        }
                    }

                    updateButtons();
                });
            }
            catch(PlaylistStoreException pse)
            {
                Platform.runLater(() -> {
                    getPlaylistTableView().getItems().clear();
                    getPlaceholderLabel().setText("Playlists are stored in the database, which isn't available:\n" +
                        pse.getMessage());
                    updateButtons();
                });
            }
        });
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("Loading playlists ...");
            mPlaceholderLabel.setWrapText(true);
        }

        return mPlaceholderLabel;
    }

    private TableView<PlaylistRecord> getPlaylistTableView()
    {
        if(mPlaylistTableView == null)
        {
            mPlaylistTableView = new TableView<>();
            mPlaylistTableView.setAccessibleText("Playlists");
            mPlaylistTableView.setPlaceholder(getPlaceholderLabel());

            TableColumn<PlaylistRecord,PlaylistRecord> selectedColumn = new TableColumn<>("Selected");
            selectedColumn.setPrefWidth(80);
            selectedColumn.setSortable(false);
            selectedColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            selectedColumn.setCellFactory(param -> {
                TableCell<PlaylistRecord,PlaylistRecord> cell = new TableCell<>()
                {
                    @Override
                    protected void updateItem(PlaylistRecord item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if(empty || item == null || !isCurrent(item))
                        {
                            setGraphic(null);
                            setAccessibleText(empty || item == null ? null : "Not selected");
                        }
                        else
                        {
                            IconNode iconNode = new IconNode(FontAwesome.CHECK);
                            iconNode.setFill(Color.GREEN);
                            setGraphic(iconNode);
                            setAccessibleText("Selected");
                        }
                    }
                };
                cell.setAlignment(Pos.CENTER);
                return cell;
            });

            TableColumn<PlaylistRecord,String> nameColumn = new TableColumn<>("Playlist");
            nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name()));
            nameColumn.setComparator(String.CASE_INSENSITIVE_ORDER);
            nameColumn.setPrefWidth(350);

            TableColumn<PlaylistRecord,Number> channelsColumn = new TableColumn<>("Channels");
            channelsColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().channels()));
            channelsColumn.setPrefWidth(90);

            TableColumn<PlaylistRecord,Number> aliasesColumn = new TableColumn<>("Aliases");
            aliasesColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().aliases()));
            aliasesColumn.setPrefWidth(90);

            TableColumn<PlaylistRecord,String> modifiedColumn = new TableColumn<>("Modified");
            modifiedColumn.setCellValueFactory(param -> new SimpleStringProperty(MODIFIED_FORMAT.format(
                Instant.ofEpochMilli(param.getValue().modified()).atZone(ZoneId.systemDefault()))));
            modifiedColumn.setPrefWidth(150);

            mPlaylistTableView.getColumns().addAll(selectedColumn, nameColumn, channelsColumn, aliasesColumn,
                modifiedColumn);
            mPlaylistTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateButtons());

            //User double-clicks on an entry - make that entry the selected playlist.
            mPlaylistTableView.setOnMouseClicked(event -> {
                if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2 &&
                    getSelected() != null && !isCurrent(getSelected()) && !mBusy)
                {
                    selectPlaylist(getSelected());
                }
            });
        }

        return mPlaylistTableView;
    }

    private void updateButtons()
    {
        PlaylistRecord selected = getSelected();
        boolean itemSelected = selected != null;
        boolean isCurrent = isCurrent(selected);
        boolean available = !getPlaylistTableView().getItems().isEmpty() || mPlaylistManager.isLoaded();

        getSelectButton().setDisable(mBusy || !itemSelected || isCurrent);
        getNewButton().setDisable(mBusy || !available);
        getImportButton().setDisable(mBusy || !available);
        getCloneButton().setDisable(mBusy || !itemSelected);
        getRenameButton().setDisable(mBusy || !itemSelected);
        getExportButton().setDisable(mBusy || !itemSelected);
        getDeleteButton().setDisable(mBusy || !itemSelected || isCurrent);
    }

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setSpacing(10.0);
            mButtonBox.setPadding(new Insets(5, 5, 5, 10));
            mButtonBox.setAlignment(Pos.TOP_CENTER);
            mButtonBox.getChildren().add(getSelectButton());
            mButtonBox.getChildren().add(new Separator());
            mButtonBox.getChildren().addAll(getNewButton(), getCloneButton(), getRenameButton());
            mButtonBox.getChildren().add(new Separator());
            mButtonBox.getChildren().addAll(getImportButton(), getExportButton());
            mButtonBox.getChildren().add(new Separator());
            mButtonBox.getChildren().add(getDeleteButton());
        }

        return mButtonBox;
    }

    /**
     * Runs the playlist operation in the background, with the buttons disabled, and refreshes the list when done.
     * @param operation to run, returns the name of the playlist to select afterwards, or null
     * @param errorHeader for the error dialog
     */
    private void runInBackground(PlaylistOperation operation, String errorHeader)
    {
        mBusy = true;
        updateButtons();

        ThreadPool.CACHED.submit(() -> {
            String select = null;
            String error = null;

            try
            {
                select = operation.run();
            }
            catch(PlaylistStoreException pse)
            {
                error = pse.getMessage();
            }
            catch(Exception e)
            {
                mLog.error(errorHeader, e);
                error = e.getMessage();
            }

            final String toSelect = select;
            final String message = error;

            Platform.runLater(() -> {
                mBusy = false;

                if(message != null)
                {
                    showError(errorHeader, message);
                }

                refresh(toSelect);
            });
        });
    }

    @FunctionalInterface
    private interface PlaylistOperation
    {
        String run() throws Exception;
    }

    private void showError(String header, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Playlist Manager");
        alert.setHeaderText(header);
        alert.initOwner(getWindow());
        alert.show();
    }

    /**
     * Asks the user for a playlist name
     * @return the name, or empty if cancelled
     */
    private Optional<String> askName(String title, String header, String initial)
    {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Name:");
        dialog.initOwner(getWindow());
        return dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty());
    }

    /**
     * Name based on the argument that isn't used by a listed playlist
     */
    private String getUnusedName(String name)
    {
        Set<String> names = new HashSet<>();

        for(PlaylistRecord playlist: getPlaylistTableView().getItems())
        {
            names.add(playlist.name());
        }

        String unused = name;

        for(int x = 2; names.contains(unused); x++)
        {
            unused = name + " (" + x + ")";
        }

        return unused;
    }

    /**
     * Makes the playlist the current playlist, and offers to start its auto-start channels.
     */
    private void selectPlaylist(PlaylistRecord selected)
    {
        if(selected == null)
        {
            return;
        }

        try
        {
            mPlaylistManager.selectPlaylist(selected.name());
        }
        catch(PlaylistStoreException pse)
        {
            mLog.error("Error loading playlist [" + selected.name() + "] - " + pse.getMessage());
            showError("Unable to load playlist [" + selected.name() + "]", pse.getMessage() +
                "\n\nThe current playlist remains loaded.");
            return;
        }
        finally
        {
            getPlaylistTableView().refresh();
            updateButtons();
        }

        final List<Channel> autoStartChannels = mPlaylistManager.getChannelModel().getAutoStartChannels();

        if(autoStartChannels.size() > 0)
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Would you like to auto-start your channels?", ButtonType.YES, ButtonType.NO);
            alert.setTitle("Auto-Start Channels");
            alert.setHeaderText("Discovered [" + autoStartChannels.size() + "] auto-start channel" +
                (autoStartChannels.size() > 1 ? "s" : ""));
            alert.initOwner(getWindow());
            alert.showAndWait().ifPresent(buttonType -> {
                if(buttonType == ButtonType.YES)
                {
                    boolean error = false;

                    for(Channel channel: autoStartChannels)
                    {
                        try
                        {
                            mPlaylistManager.getChannelProcessingManager().start(channel);
                        }
                        catch(ChannelException ce)
                        {
                            error = true;
                        }
                    }

                    if(error)
                    {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR,
                            "Unable to start some or all of the auto-start channels", ButtonType.OK);
                        errorAlert.setTitle("Channel Auto-Start Error(s)");
                        errorAlert.setHeaderText("Auto-Start Error");
                        errorAlert.initOwner(getWindow());
                        errorAlert.showAndWait();
                    }
                }
            });
        }
    }

    private Button getSelectButton()
    {
        if(mSelectButton == null)
        {
            mSelectButton = new Button("Select");
            mSelectButton.setTooltip(new Tooltip("Load the selected playlist and use it as the current playlist"));
            mSelectButton.setMaxWidth(Double.MAX_VALUE);
            mSelectButton.setOnAction(event -> selectPlaylist(getSelected()));
        }

        return mSelectButton;
    }

    private Button getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new Button("New");
            mNewButton.setTooltip(new Tooltip("Create a new, empty playlist"));
            mNewButton.setMaxWidth(Double.MAX_VALUE);
            mNewButton.setOnAction(event -> askName("New Playlist", "Name for the new playlist",
                getUnusedName("New Playlist")).ifPresent(name -> runInBackground(() ->
                    mPlaylistManager.createPlaylist(name).name(), "Unable to create the playlist")));
        }

        return mNewButton;
    }

    private Button getCloneButton()
    {
        if(mCloneButton == null)
        {
            mCloneButton = new Button("Clone");
            mCloneButton.setTooltip(new Tooltip("Create a copy of the selected playlist"));
            mCloneButton.setMaxWidth(Double.MAX_VALUE);
            mCloneButton.setOnAction(event -> {
                PlaylistRecord selected = getSelected();

                if(selected != null)
                {
                    askName("Clone Playlist", "Name for the copy of playlist [" + selected.name() + "]",
                        getUnusedName(selected.name() + " copy")).ifPresent(name -> runInBackground(() ->
                            mPlaylistManager.copyPlaylist(selected.name(), name).name(),
                        "Unable to copy the playlist"));
                }
            });
        }

        return mCloneButton;
    }

    private Button getRenameButton()
    {
        if(mRenameButton == null)
        {
            mRenameButton = new Button("Rename");
            mRenameButton.setTooltip(new Tooltip("Rename the selected playlist"));
            mRenameButton.setMaxWidth(Double.MAX_VALUE);
            mRenameButton.setOnAction(event -> {
                PlaylistRecord selected = getSelected();

                if(selected != null)
                {
                    askName("Rename Playlist", "New name for playlist [" + selected.name() + "]", selected.name())
                        .filter(name -> !name.equals(selected.name()))
                        .ifPresent(name -> runInBackground(() -> {
                            mPlaylistManager.renamePlaylist(selected.name(), name);
                            return name;
                        }, "Unable to rename the playlist"));
                }
            });
        }

        return mRenameButton;
    }

    private Button getImportButton()
    {
        if(mImportButton == null)
        {
            mImportButton = new Button("Import");
            mImportButton.setTooltip(new Tooltip("Import a playlist file (.xml) as a new playlist.  The file isn't " +
                "changed."));
            mImportButton.setMaxWidth(Double.MAX_VALUE);
            mImportButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Import Playlist File");
                fileChooser.setInitialDirectory(mSettings.getDirectorySettings().getDirectoryPlaylist().toFile());
                fileChooser.getExtensionFilters().addAll(PLAYLIST_FILE_FILTER, ALL_FILES_FILE_FILTER);
                File file = fileChooser.showOpenDialog(getWindow());

                if(file != null)
                {
                    String fileName = file.getName().replaceFirst("(?i)\\.xml$", "");
                    askName("Import Playlist File", "Name for the playlist imported from " + file.getName(),
                        getUnusedName(fileName)).ifPresent(name -> runInBackground(() ->
                            mPlaylistManager.importPlaylist(file.toPath(), name).name(),
                        "Unable to import the playlist file"));
                }
            });
        }

        return mImportButton;
    }

    private Button getExportButton()
    {
        if(mExportButton == null)
        {
            mExportButton = new Button("Export");
            mExportButton.setTooltip(new Tooltip("Save the selected playlist to a playlist file (.xml), for example " +
                "to use it with an older version of sdrtrunk"));
            mExportButton.setMaxWidth(Double.MAX_VALUE);
            mExportButton.setOnAction(event -> {
                PlaylistRecord selected = getSelected();

                if(selected != null)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Export Playlist [" + selected.name() + "]");
                    fileChooser.setInitialDirectory(mSettings.getDirectorySettings().getDirectoryPlaylist()
                        .toFile());
                    fileChooser.setInitialFileName(selected.name().replaceAll("[\\\\/:*?\"<>|]", "_") + ".xml");
                    fileChooser.getExtensionFilters().addAll(PLAYLIST_FILE_FILTER, ALL_FILES_FILE_FILTER);
                    File file = fileChooser.showSaveDialog(getWindow());

                    if(file != null)
                    {
                        Path path = file.getName().contains(".") ? file.toPath() :
                            file.toPath().resolveSibling(file.getName() + ".xml");
                        runInBackground(() -> {
                            mPlaylistManager.exportPlaylist(selected.name(), path);
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Playlist [" +
                                    selected.name() + "] was exported to:\n\n" + path, ButtonType.OK);
                                alert.setTitle("Playlist Manager");
                                alert.setHeaderText("Playlist exported");
                                alert.initOwner(getWindow());
                                alert.show();
                            });
                            return null;
                        }, "Unable to export the playlist");
                    }
                }
            });
        }

        return mExportButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setTooltip(new Tooltip("Delete the selected playlist from the database"));
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                PlaylistRecord selected = getSelected();

                if(selected != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete playlist [" + selected.name() +
                        "] with its " + selected.channels() + " channels and " + selected.aliases() + " aliases " +
                        "from the database?\n\nPlaylist files are not deleted.", ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(getWindow());
                    Button noButton = (Button)alert.getDialogPane().lookupButton(ButtonType.NO);
                    noButton.setDefaultButton(true);
                    Button yesButton = (Button)alert.getDialogPane().lookupButton(ButtonType.YES);
                    yesButton.setDefaultButton(false);

                    Optional<ButtonType> optional = alert.showAndWait();

                    if(optional.isPresent() && optional.get() == ButtonType.YES)
                    {
                        runInBackground(() -> {
                            mPlaylistManager.deletePlaylist(selected.name());
                            return null;
                        }, "Unable to delete the playlist");
                    }
                }
            });
        }

        return mDeleteButton;
    }

    /**
     * Receives preference update notifications via the event bus for playlist updates (the active playlist changed),
     * to refresh the list of playlists.
     * @param settingsType that was updated
     */
    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.PLAYLIST)
        {
            Platform.runLater(() -> refresh(null));
        }
    }
}
