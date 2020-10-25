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
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class AliasViewByIdentifierEditor extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasViewByIdentifierEditor.class);
    private PlaylistManager mPlaylistManager;
    private ComboBox<String> mAliasListNameComboBox;
    private ComboBox<AliasIDType> mAliasIDTypeComboBox;
    private TableView<AliasAndIdentifier> mAliasAndIdentifierTableView;
    private TableColumn<AliasAndIdentifier,String> mIdentifierColumn;
    private Button mViewAliasButton;
    private boolean aliasListInvalidated;

    public AliasViewByIdentifierEditor(PlaylistManager playlistManager, ReadOnlyBooleanProperty tabSelectedProperty)
    {
    	aliasListInvalidated = false;
        mPlaylistManager = playlistManager;
        //Note if aliases change since we're using a static snapshot
        mPlaylistManager.getAliasModel().aliasList().addListener((InvalidationListener)observable -> aliasListInvalidated = true);
        
        //Only update the list when this tab is selected to avoid many slow updates in the background
        tabSelectedProperty.addListener(observable -> {
        	if (aliasListInvalidated)
        		updateList();
        });

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10,10,10,10));
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        Label aliasListLabel = new Label("Alias List");
        GridPane.setHalignment(aliasListLabel, HPos.RIGHT);
        GridPane.setConstraints(aliasListLabel, 0, 0);
        gridPane.getChildren().add(aliasListLabel);

        GridPane.setConstraints(getAliasListNameComboBox(), 1, 0);
        GridPane.setHgrow(getAliasListNameComboBox(), Priority.ALWAYS);
        gridPane.getChildren().add(getAliasListNameComboBox());

        Label identifierLabel = new Label("Identifier Type");
        GridPane.setHalignment(identifierLabel, HPos.RIGHT);
        GridPane.setConstraints(identifierLabel, 0, 1);
        gridPane.getChildren().add(identifierLabel);

        GridPane.setConstraints(getAliasIDTypeComboBox(), 1, 1);
        GridPane.setHgrow(getAliasIDTypeComboBox(), Priority.ALWAYS);
        gridPane.getChildren().add(getAliasIDTypeComboBox());

        HBox tableAndButtonBox = new HBox();
        tableAndButtonBox.setPadding(new Insets(0,10,0,0));
        tableAndButtonBox.setSpacing(10);
        HBox.setHgrow(getAliasAndIdentifierTableView(), Priority.ALWAYS);
        tableAndButtonBox.getChildren().addAll(getAliasAndIdentifierTableView(), getViewAliasButton());

        VBox.setVgrow(tableAndButtonBox, Priority.ALWAYS);

        getChildren().addAll(gridPane, tableAndButtonBox);

        updateList();
    }

    private void updateList()
    {
        getAliasAndIdentifierTableView().getItems().clear();

        String aliasList = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
        AliasIDType type = getAliasIDTypeComboBox().getSelectionModel().getSelectedItem();

        List<Alias> aliases = mPlaylistManager.getAliasModel().getAliases(aliasList, type);

        for(Alias alias: aliases)
        {
            for(AliasID aliasID: alias.getAliasIdentifiers())
            {
                if(aliasID.getType() == type)
                {
                    getAliasAndIdentifierTableView().getItems().add(new AliasAndIdentifier(alias, aliasID));
                }
            }
        }

        getAliasAndIdentifierTableView().getSortOrder().setAll(mIdentifierColumn);
        
        aliasListInvalidated = false;
    }

    /**
     * Selects the Alias and Identifier that matches the alias ID in the list.
     * @param aliasID
     */
    public void show(AliasID aliasID)
    {
        if(aliasID != null)
        {
            AliasIDType type = aliasID.getType();

            if(getAliasIDTypeComboBox().getItems().contains(type))
            {
                getAliasIDTypeComboBox().getSelectionModel().select(type);

                for(AliasAndIdentifier aliasAndIdentifier: getAliasAndIdentifierTableView().getItems())
                {
                    if(aliasAndIdentifier.getAliasIdentifier().matches(aliasID))
                    {
                        getAliasAndIdentifierTableView().getSelectionModel().select(aliasAndIdentifier);
                        getAliasAndIdentifierTableView().scrollTo(aliasAndIdentifier);
                        return;
                    }
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Can't find matching identifier in list",
                ButtonType.OK);
            alert.setTitle("No Match Found");
            alert.setContentText("No Match Found");
            alert.initOwner(getAliasAndIdentifierTableView().getScene().getWindow());
            alert.showAndWait();
        }
    }

    private Button getViewAliasButton()
    {
        if(mViewAliasButton == null)
        {
            mViewAliasButton = new Button(("View Alias"));
            mViewAliasButton.setDisable(true);
            mViewAliasButton.setOnAction(event -> {
                AliasAndIdentifier selected = getAliasAndIdentifierTableView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    MyEventBus.getGlobalEventBus().post(new ViewAliasRequest(selected.getAlias()));
                }
            });
        }

        return mViewAliasButton;
    }

    private TableView<AliasAndIdentifier> getAliasAndIdentifierTableView()
    {
        if(mAliasAndIdentifierTableView == null)
        {
            mAliasAndIdentifierTableView = new TableView<>(FXCollections.observableArrayList(AliasAndIdentifier.extractor()));
            mAliasAndIdentifierTableView.setPlaceholder(new Label("No aliases or identifiers available"));
            mIdentifierColumn = new TableColumn<>("Identifier");
            mIdentifierColumn.setCellValueFactory(new PropertyValueFactory<>("identifier"));
            mIdentifierColumn.setPrefWidth(350);

            TableColumn<AliasAndIdentifier, String> aliasColumn = new TableColumn<>();
            aliasColumn.setText("Alias");
            aliasColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
            aliasColumn.setPrefWidth(200);

            TableColumn<AliasAndIdentifier, String> groupColumn = new TableColumn<>();
            groupColumn.setText("Group");
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));
            groupColumn.setPrefWidth(200);
            
            mAliasAndIdentifierTableView.getColumns().addAll(mIdentifierColumn, aliasColumn, groupColumn);
            mAliasAndIdentifierTableView.setMaxHeight(Double.MAX_VALUE);

            mAliasAndIdentifierTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    getViewAliasButton().setDisable(newValue == null);
                });
        }

        return mAliasAndIdentifierTableView;
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
            if(mAliasListNameComboBox.getItems().size() > 0)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }
            mAliasListNameComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateList();
                });
        }

        return mAliasListNameComboBox;
    }

    private ComboBox<AliasIDType> getAliasIDTypeComboBox()
    {
        if(mAliasIDTypeComboBox == null)
        {
            mAliasIDTypeComboBox = new ComboBox<>();
            List<AliasIDType> values = new ArrayList<>(AliasIDType.VIEW_BY_VALUES);
            Collections.sort(values, (o1, o2) -> o1.toString().compareTo(o2.toString()));
            mAliasIDTypeComboBox.getItems().addAll(values);

            if(mAliasIDTypeComboBox.getItems().size() > 0)
            {
                mAliasIDTypeComboBox.getSelectionModel().select(AliasIDType.TALKGROUP);
            }

            mAliasIDTypeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateList());
        }

        return mAliasIDTypeComboBox;
    }

    public static class AliasAndIdentifier
    {
        private Alias mAlias;
        private AliasID mAliasIdentifier;
        private StringProperty mName = new SimpleStringProperty();
        private StringProperty mGroup = new SimpleStringProperty();
        private StringProperty mIdentifier = new SimpleStringProperty();

        public AliasAndIdentifier(Alias alias, AliasID aliasID)
        {
            mAlias = alias;
            mAliasIdentifier = aliasID;
            mName.bind(mAlias.nameProperty());
            mGroup.bind(mAlias.groupProperty());
            mIdentifier.bind(mAliasIdentifier.valueProperty());
        }

        public StringProperty nameProperty()
        {
            return mName;
        }

        public StringProperty groupProperty()
        {
            return mGroup;
        }

        public StringProperty identifierProperty()
        {
            return mIdentifier;
        }

        public Alias getAlias()
        {
            return mAlias;
        }

        public AliasID getAliasIdentifier()
        {
            return mAliasIdentifier;
        }

        /**
         * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
         */
        public static Callback<AliasAndIdentifier,Observable[]> extractor()
        {
            return (AliasAndIdentifier a) -> new Observable[] {a.groupProperty(), a.identifierProperty(), a.nameProperty()};
        }
    }
}
