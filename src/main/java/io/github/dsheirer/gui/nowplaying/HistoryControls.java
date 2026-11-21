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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.filter.FilterEditor;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.gui.table.ColumnConfigurationButton;
import io.github.dsheirer.gui.table.TableColumnMonitor;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Toolbar for a history table: filter editor, clear, history size and column configuration controls.
 */
public class HistoryControls<T> extends HBox
{
    /**
     * Default number of items in the history
     */
    public static final int DEFAULT_HISTORY_SIZE = 200;
    private static final int MAXIMUM_HISTORY_SIZE = 2000;

    private final String mFilterEditorTitle;
    private final Button mFilterButton = new Button("Filters");
    private final Button mClearButton = new Button("Clear");
    private final Label mHistoryLabel = new Label("History:");
    private final Slider mHistorySlider = new Slider(0, MAXIMUM_HISTORY_SIZE, DEFAULT_HISTORY_SIZE);
    private final Label mHistoryValueLabel = new Label(String.valueOf(DEFAULT_HISTORY_SIZE));
    private FilterEditor<T> mFilterEditor;
    private FilterSet<T> mFilterSet;

    /**
     * Constructs an instance
     * @param filterEditorTitle for the filter editor window
     * @param clear action for the clear button
     * @param historySizeListener to receive history size changes
     * @param tableColumnMonitor for the column configuration button
     * @param columnsTitle for the column configuration editor
     */
    public HistoryControls(String filterEditorTitle, Runnable clear, IntConsumer historySizeListener,
                           TableColumnMonitor<?> tableColumnMonitor, String columnsTitle)
    {
        super(5);
        mFilterEditorTitle = filterEditorTitle;
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(3, 2, 3, 2));

        mFilterButton.setTooltip(new Tooltip("Edit filters"));
        mFilterButton.setOnAction(event -> showFilterEditor());

        mClearButton.setTooltip(new Tooltip("Clears the history"));
        mClearButton.setOnAction(event -> clear.run());

        mHistorySlider.setTooltip(new Tooltip("Adjust history size.  Double-click to reset to default " +
            DEFAULT_HISTORY_SIZE));
        mHistorySlider.setBlockIncrement(25);
        mHistorySlider.setPrefWidth(200);
        mHistorySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int size = newValue.intValue();
            mHistoryValueLabel.setText(String.valueOf(size));
            historySizeListener.accept(size);
        });
        mHistorySlider.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
            {
                mHistorySlider.setValue(DEFAULT_HISTORY_SIZE);
            }
        });
        mHistoryValueLabel.setMinWidth(40);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(mFilterButton, mClearButton, mHistoryLabel, mHistorySlider, mHistoryValueLabel, spacer,
            new ColumnConfigurationButton(tableColumnMonitor, columnsTitle));
        setControlsEnabled(false);
    }

    /**
     * Sets the enabled state of the history controls.  The column configuration button is always enabled.
     */
    public void setControlsEnabled(boolean enabled)
    {
        mFilterButton.setDisable(!enabled);
        mClearButton.setDisable(!enabled);
        mHistoryLabel.setDisable(!enabled);
        mHistorySlider.setDisable(!enabled);
        mHistoryValueLabel.setDisable(!enabled);
    }

    /**
     * Updates the filter set for the filter editor.
     * @param filterSet to edit, or null
     */
    public void setFilterSet(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;

        if(mFilterEditor != null && filterSet != null)
        {
            mFilterEditor.updateFilterSet(filterSet);
        }
    }

    /**
     * Shows the filter editor.
     */
    private void showFilterEditor()
    {
        FilterSet<T> filterSet = mFilterSet;

        if(filterSet != null)
        {
            if(mFilterEditor == null)
            {
                mFilterEditor = new FilterEditor<>(mFilterEditorTitle,
                    getScene() != null ? getScene().getWindow() : null, filterSet);
            }

            mFilterEditor.show();
        }
    }
}
