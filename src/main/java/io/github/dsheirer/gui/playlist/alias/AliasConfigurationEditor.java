/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasFactory;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.gui.playlist.IAliasListRefreshListener;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.service.radioreference.AliasListSynchronizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javax.swing.UIManager;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for aliases
 */
public class AliasConfigurationEditor extends SplitPane implements IAliasListRefreshListener
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasConfigurationEditor.class);

    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private AliasItemEditor mAliasItemEditor;
    private AliasBulkEditor mAliasBulkEditor;
    private Editor mCurrentEditor;
    private TableView<Alias> mAliasTableView;
    private Label mPlaceholderLabel;
    private Button mNewAliasButton;
    private Button mDeleteAliasButton;
    private Button mRenameAliasButton;
    private Button mCloneAliasButton;
    private MenuButton mMoveToAliasButton;
    private VBox mButtonBox;
    private HBox mSearchAndListSelectionBox;
    private TextField mSearchField;
    private ComboBox<String> mAliasListNameComboBox;
    private Button mNewAliasListButton;
    private Button mDeleteAliasListButton;
    private FilteredList<Alias> mAliasFilteredList;
    private SortedList<Alias> mAliasSortedList;
    private AliasPredicate mAliasPredicate;
    private VBox mRadioReferenceBox;
    private Label mRadioReferenceSystemLabel;
    private Label mRadioReferenceStatusLabel;
    private CheckBox mAutoUpdateCheckBox;
    private Button mRefreshAliasListButton;
    private boolean mRefreshInProgress;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Constructs an instance
     * @param playlistManager for playlist operations
     * @param userPreferences for user preferences
     */
    public AliasConfigurationEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mPlaylistManager.addAliasListRefreshListener(this);
        mUserPreferences = userPreferences;

        VBox leftBox = new VBox();
        VBox.setVgrow(getAliasTableView(), Priority.ALWAYS);
        leftBox.getChildren().addAll(getSearchAndListSelectionBox(), getRadioReferenceBox(), getAliasTableView());

        HBox topBox = new HBox();
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        topBox.getChildren().addAll(leftBox, getButtonBox());

        setOrientation(Orientation.VERTICAL);
        mCurrentEditor = getAliasItemEditor();
        getItems().addAll(topBox, getAliasItemEditor());

        //Configuration changes (e.g. a background Radio Reference check) update the Radio Reference controls
        mPlaylistManager.getAliasModel().aliasListConfigurations()
            .addListener((ListChangeListener<AliasListConfiguration>)c -> updateRadioReferenceControls());
        updateRadioReferenceControls();

        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Configuration for the alias list selected in the combo box, or null.
     */
    private AliasListConfiguration getSelectedConfiguration()
    {
        String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
        return mPlaylistManager.getAliasModel().getAliasListConfiguration(aliasListName);
    }

    /**
     * Radio Reference system tracking controls for the selected alias list.  Disabled when the alias list doesn't
     * track a Radio Reference system.
     */
    private VBox getRadioReferenceBox()
    {
        if(mRadioReferenceBox == null)
        {
            //Two rows so that a long system name doesn't push the controls and status off the editor
            Label label = new Label("Radio Reference:");
            label.setLabelFor(getRadioReferenceSystemLabel());
            label.setMinWidth(Region.USE_PREF_SIZE);
            HBox systemRow = new HBox(10, label, getRadioReferenceSystemLabel());
            systemRow.setAlignment(Pos.CENTER_LEFT);

            HBox controlsRow = new HBox(10, getAutoUpdateCheckBox(), getRefreshAliasListButton(),
                getRadioReferenceStatusLabel());
            controlsRow.setAlignment(Pos.CENTER_LEFT);

            mRadioReferenceBox = new VBox(5, systemRow, controlsRow);
            mRadioReferenceBox.setPadding(new Insets(0, 0, 10, 10));
        }

        return mRadioReferenceBox;
    }

    private Label getRadioReferenceSystemLabel()
    {
        if(mRadioReferenceSystemLabel == null)
        {
            mRadioReferenceSystemLabel = new Label();
        }

        return mRadioReferenceSystemLabel;
    }

    private Label getRadioReferenceStatusLabel()
    {
        if(mRadioReferenceStatusLabel == null)
        {
            mRadioReferenceStatusLabel = new Label();
        }

        return mRadioReferenceStatusLabel;
    }

    private CheckBox getAutoUpdateCheckBox()
    {
        if(mAutoUpdateCheckBox == null)
        {
            mAutoUpdateCheckBox = new CheckBox("Auto-Update Talkgroups");
            mAutoUpdateCheckBox.setTooltip(new Tooltip("Automatically add or update talkgroup aliases when the " +
                "system changes on Radio Reference.  Set how often to check in User Preferences > Radio Reference > Alias Lists."));
            mAutoUpdateCheckBox.setOnAction(event -> {
                AliasListConfiguration configuration = getSelectedConfiguration();

                if(configuration != null && configuration.isTrackingRadioReferenceSystem())
                {
                    configuration.setAutoUpdate(getAutoUpdateCheckBox().isSelected());
                }
            });
        }

        return mAutoUpdateCheckBox;
    }

    private Button getRefreshAliasListButton()
    {
        if(mRefreshAliasListButton == null)
        {
            mRefreshAliasListButton = new Button("Refresh Now");
            mRefreshAliasListButton.setTooltip(new Tooltip("Add or update talkgroup aliases from Radio Reference now"));
            mRefreshAliasListButton.setOnAction(event -> {
                AliasListConfiguration configuration = getSelectedConfiguration();

                if(configuration == null || !configuration.isTrackingRadioReferenceSystem())
                {
                    return;
                }

                mRefreshInProgress = true;
                updateRadioReferenceControls();
                getRadioReferenceStatusLabel().setText("Refreshing from Radio Reference...");

                mPlaylistManager.getAliasListSynchronizer().refresh(configuration)
                    .whenComplete((result, throwable) -> Platform.runLater(() -> {
                        mRefreshInProgress = false;
                        updateRadioReferenceControls();

                        if(throwable != null)
                        {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to refresh alias list [" +
                                configuration.getName() + "] from Radio Reference.\n\n" +
                                AliasListSynchronizer.getMessage(throwable), ButtonType.OK);
                            alert.setTitle("Refresh Alias List");
                            alert.setHeaderText("Radio Reference Refresh Failed");
                            alert.initOwner(getRefreshAliasListButton().getScene().getWindow());
                            alert.show();
                        }
                        else if(result != null && result.applied() && configuration == getSelectedConfiguration())
                        {
                            getRadioReferenceStatusLabel().setText("Added " + result.added() + ", updated " +
                                result.updated() + " aliases.  " + getStatusText(configuration));
                        }
                    }));
            });
        }

        return mRefreshAliasListButton;
    }

    /**
     * Updates the Radio Reference controls for the currently selected alias list.
     */
    private void updateRadioReferenceControls()
    {
        AliasListConfiguration configuration = getSelectedConfiguration();
        boolean tracking = configuration != null && configuration.isTrackingRadioReferenceSystem();

        getAutoUpdateCheckBox().setDisable(!tracking);
        getAutoUpdateCheckBox().setSelected(tracking && configuration.isAutoUpdate());
        getRefreshAliasListButton().setDisable(!tracking || mRefreshInProgress);

        if(tracking)
        {
            String name = configuration.getRadioReferenceSystemName();
            getRadioReferenceSystemLabel().setText((name != null && !name.isEmpty() ? name : "System") + " [" +
                configuration.getRadioReferenceSystemId() + "]");

            if(!mRefreshInProgress)
            {
                getRadioReferenceStatusLabel().setText(getStatusText(configuration));
            }
        }
        else
        {
            getRadioReferenceSystemLabel().setText("Not tracking a system");
            getRadioReferenceStatusLabel().setText(null);
        }
    }

    /**
     * Status text for a tracked alias list: Radio Reference last updated and synchronized timestamps.
     */
    private static String getStatusText(AliasListConfiguration configuration)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Updated on Radio Reference: ").append(format(configuration.getRadioReferenceLastUpdated()));
        sb.append("  Synchronized: ").append(format(configuration.getSynchronizedLastUpdated()));

        if(configuration.isUpdateAvailable())
        {
            sb.append("  (Update Available)");
        }

        return sb.toString();
    }

    private static String format(long timestamp)
    {
        if(timestamp <= 0)
        {
            return "never";
        }

        return DATE_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
    }

    /**
     * Redraws the alias color swatches after a change to the application theme.
     */
    @Subscribe
    public void onPreferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.APPLICATION)
        {
            Platform.runLater(() -> getAliasTableView().refresh());
        }
    }

    /**
     * Prepares for an alias list refresh by clearing the currently selected alias item from the editor.
     */
    @Override
    public void prepareForAliasListRefresh()
    {
        getAliasTableView().getSelectionModel().select(null);
    }

    /**
     * Request to show the specified alias in the editor.
     * <p>
     * Note: this must be called on the FX platform thread
     *
     * @param alias to show
     */
    public void show(Alias alias)
    {
        if(alias != null)
        {
            String aliasList = alias.getAliasListName();

            if(aliasList == null || aliasList.isEmpty())
            {
                aliasList = AliasModel.NO_ALIAS_LIST;
            }

            getAliasListNameComboBox().getSelectionModel().select(aliasList);
            getAliasTableView().getSelectionModel().clearSelection();
            getAliasTableView().getSelectionModel().select(alias);
            getAliasTableView().scrollTo(alias);
        }
    }

    /**
     * Sets the editor as the bottom alias editor, either single alias or bulk alias editor.
     */
    private void setEditor(Editor editor)
    {
        if(editor != mCurrentEditor)
        {
            getItems().remove(mCurrentEditor);
            mCurrentEditor = editor;
            getItems().add(mCurrentEditor);
        }
    }

    private void setAliases(List<Alias> aliases)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getAliasItemEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Alias configuration has been modified");
            alert.setContentText("Do you want to save these changes?");
            alert.initOwner((getButtonBox()).getScene().getWindow());

            //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
            alert.setResizable(true);
            alert.onShownProperty().addListener(e -> {
                Platform.runLater(() -> alert.setResizable(false));
            });

            Optional<ButtonType> result = alert.showAndWait();

            if(result.isPresent() && result.get() == ButtonType.YES)
            {
                getAliasItemEditor().save();
            }
        }

        if(aliases.size() <= 1)
        {
            setEditor(getAliasItemEditor());
            if(aliases.size() == 1)
            {
                getAliasItemEditor().setItem(aliases.get(0));
            }
            else
            {
                getAliasItemEditor().setItem(null);
            }
        }
        else
        {
            setEditor(getAliasBulkEditor());
            getAliasBulkEditor().setItem(aliases);
        }

        getCloneAliasButton().setDisable(aliases.size() != 1);
        getDeleteAliasButton().setDisable(aliases.isEmpty());
        getMoveToAliasButton().setDisable(aliases.isEmpty());
    }

    private AliasItemEditor getAliasItemEditor()
    {
        if(mAliasItemEditor == null)
        {
            mAliasItemEditor = new AliasItemEditor(mPlaylistManager, mUserPreferences);
        }

        return mAliasItemEditor;
    }

    private AliasBulkEditor getAliasBulkEditor()
    {
        if(mAliasBulkEditor == null)
        {
            mAliasBulkEditor = new AliasBulkEditor(mPlaylistManager);
        }

        return mAliasBulkEditor;
    }

    private HBox getSearchAndListSelectionBox()
    {
        if(mSearchAndListSelectionBox == null)
        {
            mSearchAndListSelectionBox = new HBox();
            mSearchAndListSelectionBox.setAlignment(Pos.CENTER_LEFT);
            mSearchAndListSelectionBox.setPadding(new Insets(10, 0, 10, 10));
            mSearchAndListSelectionBox.setSpacing(5);


            Label listLabel = new Label("Alias List");
            listLabel.setLabelFor(getAliasListNameComboBox());
            Label searchLabel = new Label("Search");
            searchLabel.setLabelFor(getSearchField());
            searchLabel.setAlignment(Pos.CENTER_RIGHT);

            HBox searchBox = new HBox();
            searchBox.setSpacing(5);
            searchBox.getChildren().addAll(searchLabel, getSearchField());
            HBox.setHgrow(searchBox, Priority.ALWAYS);
            searchBox.setAlignment(Pos.BASELINE_RIGHT);

            mSearchAndListSelectionBox.getChildren().addAll(listLabel, getAliasListNameComboBox(),
                getNewAliasListButton(), getRenameAliasListButton(), getDeleteAliasListButton(), searchBox);
        }

        return mSearchAndListSelectionBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> update());
        }

        return mSearchField;
    }

    private void update()
    {
        getAliasFilteredList().setPredicate(null);
        getAliasPredicate().setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        getAliasPredicate().setSearchText(getSearchField().getText());
        getAliasFilteredList().setPredicate(getAliasPredicate());
    }

    private AliasPredicate getAliasPredicate()
    {
        if(mAliasPredicate == null)
        {
            mAliasPredicate = new AliasPredicate();
            mAliasPredicate.setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        }

        return mAliasPredicate;
    }

    private ComboBox<String> getAliasListNameComboBox()
    {
        if(mAliasListNameComboBox == null)
        {
            mAliasListNameComboBox = new ComboBox<>(mPlaylistManager.getAliasModel().aliasListNames());
            mAliasListNameComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
                    {
                        getNewAliasButton().setDisable(newValue == null || newValue.contentEquals(AliasModel.NO_ALIAS_LIST));
                        update();
                        updateRadioReferenceControls();
                    });

            if(mAliasListNameComboBox.getItems().size() > 1)
            {
                if(!mAliasListNameComboBox.getItems().get(0).contentEquals(AliasModel.NO_ALIAS_LIST))
                {
                    mAliasListNameComboBox.getSelectionModel().select(0);
                }
                else
                {
                    mAliasListNameComboBox.getSelectionModel().select(1);
                }
            }
            else if(mAliasListNameComboBox.getItems().size() == 1)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }
        }

        return mAliasListNameComboBox;
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.setOnAction(event ->
            {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create New Alias List");
                dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();

                result.ifPresent(s ->
                {
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

    private Button getRenameAliasListButton() {

        if (mRenameAliasButton == null) {
            mRenameAliasButton = new Button("Rename Alias List");
            mRenameAliasButton.setOnAction(event -> {
                String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();

                if (aliasListName.equals(AliasModel.NO_ALIAS_LIST)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
                    alert.setTitle("Rename Alias List");
                    alert.setHeaderText("You cannot rename " + aliasListName + ".");
                    alert.initOwner((getRenameAliasListButton()).getScene().getWindow());
                    alert.showAndWait();
                    return;
                }

                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Renaming Alias List: " + aliasListName);
                dialog.setHeaderText("Please enter the new alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(newAliasListName -> {
                    mPlaylistManager.renameAliasList(aliasListName, newAliasListName);
                    getAliasListNameComboBox().getSelectionModel().select(newAliasListName);
                });
            });

        }
        return mRenameAliasButton;
    }

    private Button getDeleteAliasListButton() {

        if (mDeleteAliasListButton == null) {
            mDeleteAliasListButton = new Button("Delete Alias List");
            mDeleteAliasListButton.setOnAction(event -> {
                String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
                if (aliasListName.equals(AliasModel.NO_ALIAS_LIST)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
                    alert.setTitle("Delete Alias List");
                    alert.setHeaderText("You cannot delete " + aliasListName + ".");
                    alert.initOwner((getDeleteAliasListButton()).getScene().getWindow());
                    alert.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.NO, ButtonType.YES);
                alert.setTitle("Delete Alias List");
                alert.setHeaderText("Are you sure you want to delete the alias list " + aliasListName + " and all associated aliases?");
                alert.initOwner((getDeleteAliasListButton()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES)
                {
                    mPlaylistManager.deleteAliasList(aliasListName);
                }
            });
        }

        return mDeleteAliasListButton;
    }


    private TableView<Alias> getAliasTableView()
    {
        if(mAliasTableView == null)
        {
            mAliasTableView = new TableView<>();
            mAliasTableView.setAccessibleText("Aliases");

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias, String>("name"));
            nameColumn.setPrefWidth(140);

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Category");
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));
            groupColumn.setPrefWidth(140);

            TableColumn<Alias, Integer> colorColumn = new TableColumn("Color");
            colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
            colorColumn.setCellFactory(new ColorizedCell());

            TableColumn<Alias, String> iconColumn = new TableColumn("Icon");
            iconColumn.setCellValueFactory(new PropertyValueFactory<>("iconName"));
            iconColumn.setCellFactory(new IconTableCellFactory());

            TableColumn<Alias, Integer> priorityColumn = new TableColumn("Listen");
            priorityColumn.setCellFactory(new PriorityCellFactory());
            priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

            TableColumn<Alias, Boolean> recordColumn = new TableColumn("Record");
            recordColumn.setCellValueFactory(new PropertyValueFactory<>("recordable"));
            recordColumn.setCellFactory(new IconCell(FontAwesome.SQUARE, Color.RED));

            TableColumn<Alias, Boolean> streamColumn = new TableColumn("Stream");
            streamColumn.setCellValueFactory(new PropertyValueFactory<>("streamable"));
            streamColumn.setCellFactory(new IconCell(FontAwesome.VOLUME_UP, Color.DARKBLUE));

            TableColumn<Alias, Integer> idsColumn = new TableColumn("IDs");
            idsColumn.setCellValueFactory(new IdentifierCountCell());

            TableColumn<Alias, Boolean> errorsColumn = new TableColumn<>("Error");
            errorsColumn.setPrefWidth(120);
            errorsColumn.setCellValueFactory(new PropertyValueFactory<>("overlap"));
            errorsColumn.setCellFactory(param ->
            {
                TableCell<Alias, Boolean> tableCell = new TableCell<>()
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
                            IconNode iconNode = new IconNode(FontAwesome.EXCLAMATION_CIRCLE);
                            iconNode.setFill(Color.RED);
                            setGraphic(iconNode);
                            setText("Identifier Overlap");
                        }
                    }
                };

                return tableCell;
            });


            mAliasTableView.getColumns().addAll(nameColumn, groupColumn, colorColumn, iconColumn, priorityColumn,
                    recordColumn, streamColumn, idsColumn, errorsColumn);

            mAliasTableView.setPlaceholder(getPlaceholderLabel());
            mAliasTableView.setItems(getAliasSortedList());
            mAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            mAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                Platform.runLater(() -> setAliases(mAliasTableView.getSelectionModel().getSelectedItems()));
            });
        }

        return mAliasTableView;
    }

    private FilteredList<Alias> getAliasFilteredList()
    {
        if(mAliasFilteredList == null)
        {
            mAliasFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(), getAliasPredicate());
        }

        return mAliasFilteredList;
    }

    private SortedList<Alias> getAliasSortedList()
    {
        if(mAliasSortedList == null)
        {
            mAliasSortedList = new SortedList<>(getAliasFilteredList());
            mAliasSortedList.comparatorProperty().bind(getAliasTableView().comparatorProperty());

            //Don't re-sort while the bulk editor is still applying changes to aliases
            getAliasBulkEditor().changeInProgressProperty().addListener((observable, oldValue, newValue) ->
            {
                if(newValue)
                {
                    mAliasSortedList.comparatorProperty().unbind();
                    mAliasSortedList.setComparator(null);
                }
                else
                {
                    mAliasSortedList.comparatorProperty().bind(getAliasTableView().comparatorProperty());
                }
            });
        }

        return mAliasSortedList;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("Select an Alias List and click the New button to create new aliases");
        }

        return mPlaceholderLabel;
    }

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setPadding(new Insets(10, 10, 10, 10));
            mButtonBox.setSpacing(10);

            Button fillerButton = new Button();
            fillerButton.setVisible(false);
            mButtonBox.getChildren().addAll(fillerButton, getNewAliasButton(), getCloneAliasButton(),
                    getMoveToAliasButton(), getDeleteAliasButton());
        }

        return mButtonBox;
    }

    private Button getNewAliasButton()
    {
        if(mNewAliasButton == null)
        {
            mNewAliasButton = new Button("New");
            mNewAliasButton.setDisable(true);
            mNewAliasButton.setAlignment(Pos.CENTER);
            mNewAliasButton.setMaxWidth(Double.MAX_VALUE);
            mNewAliasButton.setOnAction(event ->
            {
                Alias alias = new Alias("New Alias");
                alias.setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
                mPlaylistManager.getAliasModel().addAlias(alias);

                //Queue a select alias action to allow table to update filter predicate and display the alias
                Platform.runLater(() ->
                {
                    getAliasTableView().getSelectionModel().clearSelection();
                    getAliasTableView().getSelectionModel().select(alias);
                    getAliasTableView().scrollTo(alias);
                });
            });
        }

        return mNewAliasButton;
    }

    private Button getDeleteAliasButton()
    {
        if(mDeleteAliasButton == null)
        {
            mDeleteAliasButton = new Button("Delete");
            mDeleteAliasButton.setDisable(true);
            mDeleteAliasButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteAliasButton.setOnAction(event ->
            {
                int count = getAliasTableView().getSelectionModel().getSelectedItems().size();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete [" + count + "] selected alias" + ((count > 1) ? "es?" : "?"),
                        ButtonType.NO, ButtonType.YES);
                alert.setTitle("Delete Alias");
                alert.setHeaderText("Are you sure?");
                alert.initOwner((getDeleteAliasButton()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();

                if(result.isPresent() && result.get() == ButtonType.YES)
                {
                    List<Alias> selectedAliases = new ArrayList<>(getAliasTableView().getSelectionModel().getSelectedItems());
                    mPlaylistManager.getAliasModel().removeAliases(selectedAliases);
                }
            });
        }

        return mDeleteAliasButton;
    }

    private Button getCloneAliasButton()
    {
        if(mCloneAliasButton == null)
        {
            mCloneAliasButton = new Button("Clone");
            mCloneAliasButton.setDisable(true);
            mCloneAliasButton.setMaxWidth(Double.MAX_VALUE);
            mCloneAliasButton.setOnAction(event ->
            {
                Alias original = getAliasTableView().getSelectionModel().getSelectedItem();
                Alias copy = AliasFactory.shallowCopyOf(original);
                mPlaylistManager.getAliasModel().addAlias(copy);
                getAliasTableView().getSelectionModel().clearSelection();
                getAliasTableView().getSelectionModel().select(copy);
                getAliasTableView().scrollTo(copy);
            });
        }

        return mCloneAliasButton;
    }

    private MenuButton getMoveToAliasButton()
    {
        if(mMoveToAliasButton == null)
        {
            mMoveToAliasButton = new MenuButton("Move To");
            mMoveToAliasButton.setDisable(true);
            mMoveToAliasButton.setOnShowing(event ->
            {
                mMoveToAliasButton.getItems().clear();

                MenuItem emptyItem = new MenuItem("Alias Lists");
                emptyItem.setDisable(true);
                mMoveToAliasButton.getItems().addAll(emptyItem, new SeparatorMenuItem());

                List<String> aliasLists = mPlaylistManager.getAliasModel().getListNames();

                for(String aliasList : aliasLists)
                {
                    if(!aliasList.contentEquals(AliasModel.NO_ALIAS_LIST) &&
                            !aliasList.contentEquals(getAliasListNameComboBox().getSelectionModel().getSelectedItem()))
                    {
                        mMoveToAliasButton.getItems().add(new MoveToAliasListItem(aliasList));
                    }
                }
            });
        }

        return mMoveToAliasButton;
    }

    public class MoveToAliasListItem extends MenuItem
    {
        public MoveToAliasListItem(String aliasList)
        {
            super(aliasList);

            setOnAction(event ->
            {
                List<Alias> selectedAliases = new ArrayList<>(getAliasTableView().getSelectionModel().getSelectedItems());
                for(Alias selected : selectedAliases)
                {
                    AliasList existing = mPlaylistManager.getAliasModel().getAliasList(selected.getAliasListName());
                    existing.removeAlias(selected);

                    selected.setAliasListName(getText());
                    AliasList moveToList = mPlaylistManager.getAliasModel().getAliasList(selected.getAliasListName());
                    moveToList.addAlias(selected);
                }
            });
        }
    }

    public class ColorizedCell implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            final Rectangle rectangle = new Rectangle(20, 20);
            rectangle.setArcHeight(10);
            rectangle.setArcWidth(10);

            TableCell<Alias, Integer> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(Integer item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(!empty && getTableRow() != null)
                    {
                        Alias alias = getTableRow().getItem();

                        if(alias != null)
                        {
                            rectangle.setVisible(true);
                            java.awt.Color color = alias.getDisplayColor();
                            java.awt.Color readable = ThemeManager.getInstance().getReadableColor(color,
                                UIManager.getColor("TextField.background"));
                            rectangle.setFill(ColorUtil.fromInteger(readable.equals(color) ? alias.getColor() :
                                readable.getRGB()));
                            setAccessibleText(String.format("#%06X", alias.getColor() & 0xFFFFFF));
                        }
                        else
                        {
                            rectangle.setVisible(false);
                            setAccessibleText(null);
                        }
                    }
                    else
                    {
                        rectangle.setVisible(false);
                        setAccessibleText(null);
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER);
            tableCell.setGraphic(rectangle);

            return tableCell;
        }
    }

    /**
     * Boolean table cell with an icon visibility bound to the boolean value
     */
    public class IconCell implements Callback<TableColumn<Alias, Boolean>, TableCell<Alias, Boolean>>
    {
        private IconCode mIconCode;
        private Color mColor;

        public IconCell(IconCode iconCode, Color color)
        {
            mIconCode = iconCode;
            mColor = color;
        }

        @Override
        public TableCell<Alias, Boolean> call(TableColumn<Alias, Boolean> param)
        {
            final IconNode iconNode = new IconNode(mIconCode);
            iconNode.setIconSize(20);
            iconNode.setFill(mColor);

            TableCell<Alias, Boolean> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(Boolean item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(!empty && getTableRow() != null && item != null)
                    {
                        iconNode.setVisible(item);
                        setAccessibleText(item ? "Yes" : "No");
                    }
                    else
                    {
                        iconNode.setVisible(false);
                        setAccessibleText(null);
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER);
            tableCell.setGraphic(iconNode);
            return tableCell;
        }
    }

    public class IdentifierCountCell implements Callback<TableColumn.CellDataFeatures<Alias, Integer>, ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias, Integer> param)
        {
            if(param.getValue() != null)
            {
                return param.getValue().nonAudioIdentifierCountProperty().asObject();
            }

            return null;
        }
    }

    public class ActionCountCell implements Callback<TableColumn.CellDataFeatures<Alias, Integer>, ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias, Integer> param)
        {
            Integer count = null;

            if(param.getValue() != null && param.getValue().getAliasActions().size() > 0)
            {
                count = param.getValue().getAliasActions().size();
            }

            return new ReadOnlyObjectWrapper<>(count);
        }
    }

    public class CenteredCountCellFactory implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            return new CenteredCountCell();
        }
    }

    public class CenteredCountCell extends TableCell<Alias, Integer>
    {
        public CenteredCountCell()
        {
            setAlignment(Pos.CENTER);
        }
    }

    public class PriorityCellFactory implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            TableCell tableCell = new TableCell<Alias, Integer>()
            {
                @Override
                protected void updateItem(Integer item, boolean empty)
                {
                    if(empty)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if(item == io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR)
                    {
                        setText("Mute");
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_OFF);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.RED);
                        setGraphic(iconNode);
                    }
                    else if(item == io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY)
                    {
                        setText("Default");
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_UP);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.GREEN);
                        setGraphic(iconNode);
                    }
                    else
                    {
                        setText(item.toString());
                        final IconNode iconNode = new IconNode(FontAwesome.VOLUME_UP);
                        iconNode.setIconSize(20);
                        iconNode.setFill(Color.GREEN);
                        setGraphic(iconNode);
                    }
                }
            };

            return tableCell;
        }
    }

    public class IconTableCellFactory implements Callback<TableColumn<Alias, String>, TableCell<Alias, String>>
    {
        @Override
        public TableCell<Alias, String> call(TableColumn<Alias, String> param)
        {
            TableCell<Alias, String> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER);
                    setAccessibleText(null);

                    if(empty)
                    {
                        setGraphic(null);
                    }
                    else
                    {
                        if(getTableRow() != null)
                        {
                            Alias alias = getTableRow().getItem();

                            if(alias != null)
                            {
                                Icon icon = mPlaylistManager.getIconModel().getIcon(alias.getIconName());

                                if(icon != null && icon.getFxImage() != null)
                                {
                                    setGraphic(new ImageView(icon.getFxImage()));
                                    setAccessibleText(icon.getName());
                                }
                                else
                                {
                                    setGraphic(null);
                                }
                            }
                        }
                    }
                }
            };

            return tableCell;
        }
    }

    /**
     * Alias filter predicate
     */
    public class AliasPredicate implements Predicate<Alias>
    {
        private String mAliasListName;
        private String mSearchText;

        @Override
        public boolean test(Alias alias)
        {
            if(mAliasListName == null)
            {
                return false;
            }
            else if(mAliasListName.equals(alias.getAliasListName()))
            {
                if(alias.getName() == null)
                {
                    return true;
                }
                else if(alias.getName().toLowerCase().contains(mSearchText))
                {
                    return true;
                }
                else if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mSearchText))
                {
                    return true;
                }
            }

            return false;
        }

        public void setAliasListName(String aliasListName)
        {
            if(aliasListName != null)
            {
                mAliasListName = aliasListName;
            }
        }

        public void setSearchText(String searchText)
        {
            if(searchText != null)
            {
                mSearchText = searchText.toLowerCase();
            }
            else
            {
                mSearchText = null;
            }
        }
    }
}
