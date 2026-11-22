/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.gui.table;

import io.github.dsheirer.gui.theme.ThemeManager;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Editor window for choosing the columns to show in a table and the order of the columns.  Changes apply to the
 * table immediately and are stored by the table column monitor.
 */
public class ColumnConfigurationEditor
{
    private final TableColumnMonitor<?> mTableColumnMonitor;
    private final String mTitle;
    private Stage mStage;
    private ListView<TableColumn<?,?>> mListView;
    private Button mMoveUpButton;
    private Button mMoveDownButton;
    private Button mResetButton;

    /**
     * Constructs an instance
     * @param tableColumnMonitor for the table
     * @param title for the window
     */
    public ColumnConfigurationEditor(TableColumnMonitor<?> tableColumnMonitor, String title)
    {
        mTableColumnMonitor = tableColumnMonitor;
        mTitle = title;
    }

    /**
     * Shows the editor next to the node, or brings it to the front when it is already showing.
     * @param anchor node for positioning the editor window
     */
    public void show(Node anchor)
    {
        if(mStage == null)
        {
            mStage = createStage();
        }

        if(!mStage.isShowing())
        {
            mListView.getSelectionModel().clearSelection();

            if(anchor != null && anchor.getScene() != null)
            {
                Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());

                if(bounds != null)
                {
                    mStage.setX(bounds.getMinX());
                    mStage.setY(bounds.getMaxY());
                }
            }

            mStage.show();
        }

        mStage.toFront();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stage createStage()
    {
        Stage stage = new Stage();
        stage.setTitle(mTitle);

        mListView = new ListView<>();
        //The list shares the table's column list, so that column header drags in the table show here too
        mListView.setItems((javafx.collections.ObservableList)mTableColumnMonitor.getTableView().getColumns());
        mListView.setCellFactory(CheckBoxListCell.forListView(TableColumn::visibleProperty,
            new StringConverter<>()
            {
                @Override
                public String toString(TableColumn<?,?> column)
                {
                    return column != null ? TableColumnMonitor.getName(column) : null;
                }

                @Override
                public TableColumn<?,?> fromString(String string)
                {
                    return null;
                }
            }));
        mListView.setPrefHeight(300);
        mListView.setPrefWidth(220);
        mListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> updateButtons());
        mListView.getItems().addListener((ListChangeListener<TableColumn<?,?>>)c -> updateButtons());

        mMoveUpButton = new Button("Move Up");
        mMoveUpButton.setTooltip(new Tooltip("Move the selected column toward the left side of the table"));
        mMoveUpButton.setOnAction(event -> move(-1));
        mMoveDownButton = new Button("Move Down");
        mMoveDownButton.setTooltip(new Tooltip("Move the selected column toward the right side of the table"));
        mMoveDownButton.setOnAction(event -> move(1));
        mResetButton = new Button("Reset");
        mResetButton.setTooltip(new Tooltip("Restore the default columns and column order"));
        mResetButton.setOnAction(event -> {
            mTableColumnMonitor.reset();
            mListView.getSelectionModel().clearSelection();
            updateButtons();
        });

        for(Button button: new Button[]{mMoveUpButton, mMoveDownButton, mResetButton})
        {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button closeButton = new Button("Close");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setOnAction(event -> stage.hide());
        VBox buttonBox = new VBox(5, mMoveUpButton, mMoveDownButton, mResetButton, spacer, closeButton);

        HBox listBox = new HBox(10, mListView, buttonBox);
        HBox.setHgrow(mListView, Priority.ALWAYS);
        VBox.setVgrow(listBox, Priority.ALWAYS);

        Label instructions = new Label("Check the columns to show.  You can also drag column headers in the " +
            "table to change the column order.");
        instructions.setWrapText(true);
        instructions.setMaxWidth(320);

        VBox content = new VBox(10, instructions, listBox);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);

        Scene scene = new Scene(content);
        ThemeManager.getInstance().register(scene);
        stage.setScene(scene);
        stage.setOnShown(event -> updateButtons());

        //Reset tracks whether the table has the default columns, so update it when visibility changes too
        for(TableColumn<?,?> column: mTableColumnMonitor.getDefaultOrder())
        {
            column.visibleProperty().addListener((observable, oldValue, newValue) -> updateButtons());
        }

        return stage;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void move(int direction)
    {
        TableColumn column = mListView.getSelectionModel().getSelectedItem();

        if(column != null && ((TableColumnMonitor)mTableColumnMonitor).move(column, direction))
        {
            mListView.getSelectionModel().select(column);
            mListView.scrollTo(column);
        }
    }

    private void updateButtons()
    {
        int index = mListView.getSelectionModel().getSelectedIndex();
        mMoveUpButton.setDisable(index <= 0);
        mMoveDownButton.setDisable(index < 0 || index >= mListView.getItems().size() - 1);
        mResetButton.setDisable(mTableColumnMonitor.isDefault());
    }
}
