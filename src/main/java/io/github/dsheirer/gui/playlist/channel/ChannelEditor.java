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

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.textfield.TextFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JavaFX editor for managing channel configurations.
 */
public class ChannelEditor extends SplitPane
{
    private PlaylistManager mPlaylistManager;
    private TableView<Channel> mChannelTableView;
    private Label mPlaceholderLabel;
    private MenuButton mNewButton;
    private Button mDeleteButton;
    private Button mCloneButton;
    private VBox mButtonBox;
    private HBox mSearchBox;
    private TextField mSearchField;
    private ChannelConfigurationEditor mChannelConfigurationEditor;
    private UnknownConfigurationEditor mUnknownConfigurationEditor;
    private Map<DecoderType,ChannelConfigurationEditor> mChannelConfigurationEditorMap = new HashMap();

    /**
     * Constructs an instance
     * @param playlistManager containing playlists and channel configurations
     */
    public ChannelEditor(PlaylistManager playlistManager)
    {

        mPlaylistManager = playlistManager;
        mUnknownConfigurationEditor = new UnknownConfigurationEditor(mPlaylistManager);

        HBox channelsBox = new HBox();
        channelsBox.setPadding(new Insets(5, 5, 5, 5));
        channelsBox.setSpacing(5.0);
        HBox.setHgrow(getChannelTableView(), Priority.ALWAYS);
        channelsBox.getChildren().addAll(getChannelTableView(), getButtonBox());

        VBox topBox = new VBox();
        VBox.setVgrow(channelsBox, Priority.ALWAYS);
        topBox.getChildren().addAll(getSearchBox(), channelsBox);

        setOrientation(Orientation.VERTICAL);
        getItems().addAll(topBox, getChannelConfigurationEditor());

        //TODO: add a 'Features' column that has icons: enabled/running, auto-start, logging, or recording
    }

    private void setChannel(Channel channel)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getChannelConfigurationEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Channel configuration has been modified");
            alert.setContentText("Do you want to save these changes?");
            alert.initOwner(((Node)getButtonBox()).getScene().getWindow());

            //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
            alert.setResizable(true);
            alert.onShownProperty().addListener(e -> {
                Platform.runLater(() -> alert.setResizable(false));
            });

            Optional<ButtonType> result = alert.showAndWait();

