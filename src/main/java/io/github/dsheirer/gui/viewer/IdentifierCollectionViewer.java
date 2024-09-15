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

package io.github.dsheirer.gui.viewer;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * JavaFX identifier collection viewer
 */
public class IdentifierCollectionViewer extends VBox
{
    private TableView<Identifier> mIdentifierTableView;

    /**
     * Constructs an instance
     */
    public IdentifierCollectionViewer()
    {
        GridPane gridPane = new GridPane();
        GridPane.setHgrow(getIdentifierTableView(), Priority.ALWAYS);
        gridPane.add(getIdentifierTableView(), 0, 0);
        getChildren().add(gridPane);
    }

    public void set(IdentifierCollection identifierCollection)
    {
        getIdentifierTableView().getItems().clear();

        if(identifierCollection != null)
        {
            getIdentifierTableView().getItems().addAll(identifierCollection.getIdentifiers());
        }
    }

    public TableView<Identifier> getIdentifierTableView()
    {
        if(mIdentifierTableView == null)
        {
            mIdentifierTableView = new TableView<>();

            TableColumn classColumn = new TableColumn();
            classColumn.setPrefWidth(110);
            classColumn.setText("Class");
            classColumn.setCellValueFactory(new PropertyValueFactory<>("identifierClass"));

            TableColumn formColumn = new TableColumn();
            formColumn.setPrefWidth(160);
            formColumn.setText("Form");
            formColumn.setCellValueFactory(new PropertyValueFactory<>("form"));

            TableColumn roleColumn = new TableColumn();
            roleColumn.setPrefWidth(110);
            roleColumn.setText("Role");
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

            TableColumn valueColumn = new TableColumn();
            valueColumn.setPrefWidth(160);
            valueColumn.setText("Value");
            valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

            TableColumn textColumn = new TableColumn();
            textColumn.setPrefWidth(160);
            textColumn.setText("Text");
            textColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Identifier<?>, String>, ObservableValue>) param -> {
                if(param.getValue() != null)
                {
                    return new SimpleStringProperty(param.getValue().toString());
                }

                return null;
            });

            mIdentifierTableView.getColumns().addAll(classColumn, formColumn, roleColumn, valueColumn, textColumn);
        }

        return mIdentifierTableView;
    }
}
