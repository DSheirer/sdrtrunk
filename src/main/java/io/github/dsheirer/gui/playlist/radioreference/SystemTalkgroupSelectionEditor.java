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

package io.github.dsheirer.gui.playlist.radioreference;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.Talkgroup;
import io.github.dsheirer.rrapi.type.TalkgroupCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.animation.RotateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemTalkgroupSelectionEditor extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(SystemTalkgroupSelectionEditor.class);

    private final TalkgroupCategory ALL_TALKGROUPS = new TalkgroupCategory();
    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private TableView<AliasedTalkgroup> mTalkgroupTableView;
    private ComboBox<TalkgroupCategory> mTalkgroupCategoryComboBox;
    private TextField mSearchField;
    private TalkgroupEditor mTalkgroupEditor;
    private ComboBox<String> mAliasListNameComboBox;
    private Button mNewAliasListButton;
    private TalkgroupFilter mTalkgroupFilter = new TalkgroupFilter();
    private FilteredList<AliasedTalkgroup> mTalkgroupFilteredList;
    private ObservableList<AliasedTalkgroup> mTalkgroupList = FXCollections.observableArrayList();
    private System mCurrentSystem;
    private RadioReferenceDecoder mRadioReferenceDecoder;
    private AliasList mAliasList;
    private AliasListChangeListener mAliasListChangeListener = new AliasListChangeListener();
    private Button mImportAllTalkgroupsButton;
    private Label mPlaceholderLabel;
    private ProgressIndicator mProgressIndicator;
    private CheckBox mEncryptedAsDoNotMonitorCheckBox;

    public SystemTalkgroupSelectionEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        //Register to receive flash alias box requests
        MyEventBus.getGlobalEventBus().register(this);

        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;

        ALL_TALKGROUPS.setName("(All Talkgroups)");

        setPadding(new Insets(10,0,0,0));
        setVgap(10);
        setHgap(10);
        setMaxHeight(Double.MAX_VALUE);

        int row = 0;

        ColumnConstraints column1 = new ColumnConstraints();
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(40);
        getColumnConstraints().addAll(column1, column2);

        HBox listBox = new HBox();
        listBox.setSpacing(5);
        listBox.setAlignment(Pos.CENTER);
        Label importLabel = new Label("Import To Alias List:");
        listBox.getChildren().addAll(importLabel, getAliasListNameComboBox(), getNewAliasListButton());
        GridPane.setConstraints(listBox, 0, row);
        getChildren().add(listBox);

        GridPane.setConstraints(getEncryptedAsDoNotMonitorCheckBox(), 1, row);
        GridPane.setHalignment(getEncryptedAsDoNotMonitorCheckBox(), HPos.CENTER);
        getChildren().add(getEncryptedAsDoNotMonitorCheckBox());

        HBox searchBox = new HBox();
        searchBox.setSpacing(5);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getChildren().addAll(new Label("Search"), getSearchField());
        GridPane.setConstraints(searchBox, 0, ++row);
        getChildren().add(searchBox);

        GridPane.setHalignment(getImportAllTalkgroupsButton(), HPos.CENTER);
        GridPane.setConstraints(getImportAllTalkgroupsButton(), 1, row);
        getChildren().add(getImportAllTalkgroupsButton());

        HBox categoryBox = new HBox();
        categoryBox.setAlignment(Pos.CENTER_LEFT);
        categoryBox.setSpacing(5);
        HBox.setHgrow(getTalkgroupCategoryComboBox(), Priority.ALWAYS);
        categoryBox.getChildren().addAll(new Label("Category"), getTalkgroupCategoryComboBox());
        GridPane.setHgrow(categoryBox, Priority.ALWAYS);
        GridPane.setConstraints(categoryBox, 0, ++row);
        getChildren().add(categoryBox);

        Separator separator = new Separator(Orientation.HORIZONTAL);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        GridPane.setConstraints(separator, 1, row);
        getChildren().add(separator);

        GridPane.setHgrow(getTalkgroupTableView(), Priority.ALWAYS);
        GridPane.setVgrow(getTalkgroupTableView(), Priority.ALWAYS);
        GridPane.setConstraints(getTalkgroupTableView(), 0, ++row);
        getChildren().add(getTalkgroupTableView());

        GridPane.setHgrow(getTalkgroupEditor(), Priority.ALWAYS);
        GridPane.setVgrow(getTalkgroupEditor(), Priority.ALWAYS);
        GridPane.setConstraints(getTalkgroupEditor(), 1, row);
        getChildren().add(getTalkgroupEditor());
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    public void clear()
    {
        mTalkgroupList.clear();
        getTalkgroupCategoryComboBox().getItems().clear();
    }

    public void clearAndSetLoading()
    {
        clear();
        setLoading(true);
    }

    private CheckBox getEncryptedAsDoNotMonitorCheckBox()
    {
        if(mEncryptedAsDoNotMonitorCheckBox == null)
        {
            mEncryptedAsDoNotMonitorCheckBox = new CheckBox("Set Encrypted Talkgroups To Muted");
            mEncryptedAsDoNotMonitorCheckBox.setDisable(true);
            mEncryptedAsDoNotMonitorCheckBox.selectedProperty().set(mUserPreferences.getRadioReferencePreference()
                .isEncryptedTalkgroupDoNotMonitor());
            mEncryptedAsDoNotMonitorCheckBox.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mUserPreferences.getRadioReferencePreference()
                    .setEncryptedTalkgroupDoNotMonitor(mEncryptedAsDoNotMonitorCheckBox.isSelected()));
        }

        return mEncryptedAsDoNotMonitorCheckBox;
    }

    private void setLoading(boolean loading)
    {
        getTalkgroupTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
    }

    private ProgressIndicator getProgressIndicator()
    {
        if(mProgressIndicator == null)
        {
            mProgressIndicator = new ProgressIndicator();
            mProgressIndicator.setProgress(-1);
        }

        return mProgressIndicator;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("No Talkgroups Available");
        }

        return mPlaceholderLabel;
    }

    private void updateFilter()
    {
        mTalkgroupFilter.setFilterText(getSearchField().getText());
        TalkgroupCategory category = getTalkgroupCategoryComboBox().getSelectionModel().getSelectedItem();

        if(category == ALL_TALKGROUPS)
        {
            mTalkgroupFilter.setCategory(null);
        }
        else
        {
            mTalkgroupFilter.setCategory(category != null ? category.getTalkgroupCategoryId() : null);
        }

        mTalkgroupFilteredList.setPredicate(null);
        mTalkgroupFilteredList.setPredicate(mTalkgroupFilter);
    }

    public void setSystem(System system, List<Talkgroup> talkgroups, List<TalkgroupCategory> categories,
                          RadioReferenceDecoder decoder)
    {
        mCurrentSystem = system;
        mRadioReferenceDecoder = decoder;

        clearAndSetLoading();

        if(talkgroups != null && !talkgroups.isEmpty())
        {
            Collections.sort(talkgroups, Comparator.comparingInt(Talkgroup::getDecimalValue));

            for(Talkgroup talkgroup: talkgroups)
            {
                mTalkgroupList.add(new AliasedTalkgroup(talkgroup, getAlias(talkgroup)));
            }

            if(categories.size() > 0)
            {
                Collections.sort(categories, (o1, o2) -> o1.getName().compareTo(o2.getName()));
                categories.add(0, ALL_TALKGROUPS);
                getTalkgroupCategoryComboBox().getItems().addAll(categories);
                getTalkgroupCategoryComboBox().getSelectionModel().select(ALL_TALKGROUPS);
            }
        }

        Protocol protocol = getRadioReferenceDecoder().getProtocol(getCurrentSystem());

        //If the protocol is supported then enable the talkgroup import controls
        boolean supported = getRadioReferenceDecoder().hasSupportedProtocol(getCurrentSystem());
        getImportAllTalkgroupsButton().setDisable(!supported);
        getEncryptedAsDoNotMonitorCheckBox().setDisable(!supported);
        setLoading(false);
    }

    @Subscribe
    public void process(FlashAliasListComboBoxRequest request)
    {
        flashAliasListComboBox();
    }

    /**
     * Flashes the alias list combobox to let the user know that they must select an alias list
     */
    private void flashAliasListComboBox()
    {
        RotateTransition rt = new RotateTransition(Duration.millis(150), getAliasListNameComboBox());
        rt.setByAngle(20);
        rt.setCycleCount(6);
        rt.setAutoReverse(true);
        rt.play();
    }

    private Button getImportAllTalkgroupsButton()
    {
        if(mImportAllTalkgroupsButton == null)
        {
            mImportAllTalkgroupsButton = new Button("Import All Talkgroups");
            mImportAllTalkgroupsButton.setOnAction(event -> {

                String aliasList = getAliasListNameComboBox().getSelectionModel().getSelectedItem();

                if(aliasList == null)
                {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please select an Alias List",
                        ButtonType.OK);
                    alert.setTitle("Alias List Required");
                    alert.setHeaderText("An alias list is required to create aliases");
                    alert.initOwner((getImportAllTalkgroupsButton()).getScene().getWindow());
                    alert.showAndWait();
                    flashAliasListComboBox();
                }
                else
                {
                    List<Talkgroup> aliasesToCreate = new ArrayList<>();

                    for(AliasedTalkgroup aliasedTalkgroup : mTalkgroupFilteredList)
                    {
                        if(!aliasedTalkgroup.hasAlias())
                        {
                            aliasesToCreate.add(aliasedTalkgroup.getTalkgroup());
                        }
                    }

                    if(!aliasesToCreate.isEmpty())
                    {
                        createAliases(aliasesToCreate);
                    }
                }
            });
        }

        return mImportAllTalkgroupsButton;
    }

    /**
     * Creates an alias for each of the specified talkgroups and adds it to the currently selected alias list
     * @param talkgroups to alias
     */
    public void createAliases(List<Talkgroup> talkgroups)
    {
        List<Alias> createdAliases = new ArrayList<>();

        for(Talkgroup talkgroup: talkgroups)
        {
            TalkgroupCategory talkgroupCategory = getTalkgroupCategory(talkgroup);
            String group = (talkgroupCategory != null ? talkgroupCategory.getName() : null);
            Alias alias = getRadioReferenceDecoder().createAlias(talkgroup, getCurrentSystem(),
                    getAliasListNameComboBox().getSelectionModel().getSelectedItem(), group);

            if(getEncryptedAsDoNotMonitorCheckBox().selectedProperty().get() &&
                    TalkgroupEncryption.lookup(talkgroup.getEncryptionState()) == TalkgroupEncryption.FULL)
            {
                int priority = io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR;
                alias.addAliasID(new io.github.dsheirer.alias.id.priority.Priority(priority));
            }

            createdAliases.add(alias);
        }

        mPlaylistManager.getAliasModel().addAliases(createdAliases);
    }

    /**
     * Retrieves the talkgroup category that matches the specified id from the current set in the talkgroup category
     * combo box.
     * @param talkgroup to match
     * @return matching category or null
     */
    private TalkgroupCategory getTalkgroupCategory(Talkgroup talkgroup)
    {
        if(talkgroup != null)
        {
            for(TalkgroupCategory category: getTalkgroupCategoryComboBox().getItems())
            {
                if(category.getTalkgroupCategoryId() == talkgroup.getTalkgroupCategoryId())
                {
                    return category;
                }
            }
        }

        return null;
    }

    private System getCurrentSystem()
    {
        return mCurrentSystem;
    }

    private AliasList getAliasList()
    {
        if(mAliasList == null)
        {
            mAliasList = new AliasList("empty");
        }

        return mAliasList;
    }

    /**
     * Retrieves any alias that matches the talkgroup value from the currently selected alias list
     * @param talkgroup
     * @return
     */
    private Alias getAlias(Talkgroup talkgroup)
    {
        TalkgroupIdentifier talkgroupIdentifier = getRadioReferenceDecoder().getIdentifier(talkgroup, getCurrentSystem());
        List<Alias> aliases = getAliasList().getAliases(talkgroupIdentifier);
        if(aliases.size() > 0)
        {
            return aliases.get(0);
        }

        return null;
    }

    private RadioReferenceDecoder getRadioReferenceDecoder()
    {
        return mRadioReferenceDecoder;
    }

    private TalkgroupEditor getTalkgroupEditor()
    {
        if(mTalkgroupEditor == null)
        {
            mTalkgroupEditor = new TalkgroupEditor(mUserPreferences, mPlaylistManager);
        }

        return mTalkgroupEditor;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        }

        return mSearchField;
    }

    private ComboBox<String> getAliasListNameComboBox()
    {
        if(mAliasListNameComboBox == null)
        {
            Predicate<String> filterPredicate = s -> !s.contentEquals(AliasModel.NO_ALIAS_LIST);
            FilteredList<String> filteredChannelList =
                new FilteredList<>(mPlaylistManager.getAliasModel().aliasListNames(), filterPredicate);
            mAliasListNameComboBox = new ComboBox<>(filteredChannelList);
            mAliasListNameComboBox.setPrefWidth(150);
            mAliasListNameComboBox.setOnAction(event -> updateAliasList(getAliasListNameComboBox()
                .getSelectionModel().getSelectedItem()));

            if(mAliasListNameComboBox.getItems().size() > 0)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }

            updateAliasList(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        }

        return mAliasListNameComboBox;
    }

    /**
     * Updates the alias list whenever the alias list combo box changes.  Refreshes the alias for each talkgroup
     * table entry from the new list and registers a listener to detect changes to the alias list that might occur
     * on the alias tab, so that this table stays in sync with any alias changes.
     */
    private void updateAliasList(String aliasListName)
    {
        if(mAliasList != null)
        {
            mAliasList.aliases().removeListener(mAliasListChangeListener);
        }

        mAliasList = mPlaylistManager.getAliasModel().getAliasList(aliasListName);

        if(mAliasList != null)
        {
            mAliasList.aliases().addListener(mAliasListChangeListener);
        }

        //Refresh the alias for each item in the table
        for(AliasedTalkgroup item: mTalkgroupList)
        {
            item.setAlias(getAlias(item.getTalkgroup()));
        }
    }

    /**
     * Updates the talkgroup and alias table when there is an alias change detected.  Discovers all talkgroup and
     * talkgroup ranges that are aliased and then updates the corresponding alias for any talkgroups that are in the table.
     */
    private void updateAlias(Alias alias)
    {
        AliasedTalkgroup currentlySelected = getTalkgroupTableView().getSelectionModel().getSelectedItem();

        if(alias != null)
        {
            for(AliasID aliasID: alias.getAliasIdentifiers())
            {
                if(aliasID instanceof io.github.dsheirer.alias.id.talkgroup.Talkgroup)
                {
                    int value = ((io.github.dsheirer.alias.id.talkgroup.Talkgroup)aliasID).getValue();

                    for(AliasedTalkgroup aliasedTalkgroup : mTalkgroupList)
                    {
                        if(aliasedTalkgroup.getTalkgroupValue() == value)
                        {
                            //Even though the new alias is triggering the change, we still use the alias that the
                            //alias list provides.  That way if there's duplicate aliases, the user will always see the
                            //alias that will match the talkgroup during operation.
                            aliasedTalkgroup.setAlias(getAlias(aliasedTalkgroup.getTalkgroup()));

                            //Re-select the current item to refresh the editor view.
                            if(currentlySelected != null && currentlySelected == aliasedTalkgroup)
                            {
                                getTalkgroupTableView().getSelectionModel().select(null);
                                getTalkgroupTableView().getSelectionModel().select(aliasedTalkgroup);
                            }
                        }
                    }
                }
                else if(aliasID instanceof TalkgroupRange)
                {
                    TalkgroupRange range = (TalkgroupRange)aliasID;

                    for(int x = range.getMinTalkgroup(); x <= range.getMaxTalkgroup(); x++)
                    {
                        for(AliasedTalkgroup aliasedTalkgroup : mTalkgroupList)
                        {
                            if(aliasedTalkgroup.getTalkgroupValue() == x)
                            {
                                //Even though the new alias is triggering the change, we still use the alias that the
                                //alias list provides.  That way if there's duplicate aliases, the user will always see the
                                //alias that will match the talkgroup during operation.
                                aliasedTalkgroup.setAlias(getAlias(aliasedTalkgroup.getTalkgroup()));

                                //Re-select the current item to refresh the editor view.
                                if(currentlySelected != null && currentlySelected == aliasedTalkgroup)
                                {
                                    getTalkgroupTableView().getSelectionModel().select(null);
                                    getTalkgroupTableView().getSelectionModel().select(aliasedTalkgroup);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create New Alias List");
                dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();

                result.ifPresent(s -> {
                    String name = result.get();

                    if(name != null && !name.isEmpty())
                    {
                        name = name.trim();
                        mPlaylistManager.getAliasModel().addAliasList(name);
                        getAliasListNameComboBox().getSelectionModel().select(name);
                    }
                });
            });
        }

        return mNewAliasListButton;
    }

    private ComboBox<TalkgroupCategory> getTalkgroupCategoryComboBox()
    {
        if(mTalkgroupCategoryComboBox == null)
        {
            mTalkgroupCategoryComboBox = new ComboBox<>();
            mTalkgroupCategoryComboBox.setMaxWidth(Double.MAX_VALUE);
            mTalkgroupCategoryComboBox.setConverter(new TalkgroupCategoryStringConverter());
            mTalkgroupCategoryComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateFilter());
        }

        return mTalkgroupCategoryComboBox;
    }

    private TableView<AliasedTalkgroup> getTalkgroupTableView()
    {
        if(mTalkgroupTableView == null)
        {
            mTalkgroupTableView = new TableView<>();
            mTalkgroupTableView.setMaxHeight(Double.MAX_VALUE);
            TableColumn<AliasedTalkgroup,String> talkgroupColumn = new TableColumn("Talkgroup");
            talkgroupColumn.setCellValueFactory(new PropertyValueFactory<>("talkgroup"));

            TableColumn<AliasedTalkgroup,String> descriptionColumn = new TableColumn("Description");
            descriptionColumn.setPrefWidth(300);
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

            TableColumn<AliasedTalkgroup,String> aliasColumn = new TableColumn("Alias");
            aliasColumn.setPrefWidth(170);
            aliasColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));

            mTalkgroupTableView.getColumns().addAll(talkgroupColumn, descriptionColumn, aliasColumn);
            mTalkgroupTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, selected) -> {

                    TalkgroupCategory talkgroupCategory =
                        getTalkgroupCategory(selected != null ? selected.getTalkgroup() : null);
                    String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
                    getTalkgroupEditor().setTalkgroup((selected != null ? selected.getTalkgroup() : null),
                        getCurrentSystem(), getRadioReferenceDecoder(), (selected != null ? selected.getAlias() : null),
                        aliasListName, (talkgroupCategory != null ? talkgroupCategory.getName() : null),
                        getEncryptedAsDoNotMonitorCheckBox().selectedProperty().get());
                });
            mTalkgroupFilteredList = new FilteredList<>(mTalkgroupList);
            SortedList<AliasedTalkgroup> sortedList = new SortedList<>(mTalkgroupFilteredList);
            sortedList.comparatorProperty().bind(mTalkgroupTableView.comparatorProperty());
            mTalkgroupTableView.setItems(sortedList);
        }

        return mTalkgroupTableView;
    }

    public class TalkgroupCategoryListCell extends ListCell<TalkgroupCategory>
    {
        @Override
        protected void updateItem(TalkgroupCategory item, boolean empty)
        {
            super.updateItem(item, empty);
            setText((empty || item == null) ? null : item.getName());
        }
    }

    public class TalkgroupCategoryStringConverter extends StringConverter<TalkgroupCategory>
    {
        @Override
        public String toString(TalkgroupCategory cat)
        {
            return cat != null ? cat.getName() : null;
        }

        @Override
        public TalkgroupCategory fromString(String string)
        {
            for(TalkgroupCategory cat: getTalkgroupCategoryComboBox().getItems())
            {
                if(cat.getName().contentEquals(string))
                {
                    return cat;
                }
            }

            return null;
        }
    }

    public class TalkgroupFilter implements Predicate<AliasedTalkgroup>
    {
        private String mFilterText;
        private Integer mCategory;

        public TalkgroupFilter()
        {
        }

        public void setFilterText(String filterText)
        {
            mFilterText = filterText != null ? filterText.toLowerCase() : null;
        }

        public void setCategory(Integer category)
        {
            mCategory = category;
        }

        @Override
        public boolean test(AliasedTalkgroup aliasedTalkgroup)
        {
            if(mCategory == null && (mFilterText == null  || mFilterText.isEmpty()))
            {
                return true;
            }

            Talkgroup talkgroup = aliasedTalkgroup.getTalkgroup();

            if(mCategory != null && mFilterText != null)
            {
                if(talkgroup.getTalkgroupCategoryId() != mCategory)
                {
                    return false;
                }

                if(aliasedTalkgroup.descriptionProperty().get() != null &&
                    aliasedTalkgroup.descriptionProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                if(aliasedTalkgroup.talkgroupProperty().get() != null &&
                   aliasedTalkgroup.talkgroupProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                if(aliasedTalkgroup.aliasProperty().get() != null &&
                   aliasedTalkgroup.aliasProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                return false;
            }
            else if(mCategory != null)
            {
                return talkgroup.getTalkgroupCategoryId() == mCategory;
            }
            else
            {
                if(aliasedTalkgroup.descriptionProperty().get() != null &&
                    aliasedTalkgroup.descriptionProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                if(aliasedTalkgroup.talkgroupProperty().get() != null &&
                    aliasedTalkgroup.talkgroupProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                if(aliasedTalkgroup.aliasProperty().get() != null &&
                    aliasedTalkgroup.aliasProperty().get().toLowerCase().contains(mFilterText))
                {
                    return true;
                }

                return false;
            }
        }
    }

    /**
     * Wrapper class for talkgroups and correlated aliases
     */
    public class AliasedTalkgroup
    {
        private Talkgroup mTalkgroup;
        private Alias mAlias;
        private StringProperty mAliasProperty = new SimpleStringProperty();
        private StringProperty mDescriptionProperty = new SimpleStringProperty();
        private StringProperty mTalkgroupProperty = new SimpleStringProperty();

        public AliasedTalkgroup(Talkgroup talkgroup, Alias alias)
        {
            mTalkgroup = talkgroup;
            mDescriptionProperty.setValue(mTalkgroup.getDescription());
            setAlias(alias);
            updateTalkgroup();
        }

        public boolean hasAlias()
        {
            return mAlias != null;
        }

        public Alias getAlias()
        {
            return mAlias;
        }

        public int getTalkgroupValue()
        {
            return getRadioReferenceDecoder().getTalkgroupValue(mTalkgroup, getCurrentSystem());
        }

        public void updateTalkgroup()
        {
            mTalkgroupProperty.set(getRadioReferenceDecoder().format(mTalkgroup, getCurrentSystem()));
        }

        public StringProperty aliasProperty()
        {
            return mAliasProperty;
        }

        public StringProperty descriptionProperty()
        {
            return mDescriptionProperty;
        }

        public StringProperty talkgroupProperty()
        {
            return mTalkgroupProperty;
        }

        public Talkgroup getTalkgroup()
        {
            return mTalkgroup;
        }

        public void setAlias(Alias alias)
        {
            mAliasProperty.unbind();
            mAliasProperty.setValue(null);
            mAlias = alias;

            if(mAlias != null)
            {
                mAliasProperty.bind(mAlias.nameProperty());
            }
        }
    }

    /**
     * Observable list change listener to detect alias changes and update the talkgroup and alias table
     */
    public class AliasListChangeListener implements ListChangeListener<Alias>
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends Alias> change)
        {
            while(change.next())
            {
                if(change.wasAdded())
                {
                    for(Alias alias: change.getAddedSubList())
                    {
                        updateAlias(alias);
                    }
                }
                else if(change.wasRemoved())
                {
                    for(Alias alias: change.getRemoved())
                    {
                        updateAlias(alias);
                    }
                }
                else if(change.wasUpdated())
                {
                    for(int x = change.getFrom(); x < change.getTo(); x++)
                    {
                        updateAlias(change.getList().get(x));
                    }
                }
            }
        }
    }
}