            if(result.get() == ButtonType.YES)
            {
                getChannelConfigurationEditor().save();
            }
        }

        getCloneButton().setDisable(channel == null);
        getDeleteButton().setDisable(channel == null);

        if(channel == null)
        {
            setChannelConfigurationEditor(mUnknownConfigurationEditor);
        }
        else
        {
            DecoderType channelDecoderType = null;

            if(channel.getDecodeConfiguration() != null)
            {
                channelDecoderType = channel.getDecodeConfiguration().getDecoderType();
            }

            if(channelDecoderType == null)
            {
                setChannelConfigurationEditor(mUnknownConfigurationEditor);
            }
            else
            {
                DecoderType editorDecoderType = getChannelConfigurationEditor().getDecoderType();

                if(editorDecoderType == null || editorDecoderType != channelDecoderType)
                {
                    ChannelConfigurationEditor editor = mChannelConfigurationEditorMap.get(channelDecoderType);

                    if(editor == null)
                    {
                        editor = ChannelConfigurationEditorFactory.getEditor(channelDecoderType, mPlaylistManager);

                        if(editor != null)
                        {
                            mChannelConfigurationEditorMap.put(channelDecoderType, editor);
                        }
                    }

                    if(editor == null)
                    {
                        editor = mUnknownConfigurationEditor;
                    }

                    setChannelConfigurationEditor(editor);
                }
            }
        }

        getChannelConfigurationEditor().setItem(channel);
    }

    private void createNewChannel(DecoderType decoderType)
    {
        Channel channel = new Channel();
        channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoderType));
        mPlaylistManager.getChannelModel().addChannel(channel);
        getChannelTableView().getSelectionModel().select(channel);
    }

    /**
     * Sets the editor to be the current channel configuration editor
     */
    private void setChannelConfigurationEditor(ChannelConfigurationEditor editor)
    {
        if(editor != getChannelConfigurationEditor())
        {
            getItems().remove(getChannelConfigurationEditor());
            mChannelConfigurationEditor = editor;
            getItems().add(getChannelConfigurationEditor());
        }
    }

    private ChannelConfigurationEditor getChannelConfigurationEditor()
    {
        if(mChannelConfigurationEditor == null)
        {
            mChannelConfigurationEditor = mUnknownConfigurationEditor;
            mChannelConfigurationEditor.setMaxWidth(Double.MAX_VALUE);
        }

        return mChannelConfigurationEditor;
    }

    private HBox getSearchBox()
    {
        if(mSearchBox == null)
        {
            mSearchBox = new HBox();
            mSearchBox.setAlignment(Pos.CENTER_LEFT);
            mSearchBox.setPadding(new Insets(5, 5, 0, 15));
            mSearchBox.setSpacing(5);

            Label searchLabel = new Label("Search:");
            searchLabel.setAlignment(Pos.CENTER_RIGHT);
            mSearchBox.getChildren().addAll(searchLabel, getSearchField());
        }

        return mSearchBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
        }

        return mSearchField;
    }

    private TableView<Channel> getChannelTableView()
    {
        if(mChannelTableView == null)
        {
            mChannelTableView = new TableView<>();

            TableColumn systemColumn = new TableColumn();
            systemColumn.setText("System");
            systemColumn.setCellValueFactory(new PropertyValueFactory<>("system"));
            systemColumn.setPrefWidth(175);

            TableColumn siteColumn = new TableColumn();
            siteColumn.setText("Site");
            siteColumn.setCellValueFactory(new PropertyValueFactory<>("site"));
            siteColumn.setPrefWidth(175);

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Name");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setPrefWidth(400);

            TableColumn protocolColumn = new TableColumn();
            protocolColumn.setText("Protocol");
            protocolColumn.setCellValueFactory(new ProtocolCellValueFactory());
            protocolColumn.setPrefWidth(100);

            mChannelTableView.getColumns().addAll(systemColumn, siteColumn, nameColumn, protocolColumn);
            mChannelTableView.setPlaceholder(getPlaceholderLabel());

            //Sorting and filtering for the table
            FilteredList<Channel> filteredList = new FilteredList<>(mPlaylistManager.getChannelModel().channelList(),
                p -> true);

            getSearchField().textProperty().addListener(new ChangeListener<String>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
                {
                    filteredList.setPredicate(channel -> {
                        if(newValue == null || newValue.isEmpty())
                        {
                            return true;
                        }

                        String filterText = newValue.toLowerCase();

                        if(channel.getSystem() != null && channel.getSystem().toLowerCase().contains(filterText))
                        {
                            return true;
                        }
                        else if(channel.getSite() != null && channel.getSite().toLowerCase().contains(filterText))
                        {
                            return true;
                        }
                        else if(channel.getName() != null && channel.getName().toLowerCase().contains(filterText))
                        {
                            return true;
                        }
                        else if(channel.getDecodeConfiguration().getDecoderType().getDisplayString().toLowerCase().contains(filterText))
                        {
                            return true;
                        }

                        return false;
                    });
                }
            });

            SortedList<Channel> sortedList = new SortedList<>(filteredList);

            sortedList.comparatorProperty().bind(mChannelTableView.comparatorProperty());

            mChannelTableView.setItems(sortedList);

            mChannelTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setChannel(newValue));
        }

        return mChannelTableView;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("No Channel Configurations Available");
        }

        return mPlaceholderLabel;
    }

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setPadding(new Insets(0, 5, 5, 5));
            mButtonBox.setSpacing(10);
            mButtonBox.getChildren().addAll(getNewButton(), getCloneButton(), getDeleteButton());
        }

        return mButtonBox;
    }

    private MenuButton getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new MenuButton("New");
            mNewButton.setAlignment(Pos.CENTER);
            mNewButton.setMaxWidth(Double.MAX_VALUE);

            for(DecoderType decoderType: DecoderType.PRIMARY_DECODERS)
            {
                mNewButton.getItems().add(new NewChannelMenuItem(decoderType));
            }
        }

        return mNewButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setDisable(true);
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                Channel selected = getChannelTableView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete the selected channel?", ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Delete Channel");
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteButton()).getScene().getWindow());

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        mPlaylistManager.getChannelModel().removeChannel(selected);
                    }
                }
            });
        }

        return mDeleteButton;
    }

    private Button getCloneButton()
    {
        if(mCloneButton == null)
        {
            mCloneButton = new Button("Clone");
            mCloneButton.setDisable(true);
            mCloneButton.setMaxWidth(Double.MAX_VALUE);
            mCloneButton.setOnAction(event -> {
                Channel selected = getChannelTableView().getSelectionModel().getSelectedItem();
                Channel copy = selected.copyOf();
                mPlaylistManager.getChannelModel().addChannel(copy);
                getChannelTableView().getSelectionModel().select(copy);
            });
        }

        return mCloneButton;
    }

    /**
     * Menu item for creating a new channel for a specific decoder type
     */
    public class NewChannelMenuItem extends MenuItem
    {
        private DecoderType mDecoderType;

        /**
         * Constructs an instance
         * @param decoderType to use for the decoder configuration for the channel.
         */
        public NewChannelMenuItem(DecoderType decoderType)
        {
            setText(decoderType.getDisplayString());
            mDecoderType = decoderType;
            setOnAction(event -> createNewChannel(mDecoderType));
        }
    }

    public class ProtocolCellValueFactory implements Callback<TableColumn.CellDataFeatures<Channel, String>,
        ObservableValue<String>>
    {
        private SimpleStringProperty mProtocol = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Channel, String> param)
        {
            Channel channel = param.getValue();

            if(channel != null)
            {
                mProtocol.set(channel.getDecodeConfiguration().getDecoderType().getDisplayString());
            }
            else
            {
                mProtocol.set(null);
            }

            return mProtocol;
        }
    }
}
