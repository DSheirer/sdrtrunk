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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Editor for managing which aliases are configured for a specific broadcast configuration audio stream
 */
public class StreamAliasSelectionEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(StreamAliasSelectionEditor.class);
    private PlaylistManager mPlaylistManager;
    private BroadcastConfiguration mSelectedBroadcastConfiguration;
    private HBox mSearchBox;
    private TextField mSearchField;
    private TableView<Alias> mAvailableAliasTableView;
    private TableView<Alias> mSelectedAliasTableView;
    private Button mAddButton;
    private Button mAddAllButton;
    private Button mRemoveButton;
    private Button mRemoveAllButton;
    private FilteredList<Alias> mAvailableFilteredList;
    private FilteredList<Alias> mSelectedFilteredList;
    private AvailableAliasPredicate mAvailableAliasPredicate = new AvailableAliasPredicate();
    private SelectedAliasPredicate mSelectedAliasPredicate = new SelectedAliasPredicate();

    /**
     * Constructs an instance
     * @param playlistManager for access to alias model
     */
    public StreamAliasSelectionEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        VBox buttonBox = new VBox();
        buttonBox.setMaxHeight(Double.MAX_VALUE);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(5);
        buttonBox.getChildren().addAll(getAddAllButton(), getAddButton(), getRemoveButton(), getRemoveAllButton());

        VBox availableBox = new VBox();
        availableBox.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(getAvailableAliasTableView(), Priority.ALWAYS);
        availableBox.getChildren().addAll(new Label("Available"), getAvailableAliasTableView());

        VBox selectedBox = new VBox();
        selectedBox.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(getSelectedAliasTableView(), Priority.ALWAYS);
        selectedBox.getChildren().addAll(new Label("Selected"), getSelectedAliasTableView());

        HBox listsBox = new HBox();
        listsBox.setMaxHeight(Double.MAX_VALUE);
        listsBox.setPadding(new Insets(5,10,10,10));
        listsBox.setSpacing(10);
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        HBox.setHgrow(selectedBox, Priority.ALWAYS);
        listsBox.getChildren().addAll(availableBox, buttonBox, selectedBox);

        VBox.setVgrow(listsBox, Priority.ALWAYS);

        getChildren().addAll(getSearchBox(), listsBox);
    }

    public void setBroadcastConfiguration(BroadcastConfiguration broadcastConfiguration)
    {
        mSearchField.setText(null);
        mSelectedBroadcastConfiguration = broadcastConfiguration;
        getSearchField().setDisable(mSelectedBroadcastConfiguration == null);
        getAvailableAliasTableView().setDisable(mSelectedBroadcastConfiguration == null);
        getSelectedAliasTableView().setDisable(mSelectedBroadcastConfiguration == null);
        updateListFilters();
    }

    private FilteredList<Alias> getAvailableFilteredList()
    {
        if(mAvailableFilteredList == null)
        {
            mAvailableFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(),
                mAvailableAliasPredicate);
        }

        return mAvailableFilteredList;
    }

    private FilteredList<Alias> getSelectedFilteredList()
    {
        if(mSelectedFilteredList == null)
        {
            mSelectedFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(),
                mSelectedAliasPredicate);
        }

        return mSelectedFilteredList;
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
            mSearchField.setDisable(true);
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                updateListFilters();
            });
        }

        return mSearchField;
    }

    /**
     * Updates the predicate filters applied to the available and selected alias lists
     */
    private void updateListFilters()
    {
        String streamName = getSelectedStreamName();
        String filterText = getSearchField().getText();

        mAvailableAliasPredicate.setStreamName(streamName);
        mAvailableAliasPredicate.setFilterText(filterText);
        mSelectedAliasPredicate.setStreamName(streamName);
        mSelectedAliasPredicate.setFilterText(filterText);

        getAvailableFilteredList().setPredicate(null);
        getAvailableFilteredList().setPredicate(mAvailableAliasPredicate);
        getSelectedFilteredList().setPredicate(null);
        getSelectedFilteredList().setPredicate(mSelectedAliasPredicate);
    }

    /**
     * Selected stream name or null
     */
    private String getSelectedStreamName()
    {
        if(mSelectedBroadcastConfiguration != null && mSelectedBroadcastConfiguration.getName() != null &&
            !mSelectedBroadcastConfiguration.getName().isEmpty())
        {
            return mSelectedBroadcastConfiguration.getName();
        }

        return null;
    }

    private TableView<Alias> getAvailableAliasTableView()
    {
        if(mAvailableAliasTableView == null)
        {
            mAvailableAliasTableView = new TableView<>();
            mAvailableAliasTableView.setMaxHeight(Double.MAX_VALUE);
            mAvailableAliasTableView.setDisable(true);
            mAvailableAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            TableColumn aliasListNameColumn = new TableColumn();
            aliasListNameColumn.setText("Alias List");
            aliasListNameColumn.setPrefWidth(150);
            aliasListNameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasListName"));

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setPrefWidth(150);
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setPrefWidth(150);
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias,String>("name"));

            mAvailableAliasTableView.getColumns().addAll(aliasListNameColumn, groupColumn, nameColumn);
            mAvailableAliasTableView.setPlaceholder(new Label("No aliases available"));
            mAvailableAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                int selectedCount = mAvailableAliasTableView.getSelectionModel().getSelectedItems().size();
                getAddButton().setDisable(selectedCount != 1);
                getAddAllButton().setDisable(selectedCount < 2);
                if(selectedCount > 0)
                {
                    getSelectedAliasTableView().getSelectionModel().clearSelection();
                }
            });

            SortedList<Alias> sortedList = new SortedList<>(getAvailableFilteredList());
            sortedList.comparatorProperty().bind(mAvailableAliasTableView.comparatorProperty());
            mAvailableAliasTableView.setItems(sortedList);
        }

        return mAvailableAliasTableView;
    }

    private TableView<Alias> getSelectedAliasTableView()
    {
        if(mSelectedAliasTableView == null)
        {
            mSelectedAliasTableView = new TableView<>();
            mSelectedAliasTableView.setMaxHeight(Double.MAX_VALUE);
            mSelectedAliasTableView.setDisable(true);
            mSelectedAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            TableColumn aliasListNameColumn = new TableColumn();
            aliasListNameColumn.setText("Alias List");
            aliasListNameColumn.setPrefWidth(150);
            aliasListNameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasListName"));

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setPrefWidth(150);
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setPrefWidth(150);
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias,String>("name"));

            mSelectedAliasTableView.getColumns().addAll(aliasListNameColumn, groupColumn, nameColumn);
            mSelectedAliasTableView.setPlaceholder(new Label("No aliases selected"));
            mSelectedAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                int selectedCount = mSelectedAliasTableView.getSelectionModel().getSelectedItems().size();
                getRemoveButton().setDisable(selectedCount != 1);
                getRemoveAllButton().setDisable(selectedCount < 2);
                if(selectedCount > 0)
                {
                    getAvailableAliasTableView().getSelectionModel().clearSelection();
                }
            });

            SortedList<Alias> sortedList = new SortedList<>(getSelectedFilteredList());
            sortedList.comparatorProperty().bind(mSelectedAliasTableView.comparatorProperty());
            mSelectedAliasTableView.setItems(sortedList);
        }

        return mSelectedAliasTableView;
    }

    private Button getAddButton()
    {
        if(mAddButton == null)
        {
            mAddButton = new Button();
            mAddButton.setDisable(true);
            mAddButton.setMaxWidth(Double.MAX_VALUE);
            mAddButton.setGraphic(new IconNode(FontAwesome.ANGLE_RIGHT));
            mAddButton.setAlignment(Pos.CENTER);
            mAddButton.setOnAction(event -> {
                Alias selectedAlias = getAvailableAliasTableView().getSelectionModel().getSelectedItem();
                String stream = getSelectedStreamName();

                if(selectedAlias != null && stream != null)
                {
                    selectedAlias.addAliasID(new BroadcastChannel(stream));
                    updateListFilters();
                }
            });
        }

        return mAddButton;
    }

    private Button getAddAllButton()
    {
        if(mAddAllButton == null)
        {
            mAddAllButton = new Button();
            mAddAllButton.setDisable(true);
            mAddAllButton.setMaxWidth(Double.MAX_VALUE);
            mAddAllButton.setGraphic(new IconNode(FontAwesome.ANGLE_DOUBLE_RIGHT));
            mAddAllButton.setAlignment(Pos.CENTER);
            mAddAllButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    List<Alias> selectedAliases =
                        new ArrayList(getAvailableAliasTableView().getSelectionModel().getSelectedItems());
                    String stream = getSelectedStreamName();

                    if(!selectedAliases.isEmpty() && stream != null)
                    {
                        for(Alias selectedAlias: selectedAliases)
                        {
                            selectedAlias.addAliasID(new BroadcastChannel(stream));
                        }

                        updateListFilters();
                    }
                }
            });
        }

        return mAddAllButton;
    }

    private Button getRemoveButton()
    {
        if(mRemoveButton == null)
        {
            mRemoveButton = new Button();
            mRemoveButton.setDisable(true);
            mRemoveButton.setMaxWidth(Double.MAX_VALUE);
            mRemoveButton.setGraphic(new IconNode(FontAwesome.ANGLE_LEFT));
            mRemoveButton.setAlignment(Pos.CENTER);
            mRemoveButton.setOnAction(event -> {
                Alias selectedAlias = getSelectedAliasTableView().getSelectionModel().getSelectedItem();
                String stream = getSelectedStreamName();

                if(selectedAlias != null && stream != null)
                {
                    selectedAlias.removeBroadcastChannel(stream);
                    updateListFilters();
                }
            });
        }

        return mRemoveButton;
    }

    private Button getRemoveAllButton()
    {
        if(mRemoveAllButton == null)
        {
            mRemoveAllButton = new Button();
            mRemoveAllButton.setDisable(true);
            mRemoveAllButton.setMaxWidth(Double.MAX_VALUE);
            mRemoveAllButton.setGraphic(new IconNode(FontAwesome.ANGLE_DOUBLE_LEFT));
            mRemoveAllButton.setAlignment(Pos.CENTER);
            mRemoveAllButton.setOnAction(event -> {
                List<Alias> selectedAliases =
                    new ArrayList(getSelectedAliasTableView().getSelectionModel().getSelectedItems());
                String stream = getSelectedStreamName();

                if(!selectedAliases.isEmpty() && stream != null)
                {
                    for(Alias selectedAlias: selectedAliases)
                    {
                        selectedAlias.removeBroadcastChannel(stream);
                    }

                    updateListFilters();
                }
            });
        }

        return mRemoveAllButton;
    }

    /**
     * Predicate for filtering aliases by stream name and optional filter text
     */
    public static class AvailableAliasPredicate implements Predicate<Alias>
    {
        private String mStreamName;
        private String mFilterText;

        public AvailableAliasPredicate()
        {
        }

        public void setStreamName(String streamName)
        {
            mStreamName = streamName;
        }

        public void setFilterText(String filterText)
        {
            mFilterText = (filterText == null || filterText.isEmpty()) ? null : filterText.toLowerCase();
        }

        @Override
        public boolean test(Alias alias)
        {
            if(mStreamName == null || alias.hasBroadcastChannel(mStreamName))
            {
                return false;
            }

            if(mFilterText == null)
            {
                return true;
            }

            if(alias.getAliasListName() != null && alias.getAliasListName().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(alias.getName() != null && alias.getName().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            return false;
        }
    }

    /**
     * Predicate for filtering aliases by stream name and optional filter text
     */
    public static class SelectedAliasPredicate implements Predicate<Alias>
    {
        private String mStreamName;
        private String mFilterText;

        public SelectedAliasPredicate()
        {
        }

        public void setStreamName(String streamName)
        {
            mStreamName = streamName;
        }

        public void setFilterText(String filterText)
        {
            mFilterText = (filterText == null || filterText.isEmpty()) ? null : filterText.toLowerCase();
        }

        @Override
        public boolean test(Alias alias)
        {
            if(!alias.hasBroadcastChannel(mStreamName))
            {
                return false;
            }
            if(mFilterText == null)
            {
                return true;
            }
            if(alias.getAliasListName() != null && alias.getAliasListName().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mFilterText))
            {
                return true;
            }
            if(alias.getName() != null && alias.getName().toLowerCase().contains(mFilterText))
            {
                return true;
            }

            return false;
        }
    }
}
