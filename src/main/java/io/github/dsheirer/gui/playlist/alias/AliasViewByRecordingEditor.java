/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

public class AliasViewByRecordingEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasViewByRecordingEditor.class);

    private PlaylistManager mPlaylistManager;
    private ComboBox<String> mAliasListNameComboBox;
    private HBox mTopBox;
    private TextField mSearchField;
    private TableView<Alias> mNoRecordAliasTableView;
    private TableView<Alias> mRecordAliasTableView;
    private Button mAddButton;
    private Button mAddAllButton;
    private Button mRemoveButton;
    private Button mRemoveAllButton;
    private FilteredList<Alias> mNoRecordFilteredList;
    private FilteredList<Alias> mRecordFilteredList;
    private AvailablePredicate mNoRecordPredicate;
    private SelectedPredicate mRecordPredicate;

    public AliasViewByRecordingEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        VBox buttonBox = new VBox();
        buttonBox.setMaxHeight(Double.MAX_VALUE);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(5);
        buttonBox.getChildren().addAll(getAddAllButton(), getAddButton(), getRemoveButton(), getRemoveAllButton());

        VBox availableBox = new VBox();
        availableBox.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(getNoRecordAliasTableView(), Priority.ALWAYS);
        availableBox.getChildren().addAll(new Label("Don't Record Audio"), getNoRecordAliasTableView());

        VBox selectedBox = new VBox();
        selectedBox.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(getRecordAliasTableView(), Priority.ALWAYS);
        selectedBox.getChildren().addAll(new Label("Record Audio"), getRecordAliasTableView());

        HBox listsBox = new HBox();
        listsBox.setMaxHeight(Double.MAX_VALUE);
        listsBox.setPadding(new Insets(0,10,10,10));
        listsBox.setSpacing(10);
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        HBox.setHgrow(selectedBox, Priority.ALWAYS);
        listsBox.getChildren().addAll(availableBox, buttonBox, selectedBox);

        VBox.setVgrow(listsBox, Priority.ALWAYS);

        getChildren().addAll(getTopBox(), listsBox);
    }

    private ComboBox<String> getAliasListNameComboBox()
    {
        if(mAliasListNameComboBox == null)
        {
            Predicate<String> filterPredicate = s -> !s.contentEquals(AliasModel.NO_ALIAS_LIST);
            FilteredList<String> filteredChannelList =
                new FilteredList<>(mPlaylistManager.getAliasModel().aliasListNames(), filterPredicate);
            mAliasListNameComboBox = new ComboBox<>(filteredChannelList);
            mAliasListNameComboBox.setPadding(new Insets(0,10,0,0));
            mAliasListNameComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateListFilters();
                });
            if(mAliasListNameComboBox.getItems().size() > 0)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }
        }

        return mAliasListNameComboBox;
    }

    private FilteredList<Alias> getNoRecordFilteredList()
    {
        if(mNoRecordFilteredList == null)
        {
            mNoRecordFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(),
                getNoRecordPredicate());
        }

        return mNoRecordFilteredList;
    }

    private FilteredList<Alias> getRecordFilteredList()
    {
        if(mRecordFilteredList == null)
        {
            mRecordFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(),
                getRecordPredicate());
        }

        return mRecordFilteredList;
    }

    private HBox getTopBox()
    {
        if(mTopBox == null)
        {
            mTopBox = new HBox();
            mTopBox.setAlignment(Pos.CENTER_LEFT);
            mTopBox.setPadding(new Insets(10, 10, 10, 10));
            mTopBox.setSpacing(15);

            HBox aliasListBox = new HBox();
            aliasListBox.setAlignment(Pos.CENTER);
            aliasListBox.setSpacing(5);
            Label aliasListLabel = new Label("Alias List");
            aliasListBox.getChildren().addAll(aliasListLabel, getAliasListNameComboBox());

            HBox searchBox = new HBox();
            searchBox.setAlignment(Pos.CENTER);
            searchBox.setSpacing(5);
            Label searchLabel = new Label("Search");
            searchBox.getChildren().addAll(searchLabel, getSearchField());

            mTopBox.getChildren().addAll(aliasListBox, searchBox);
        }

        return mTopBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                updateListFilters();
            });
        }

        return mSearchField;
    }

    /**
     * Updates the filters applied to the available and selected alias lists
     */
    private void updateListFilters()
    {
        String aliasList = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
        String filterText = getSearchField().getText();

        mNoRecordPredicate.setAliasListName(aliasList);
        mNoRecordPredicate.setFilterText(filterText);
        getNoRecordFilteredList().setPredicate(null);
        getNoRecordFilteredList().setPredicate(mNoRecordPredicate);

        mRecordPredicate.setAliasListName(aliasList);
        mRecordPredicate.setFilterText(filterText);
        getRecordFilteredList().setPredicate(null);
        getRecordFilteredList().setPredicate(mRecordPredicate);
    }

    private AvailablePredicate getNoRecordPredicate()
    {
        if(mNoRecordPredicate == null)
        {
            mNoRecordPredicate = new AvailablePredicate();
        }

        return mNoRecordPredicate;
    }

    private SelectedPredicate getRecordPredicate()
    {
        if(mRecordPredicate == null)
        {
            mRecordPredicate = new SelectedPredicate();
        }

        return mRecordPredicate;
    }

    private TableView<Alias> getNoRecordAliasTableView()
    {
        if(mNoRecordAliasTableView == null)
        {
            mNoRecordAliasTableView = new TableView<>();
            mNoRecordAliasTableView.setMaxHeight(Double.MAX_VALUE);
            mNoRecordAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setPrefWidth(200);
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias,String>("name"));

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setPrefWidth(200);
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));

            mNoRecordAliasTableView.getColumns().addAll(nameColumn, groupColumn);
            mNoRecordAliasTableView.setPlaceholder(new Label("No non-recordable aliases available"));
            mNoRecordAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                int selectedCount = mNoRecordAliasTableView.getSelectionModel().getSelectedItems().size();
                getAddButton().setDisable(selectedCount != 1);
                getAddAllButton().setDisable(selectedCount < 2);
                if(selectedCount > 0)
                {
                    getRecordAliasTableView().getSelectionModel().clearSelection();
                }
            });

            SortedList<Alias> sortedList = new SortedList<>(getNoRecordFilteredList());
            sortedList.comparatorProperty().bind(mNoRecordAliasTableView.comparatorProperty());
            mNoRecordAliasTableView.setItems(sortedList);
        }

        return mNoRecordAliasTableView;
    }

    private TableView<Alias> getRecordAliasTableView()
    {
        if(mRecordAliasTableView == null)
        {
            mRecordAliasTableView = new TableView<>();
            mRecordAliasTableView.setMaxHeight(Double.MAX_VALUE);
            mRecordAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setPrefWidth(200);
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias,String>("name"));

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setPrefWidth(200);
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));

            mRecordAliasTableView.getColumns().addAll(nameColumn, groupColumn);
            mRecordAliasTableView.setPlaceholder(new Label("No recordable aliases available"));
            mRecordAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                int selectedCount = mRecordAliasTableView.getSelectionModel().getSelectedItems().size();
                getRemoveButton().setDisable(selectedCount != 1);
                getRemoveAllButton().setDisable(selectedCount < 2);
                if(selectedCount > 0)
                {
                    getNoRecordAliasTableView().getSelectionModel().clearSelection();
                }
            });

            SortedList<Alias> sortedList = new SortedList<>(getRecordFilteredList());
            sortedList.comparatorProperty().bind(mRecordAliasTableView.comparatorProperty());
            mRecordAliasTableView.setItems(sortedList);
        }

        return mRecordAliasTableView;
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
                Alias selectedAlias = getNoRecordAliasTableView().getSelectionModel().getSelectedItem();

                if(selectedAlias != null)
                {
                    selectedAlias.setRecordable(true);
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
            mAddAllButton.setOnAction(event -> {
                List<Alias> selectedAliases =
                    new ArrayList(getNoRecordAliasTableView().getSelectionModel().getSelectedItems());

                if(!selectedAliases.isEmpty())
                {
                    for(Alias selectedAlias: selectedAliases)
                    {
                        selectedAlias.setRecordable(true);
                    }

                    updateListFilters();
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
                Alias selectedAlias = getRecordAliasTableView().getSelectionModel().getSelectedItem();

                if(selectedAlias != null)
                {
                    selectedAlias.setRecordable(false);
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
                    new ArrayList(getRecordAliasTableView().getSelectionModel().getSelectedItems());

                if(!selectedAliases.isEmpty())
                {
                    for(Alias selectedAlias: selectedAliases)
                    {
                        selectedAlias.setRecordable(false);
                    }

                    updateListFilters();
                }
            });
        }

        return mRemoveAllButton;
    }

    public static class AvailablePredicate implements Predicate<Alias>
    {
        private String mAliasListName;
        private String mFilterText;

        public AvailablePredicate()
        {
        }

        public void setAliasListName(String aliasListName)
        {
            mAliasListName = aliasListName;
        }

        public void setFilterText(String filterText)
        {
            mFilterText = (filterText == null || filterText.isEmpty()) ? null : filterText.toLowerCase();
        }

        @Override
        public boolean test(Alias alias)
        {
            if(mAliasListName == null || alias.isRecordable() || alias.getAliasListName() == null)
            {
                return false;
            }

            if(mAliasListName.contentEquals(alias.getAliasListName()))
            {
                if(mFilterText == null)
                {
                    return true;
                }
                else
                {
                    if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mFilterText))
                    {
                        return true;
                    }

                    if(alias.getName() != null && alias.getName().toLowerCase().contains(mFilterText))
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static class SelectedPredicate implements Predicate<Alias>
    {
        private String mAliasListName;
        private String mFilterText;

        public SelectedPredicate()
        {
        }

        public void setAliasListName(String aliasListName)
        {
            mAliasListName = aliasListName;
        }

        public void setFilterText(String filterText)
        {
            mFilterText = (filterText == null || filterText.isEmpty()) ? null : filterText.toLowerCase();
        }

        @Override
        public boolean test(Alias alias)
        {
            if(mAliasListName == null || !alias.isRecordable() || alias.getAliasListName() == null)
            {
                return false;
            }

            if(mAliasListName.contentEquals(alias.getAliasListName()))
            {
                if(mFilterText == null)
                {
                    return true;
                }
                else
                {
                    if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mFilterText))
                    {
                        return true;
                    }

                    if(alias.getName() != null && alias.getName().toLowerCase().contains(mFilterText))
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
