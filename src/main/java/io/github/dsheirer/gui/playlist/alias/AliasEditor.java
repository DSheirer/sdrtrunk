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

package io.github.dsheirer.gui.playlist.alias;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.textfield.TextFields;

import java.util.Optional;

/**
 * Editor for aliases
 */
public class AliasEditor extends SplitPane
{
    private PlaylistManager mPlaylistManager;
    private AliasConfigurationEditor mAliasConfigurationEditor;
    private TableView<Alias> mAliasTableView;
    private Label mPlaceholderLabel;
    private Button mNewButton;
    private Button mDeleteButton;
    private Button mCloneButton;
    private VBox mButtonBox;
    private HBox mSearchBox;
    private TextField mSearchField;

    public AliasEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        HBox channelsBox = new HBox();
        channelsBox.setPadding(new Insets(5, 5, 5, 5));
        channelsBox.setSpacing(5.0);
        HBox.setHgrow(getAliasTableView(), Priority.ALWAYS);
        channelsBox.getChildren().addAll(getAliasTableView(), getButtonBox());

        VBox topBox = new VBox();
        VBox.setVgrow(channelsBox, Priority.ALWAYS);
        topBox.getChildren().addAll(getSearchBox(), channelsBox);

        setOrientation(Orientation.VERTICAL);
        getItems().addAll(topBox, getAliasConfigurationEditor());
    }

    private void setAlias(Alias alias)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getAliasConfigurationEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Alias configuration has been modified");
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
                getAliasConfigurationEditor().save();
            }
        }

        getCloneButton().setDisable(alias == null);
        getDeleteButton().setDisable(alias == null);
        getAliasConfigurationEditor().setItem(alias);
    }

    private AliasConfigurationEditor getAliasConfigurationEditor()
    {
        if(mAliasConfigurationEditor == null)
        {
            mAliasConfigurationEditor = new AliasConfigurationEditor(mPlaylistManager);
        }

        return mAliasConfigurationEditor;
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

    private TableView<Alias> getAliasTableView()
    {
        if(mAliasTableView == null)
        {
            mAliasTableView = new TableView<>();

            TableColumn aliasListNameColumn = new TableColumn();
            aliasListNameColumn.setText("Alias List");
            aliasListNameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasListName"));
            aliasListNameColumn.setPrefWidth(140);

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));
            groupColumn.setPrefWidth(140);

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias,String>("name"));
            nameColumn.setPrefWidth(140);

            TableColumn<Alias,Integer> colorColumn = new TableColumn("Color");
            colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
            colorColumn.setCellFactory(new ColorizedCell());

            TableColumn<Alias,String> iconColumn = new TableColumn("Icon");

            TableColumn<Alias,Integer> priorityColumn = new TableColumn("Priority");
            priorityColumn.setCellFactory(new PriorityCellFactory());
            priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

            TableColumn<Alias,Boolean> recordColumn = new TableColumn("Record");
            recordColumn.setCellValueFactory(new PropertyValueFactory<>("recordable"));
            recordColumn.setCellFactory(new IconCell(FontAwesome.SQUARE, Color.RED));

            TableColumn<Alias,Boolean> streamColumn = new TableColumn("Stream");
            streamColumn.setCellValueFactory(new PropertyValueFactory<>("streamable"));
            streamColumn.setCellFactory(new IconCell(FontAwesome.VOLUME_UP, Color.DARKBLUE));

            TableColumn<Alias,Integer> idsColumn = new TableColumn("IDs");
//            idsColumn.setCellFactory(new CenteredCountCellFactory());
            idsColumn.setCellValueFactory(new IdentifierCountCell());

            TableColumn<Alias,Integer> actionsColumn = new TableColumn("Actions");
