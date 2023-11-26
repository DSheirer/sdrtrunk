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

package io.github.dsheirer.gui.javafx.table;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Editor for managing table column ordering and visibility.
 */
public class TableColumnEditor extends VBox
{
    private TableView mTableView;
    private TableViewColumnController mTableViewColumnController;
    private TableView mColumnsTableView;
    private Button mCloseButton;
    private Button mOrderUpButton;
    private Button mOrderDownButton;
    private Button mResetColumnsButton;
    private BooleanProperty mClosed = new SimpleBooleanProperty();

    /**
     * Constructs an instance
     * @param tableView to be configured/managed
     */
    public TableColumnEditor(TableView tableView, TableViewColumnController monitor)
    {
        mTableView = tableView;
        mTableViewColumnController = monitor;
        setPadding(new Insets(10));
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(5);
        int row = 0;

        Label infoLabel = new Label("Configure Columns");
        GridPane.setConstraints(infoLabel, 0, row);
        gridPane.getChildren().add(infoLabel);

        GridPane.setConstraints(getCloseButton(), 1, row);
        gridPane.getChildren().add(getCloseButton());

        row++;

        GridPane.setConstraints(getColumnsTableView(), 0, row, 1, 6);
        gridPane.getChildren().add(getColumnsTableView());

        row += 2;

        GridPane.setConstraints(getOrderUpButton(), 1, row);
        gridPane.getChildren().add(getOrderUpButton());

        row++;

        GridPane.setConstraints(getOrderDownButton(), 1, row);
        gridPane.getChildren().add(getOrderDownButton());

        row++;
        GridPane.setConstraints(getResetColumnsButton(), 1, row);
        gridPane.getChildren().add(getResetColumnsButton());

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.NEVER);
        cc2.setPrefWidth(60);
        cc2.setMinWidth(60);
        gridPane.getColumnConstraints().addAll(cc1, cc2);

        VBox.setVgrow(gridPane, Priority.ALWAYS);
        getChildren().add(gridPane);
    }

    private TableView<TableColumn> getColumnsTableView()
    {
        if(mColumnsTableView == null)
        {
            mColumnsTableView = new TableView(mTableView.getColumns());
            mColumnsTableView.setEditable(true);

            TableColumn textColumn = new TableColumn("Column");
            textColumn.setEditable(false);
            textColumn.setCellValueFactory(new PropertyValueFactory<>("text"));
            mColumnsTableView.getColumns().add(textColumn);

            TableColumn<TableColumn,Boolean> visibleColumn = new TableColumn("Visible");
            visibleColumn.setCellValueFactory(new PropertyValueFactory<>("visible"));
            visibleColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibleColumn));
            visibleColumn.setEditable(true);
            mColumnsTableView.getColumns().add(visibleColumn);

            mColumnsTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener()
            {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue)
                {
                    //If monitored table has only a single column keep both buttons disabled.
                    if(mTableView.getColumns().size() <= 1)
                    {
                        getOrderDownButton().setDisable(true);
                        getOrderUpButton().setDisable(true);
                        return;
                    }

                    if(newValue == null)
                    {
                        getOrderDownButton().setDisable(true);
                        getOrderUpButton().setDisable(true);
                    }
                    else
                    {
                        if(mColumnsTableView.getSelectionModel().isSelected(0))
                        {
                            getOrderDownButton().setDisable(false);
                            getOrderUpButton().setDisable(true);
                        }
                        else if(mColumnsTableView.getSelectionModel().isSelected(mTableView.getColumns().size() - 1))
                        {
                            getOrderDownButton().setDisable(true);
                            getOrderUpButton().setDisable(false);
                        }
                        else
                        {
                            getOrderDownButton().setDisable(false);
                            getOrderUpButton().setDisable(false);
                        }
                    }
                }
            });
        }

        return mColumnsTableView;
    }

    /**
     * Close button
     * @return button
     */
    private Button getCloseButton()
    {
        if(mCloseButton == null)
        {
            mCloseButton = new Button("Close");
            mCloseButton.setMaxWidth(Double.MAX_VALUE);
            mCloseButton.setOnAction(event -> closedProperty().set(true));
        }

        return mCloseButton;
    }

    private Button getOrderUpButton()
    {
        if(mOrderUpButton == null)
        {
            mOrderUpButton = new Button("Up");
            mOrderUpButton.setDisable(true);
            mOrderUpButton.setMaxWidth(Double.MAX_VALUE);
            mOrderUpButton.setOnAction(event -> {
                if(mColumnsTableView.getSelectionModel().selectedItemProperty().get() != null)
                {
                    TableColumn selected = (TableColumn)mColumnsTableView.getSelectionModel().getSelectedItem();
                    int currentIndex = mTableView.getColumns().indexOf(selected);
                    mTableView.getColumns().remove(selected);
                    mTableView.getColumns().add(currentIndex - 1, selected);
                    //Reselect the column since it moved and lost the selection
                    mColumnsTableView.getSelectionModel().select(selected);
                }
            });
        }

        return mOrderUpButton;
    }

    private Button getOrderDownButton()
    {
        if(mOrderDownButton == null)
        {
            mOrderDownButton = new Button("Down");
            mOrderDownButton.setDisable(true);
            mOrderDownButton.setMaxWidth(Double.MAX_VALUE);
            mOrderDownButton.setOnAction(event -> {
                if(mColumnsTableView.getSelectionModel().selectedItemProperty().get() != null)
                {
                    TableColumn selected = (TableColumn)mColumnsTableView.getSelectionModel().getSelectedItem();
                    int currentIndex = mTableView.getColumns().indexOf(selected);
                    mTableView.getColumns().remove(selected);
                    mTableView.getColumns().add(currentIndex + 1, selected);
                    //Reselect the column since it moved and lost the selection
                    mColumnsTableView.getSelectionModel().select(selected);
                }
            });
        }

        return mOrderDownButton;
    }

    /**
     * Resets the columns to default state.
     * @return button to reset
     */
    private Button getResetColumnsButton()
    {
        if(mResetColumnsButton == null)
        {
            mResetColumnsButton = new Button("Reset");
            mResetColumnsButton.setMaxWidth(Double.MAX_VALUE);
            mResetColumnsButton.setOnAction(event -> mTableViewColumnController.reset());
        }

        return mResetColumnsButton;
    }

    /**
     * Observable property to monitor for when the user clicks the close button.
     */
    public BooleanProperty closedProperty()
    {
        return mClosed;
    }
}
