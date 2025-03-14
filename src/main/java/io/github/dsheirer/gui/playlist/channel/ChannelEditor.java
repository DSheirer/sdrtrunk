/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.channel;

import com.google.common.base.Joiner;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.playlist.IAliasListRefreshListener;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX editor for managing channel configurations.
 */
public class ChannelEditor extends SplitPane implements IFilterProcessor, IAliasListRefreshListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelEditor.class);
    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private TableView<Channel> mChannelTableView;
    private Label mPlaceholderLabel;
    private MenuButton mNewButton;
    private Button mDeleteButton;
    private Button mCloneButton;
    private VBox mButtonBox;
    private HBox mSearchAndViewBox;
    private TextField mSearchField;
    private SegmentedButton mViewSegmentedButton;
    private ToggleButton mAllToggleButton;
    private ToggleButton mAutoStartToggleButton;
    private ToggleButton mPlayingToggleButton;
    private ChannelConfigurationEditor mChannelConfigurationEditor;
    private UnknownConfigurationEditor mUnknownConfigurationEditor;
    private Map<DecoderType,ChannelConfigurationEditor> mChannelConfigurationEditorMap = new HashMap();
    private FilteredList<Channel> mChannelFilteredList;
    private ChannelListFilter mChannelListFilter = new ChannelListFilter();

    /**
     * Constructs an instance
     * @param playlistManager containing playlists and channel configurations
     */
    public ChannelEditor(PlaylistManager playlistManager, TunerManager tunerManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mPlaylistManager.addAliasListRefreshListener(this);
        mTunerManager = tunerManager;
        mUserPreferences = userPreferences;
        mUnknownConfigurationEditor = new UnknownConfigurationEditor(mPlaylistManager, mTunerManager,
                userPreferences, this);

        HBox channelsBox = new HBox();
        channelsBox.setSpacing(10.0);
        HBox.setHgrow(getChannelTableView(), Priority.ALWAYS);
        channelsBox.getChildren().addAll(getChannelTableView(), getButtonBox());

        VBox topBox = new VBox();
        topBox.setPadding(new Insets(10,10,10,10));
        topBox.setSpacing(10);
        VBox.setVgrow(channelsBox, Priority.ALWAYS);
        topBox.getChildren().addAll(getSearchAndViewBox(), channelsBox);

        setOrientation(Orientation.VERTICAL);
        getItems().addAll(topBox, getChannelConfigurationEditor());
    }

    /**
     * Prepares for an alias list refresh by clearing the currently selected channel from the channel table.
     */
    @Override
    public void prepareForAliasListRefresh()
    {
        getChannelTableView().getSelectionModel().select(null);
    }

    /**
     * Processes the channel view request.
     *
     * Note: this method must be invoked on the JavaFX platform thread
     *
     * @param channelTabRequest to process
     */
    public void process(ChannelTabRequest channelTabRequest)
    {
        if(channelTabRequest instanceof ViewChannelRequest)
        {
            Channel channel = ((ViewChannelRequest)channelTabRequest).getChannel();

            if(channel != null)
            {
                getSearchField().setText(null);
                getChannelTableView().getSelectionModel().select(channel);
                getChannelTableView().scrollTo(channel);
            }
        }
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
                        editor = ChannelConfigurationEditorFactory.getEditor(channelDecoderType, mPlaylistManager,
                            mTunerManager, mUserPreferences, this);

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
        getChannelTableView().scrollTo(channel);
    }

    private SegmentedButton getViewSegmentedButton()
    {
        if(mViewSegmentedButton == null)
        {
            mViewSegmentedButton = new SegmentedButton(getAllToggleButton(), getPlayingToggleButton(),
                getAutoStartToggleButton());
            mViewSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            getAllToggleButton().setSelected(true);
            mViewSegmentedButton.getToggleGroup().selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> {
                //Don't allow toggles to be de-selected
                if(newValue == null)
                {
                    oldValue.setSelected(true);
                }
                else
                {
                    updateChannelListFilter();
                }
            });
        }

        return mViewSegmentedButton;
    }

    private ToggleButton getAllToggleButton()
    {
        if(mAllToggleButton == null)
        {
            mAllToggleButton = new ToggleButton("All");
        }

        return mAllToggleButton;
    }

    private ToggleButton getAutoStartToggleButton()
    {
        if(mAutoStartToggleButton == null)
        {
            mAutoStartToggleButton = new ToggleButton("Auto-Start");
        }

        return mAutoStartToggleButton;
    }

    private ToggleButton getPlayingToggleButton()
    {
        if(mPlayingToggleButton == null)
        {
            mPlayingToggleButton = new ToggleButton("Playing");
        }

        return mPlayingToggleButton;
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

    private HBox getSearchAndViewBox()
    {
        if(mSearchAndViewBox == null)
        {
            mSearchAndViewBox = new HBox();
            mSearchAndViewBox.setAlignment(Pos.CENTER_LEFT);
            mSearchAndViewBox.setSpacing(10);

            HBox searchBox = new HBox();
            searchBox.setSpacing(5);
            searchBox.setAlignment(Pos.CENTER);
            Label searchLabel = new Label("Search:");
            searchLabel.setAlignment(Pos.CENTER_RIGHT);
            searchBox.getChildren().addAll(searchLabel, getSearchField());

            HBox viewBox = new HBox();
            viewBox.setSpacing(5);
            viewBox.setAlignment(Pos.CENTER);
            Label viewLabel = new Label("View Channels:");
            viewBox.getChildren().addAll(viewLabel, getViewSegmentedButton());

            mSearchAndViewBox.getChildren().addAll(searchBox, viewBox);
        }

        return mSearchAndViewBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> updateChannelListFilter());
        }

        return mSearchField;
    }

    /**
     * Updates the predicate on the channel list to apply search string and view settings
     */
    private void updateChannelListFilter()
    {
        mChannelListFilter.setFilterText(getSearchField().getText());

        if(getAllToggleButton().isSelected())
        {
            mChannelListFilter.setView(ChannelListFilter.View.ALL);
        }
        else if(getAutoStartToggleButton().isSelected())
        {
            mChannelListFilter.setView(ChannelListFilter.View.AUTO_START);
        }
        else if(getPlayingToggleButton().isSelected())
        {
            mChannelListFilter.setView(ChannelListFilter.View.PLAYING);
        }

        mChannelFilteredList.setPredicate(null);
        mChannelFilteredList.setPredicate(mChannelListFilter);
    }

    /**
     * Temporarily clear the filter when the channel configuration editor is applying changes.
     */
    @Override
    public void clearFilter()
    {
        mChannelFilteredList.setPredicate(null);
    }

    /**
     * Restore the filter once the channel configuration editor has completed applying changes.
     */
    @Override
    public void restoreFilter()
    {
        mChannelFilteredList.setPredicate(mChannelListFilter);
    }

    private TableView<Channel> getChannelTableView()
    {
        if(mChannelTableView == null)
        {
            mChannelTableView = new TableView<>();

            TableColumn<Channel,Boolean> playingColumn = new TableColumn("Playing");
            playingColumn.setPrefWidth(75);
            playingColumn.setCellValueFactory(new PropertyValueFactory<>("processing"));
            playingColumn.setCellFactory(param -> {
                TableCell<Channel,Boolean> tableCell = new TableCell<>()
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

            TableColumn<Channel,Boolean> autoStartColumn = new TableColumn<>("Auto-Start");
            autoStartColumn.setCellValueFactory(new PropertyValueFactory<>("autoStart"));
            autoStartColumn.setPrefWidth(95);
            autoStartColumn.setCellFactory(param -> {
                TableCell<Channel,Boolean> tableCell = new TableCell<>()
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

            TableColumn systemColumn = new TableColumn("System");
            systemColumn.setCellValueFactory(new PropertyValueFactory<>("system"));
            systemColumn.setPrefWidth(175);

            TableColumn siteColumn = new TableColumn("Site");
            siteColumn.setCellValueFactory(new PropertyValueFactory<>("site"));
            siteColumn.setPrefWidth(175);

            TableColumn nameColumn = new TableColumn("Name");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setPrefWidth(200);

            TableColumn frequencyColumn = new TableColumn("Frequency");
            frequencyColumn.setCellValueFactory(new FrequencyCellValueFactory());
            frequencyColumn.setPrefWidth(100);

            TableColumn protocolColumn = new TableColumn("Protocol");
            protocolColumn.setCellValueFactory(new ProtocolCellValueFactory());
            protocolColumn.setPrefWidth(100);

            mChannelTableView.getColumns().addAll(systemColumn, siteColumn, nameColumn, frequencyColumn, protocolColumn,
                playingColumn, autoStartColumn);
            mChannelTableView.setPlaceholder(getPlaceholderLabel());

            //Sorting and filtering for the table
            mChannelFilteredList = new FilteredList<>(mPlaylistManager.getChannelModel().channelList(), mChannelListFilter);
            SortedList<Channel> sortedList = new SortedList<>(mChannelFilteredList);
            sortedList.comparatorProperty().bind(mChannelTableView.comparatorProperty());
            mChannelTableView.setItems(sortedList);
            mChannelTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setChannel(newValue));
            mChannelTableView.setOnMouseClicked(event -> {
                if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2)
                {
                    getChannelConfigurationEditor().startChannel();
                }
            });
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

            MenuItem decodersItem = new MenuItem("Decoder");
            decodersItem.setDisable(true);
            mNewButton.getItems().addAll(decodersItem, new SeparatorMenuItem());

            for(DecoderType decoderType: DecoderType.PRIMARY_DECODERS)
            {
                if(decoderType == DecoderType.P25_PHASE2)
                {
                    mNewButton.getItems().add(new NewP25P2ChannelMenu());
                }
                else
                {
                    mNewButton.getItems().add(new NewChannelMenuItem(decoderType));
                }
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
                        if(selected.isProcessing())
                        {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().stop(selected);
                            }
                            catch(Exception e)
                            {
                                mLog.error("Couldn't stop channel [" + selected.getName() + "] prior to delete by user");
                            }
                        }

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

    /**
     * Menu item for creating new P25 Phase 2 channel configurations
     */
    public class NewP25P2ChannelMenu extends Menu
    {
        public NewP25P2ChannelMenu()
        {
            setText(DecoderType.P25_PHASE2.getDisplayString());
            MenuItem trunkedP1 = new MenuItem("Trunked System - FDMA Phase 1 Control Channel");
            trunkedP1.setOnAction(event -> createNewChannel(DecoderType.P25_PHASE1));
            MenuItem trunkedP2 = new MenuItem("Trunked System - TDMA Phase 2 Control Channel");
            trunkedP2.setOnAction(event -> createNewChannel(DecoderType.P25_PHASE2));
            MenuItem channel = new MenuItem("Individual Phase 2 Channel");
            channel.setOnAction(event -> createNewChannel(DecoderType.P25_PHASE2));
            getItems().addAll(trunkedP1, trunkedP2, channel);
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


    /**
     * Channel tuner channel source frequencies value factory
     */
    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<Channel, String>,
        ObservableValue<String>>
    {
        private SimpleStringProperty mFrequency = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Channel, String> param)
        {
            ObservableList<Long> frequencies = param.getValue().getFrequencyList();

            if(frequencies != null)
            {
                List<String> freqsMHz = new ArrayList<>();
                for(Long frequency: frequencies)
                {
                    freqsMHz.add(String.valueOf(frequency / 1E6));
                }

                mFrequency.set(Joiner.on(", ").join(freqsMHz));
            }
            else
            {
                mFrequency.set(null);
            }

            return mFrequency;
        }
    }

    /**
     * Filter predicate for a filtered channel list
     */
    public static class ChannelListFilter implements Predicate<Channel>
    {
        public enum View{ALL,AUTO_START,PLAYING};

        private String mFilterText;
        private View mView = View.ALL;

        /**
         * Sets the filter text to search for
         */
        public void setFilterText(String filterText)
        {
            if(filterText == null || filterText.trim().isEmpty())
            {
                mFilterText = null;
            }
            else
            {
                mFilterText = filterText.toLowerCase();
            }
        }

        /**
         * Sets the channel view
         */
        public void setView(View view)
        {
            mView = view;
        }

        /**
         * Tests the channel to see if it matches the current view setting and the search filter text
         */
        @Override
        public boolean test(Channel channel)
        {
            switch(mView)
            {
                case ALL:
                default:
                    return matchesFilter(channel);
                case AUTO_START:
                    return channel.isAutoStart() && matchesFilter(channel);
                case PLAYING:
                    return channel.isProcessing() && matchesFilter(channel);
            }
        }

        /**
         * Tests the channel to see if the system, site or name values match the filter text
         */
        private boolean matchesFilter(Channel channel)
        {
            if(mFilterText == null)
            {
                return true;
            }

            if(channel.getName() != null && channel.getName().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(channel.getSite() != null && channel.getSite().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(channel.getSystem() != null && channel.getSystem().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(channel.getDecodeConfiguration().getDecoderType().toString().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            return false;
        }
    }
}