//            actionsColumn.setCellFactory(new CenteredCountCellFactory());
            actionsColumn.setCellValueFactory(new ActionCountCell());


            mAliasTableView.getColumns().addAll(aliasListNameColumn, groupColumn, nameColumn, colorColumn,
                iconColumn, priorityColumn, recordColumn, streamColumn, idsColumn, actionsColumn);

            mAliasTableView.setPlaceholder(getPlaceholderLabel());

            //Sorting and filtering for the table
            FilteredList<Alias> filteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(),
                p -> true);

            getSearchField().textProperty()
                .addListener((observable, oldValue, newValue) -> filteredList.setPredicate(alias -> {
                if(newValue == null || newValue.isEmpty())
                {
                    return true;
                }

                String filterText = newValue.toLowerCase();

                if(alias.getName() != null && alias.getName().toLowerCase().contains(filterText))
                {
                    return true;
                }
                else if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(filterText))
                {
                    return true;
                }
                else if(alias.getAliasListName() != null && alias.getAliasListName().toLowerCase().contains(filterText))
                {
                    return true;
                }

                return false;
            }));

            SortedList<Alias> sortedList = new SortedList<>(filteredList);

            sortedList.comparatorProperty().bind(mAliasTableView.comparatorProperty());

            mAliasTableView.setItems(sortedList);

            mAliasTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setAlias(newValue));
        }

        return mAliasTableView;
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

    private Button getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new Button("New");
            mNewButton.setAlignment(Pos.CENTER);
            mNewButton.setMaxWidth(Double.MAX_VALUE);
            mNewButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Alias alias = new Alias("New Alias");
                    mPlaylistManager.getAliasModel().addAlias(alias);
                    setAlias(alias);
                }
            });
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
            mDeleteButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Alias selected = getAliasTableView().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Do you want to delete the selected alias?", ButtonType.NO, ButtonType.YES);
                        alert.setTitle("Delete Alias");
                        alert.setHeaderText("Are you sure?");
                        alert.initOwner(((Node)getDeleteButton()).getScene().getWindow());

                        Optional<ButtonType> result = alert.showAndWait();

                        if(result.get() == ButtonType.YES)
                        {
                            mPlaylistManager.getAliasModel().removeAlias(selected);
                        }
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
//            mCloneButton.setOnAction(event -> {
//                Channel selected = getAliasTableView().getSelectionModel().getSelectedItem();
//                Channel copy = selected.copyOf();
//                mPlaylistManager.getChannelModel().addChannel(copy);
//                getAliasTableView().getSelectionModel().select(copy);
//            });
        }

        return mCloneButton;
    }

    public class ColorizedCell implements Callback<TableColumn<Alias,Integer>,TableCell<Alias,Integer>>
    {
        @Override
        public TableCell<Alias,Integer> call(TableColumn<Alias,Integer> param)
        {
            final Rectangle rectangle = new Rectangle(20,20);
            rectangle.setArcHeight(10);
            rectangle.setArcWidth(10);

            TableCell<Alias,Integer> tableCell = new TableCell<>()
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
                            rectangle.setFill(ColorUtil.fromInteger(alias.getColor()));
                        }
                        else
                        {
                            rectangle.setVisible(false);
                        }
                    }
                    else
                    {
                        rectangle.setVisible(false);
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
    public class IconCell implements Callback<TableColumn<Alias,Boolean>,TableCell<Alias,Boolean>>
    {
        private IconCode mIconCode;
        private Color mColor;

        public IconCell(IconCode iconCode, Color color)
        {
            mIconCode = iconCode;
            mColor = color;
        }

        @Override
        public TableCell<Alias,Boolean> call(TableColumn<Alias,Boolean> param)
        {
            final IconNode iconNode = new IconNode(mIconCode);
            iconNode.setIconSize(20);
            iconNode.setFill(mColor);

            TableCell<Alias,Boolean> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(Boolean item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(!empty && getTableRow() != null)
                    {
                        iconNode.setVisible(item);
                    }
                    else
                    {
                        iconNode.setVisible(false);
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER);
            tableCell.setGraphic(iconNode);
            return tableCell;
        }
    }

    public class IdentifierCountCell implements Callback<TableColumn.CellDataFeatures<Alias,Integer>,ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias,Integer> param)
        {
            Integer count = null;

            if(param.getValue() != null && param.getValue().getAliasIdentifiers().size() > 0)
            {
                count = param.getValue().getAliasIdentifiers().size();
            }

            return new ReadOnlyObjectWrapper<>(count);
        }
    }

    public class ActionCountCell implements Callback<TableColumn.CellDataFeatures<Alias,Integer>,ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias,Integer> param)
        {
            Integer count = null;

            if(param.getValue() != null && param.getValue().getAliasActions().size() > 0)
            {
                count = param.getValue().getAliasActions().size();
            }

            return new ReadOnlyObjectWrapper<>(count);
        }
    }

    public class CenteredCountCellFactory implements Callback<TableColumn<Alias,Integer>,TableCell<Alias,Integer>>
    {
        @Override
        public TableCell<Alias,Integer> call(TableColumn<Alias,Integer> param)
        {
            return new CenteredCountCell();
        }
    }

    public class CenteredCountCell extends TableCell<Alias,Integer>
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
            TableCell tableCell = new TableCell<Alias,Integer>()
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
                        setText(null);
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
}
