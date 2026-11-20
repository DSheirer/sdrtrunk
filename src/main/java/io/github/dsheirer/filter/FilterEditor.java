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

package io.github.dsheirer.filter;

import io.github.dsheirer.gui.ApplicationIcons;
import io.github.dsheirer.gui.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Window for turning filters and filter elements on and off.  Each filter shows how many of its elements are
 * enabled.  Checking a filter turns all of its elements on or off.  Changes apply immediately.
 *
 * @param <T> type of item that is filtered
 */
public class FilterEditor<T>
{
    private final Stage mStage = new Stage();
    private final TreeView<Object> mTreeView = new TreeView<>();

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param title for the window
     * @param owner window, or null
     * @param filterSet to edit
     */
    public FilterEditor(String title, Window owner, FilterSet<T> filterSet)
    {
        if(filterSet == null)
        {
            throw new IllegalArgumentException("Unable to construct FilterEditor - FilterSet cannot be null");
        }

        mTreeView.setShowRoot(true);
        mTreeView.setCellFactory(CheckBoxTreeCell.forTreeView(item -> ((CheckBoxTreeItem<Object>)item)
            .selectedProperty(), new LabelConverter()));
        VBox.setVgrow(mTreeView, Priority.ALWAYS);

        Button expand = new Button("Expand All");
        expand.setOnAction(event -> setExpanded(mTreeView.getRoot(), true));
        Button collapse = new Button("Collapse All");
        collapse.setOnAction(event -> {
            setExpanded(mTreeView.getRoot(), false);
            mTreeView.getRoot().setExpanded(true);
        });
        Button close = new Button("Close");
        close.setCancelButton(true);
        close.setDefaultButton(true);
        close.setOnAction(event -> mStage.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, expand, collapse, spacer, close);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(8, mTreeView, buttons);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 500);
        ThemeManager.getInstance().register(scene);
        mStage.setScene(scene);
        mStage.setTitle(title);
        ApplicationIcons.apply(mStage);

        if(owner != null)
        {
            mStage.initOwner(owner);
        }

        updateFilterSet(filterSet);
    }

    /**
     * Shows the window and brings it to the front.  JavaFX thread only.
     */
    public void show()
    {
        mStage.show();
        mStage.toFront();
    }

    /**
     * Updates this editor with the filter set.  JavaFX thread only.
     * @param filterSet to edit
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        CheckBoxTreeItem<Object> root = new CheckBoxTreeItem<>(filterSet);
        root.setExpanded(true);
        addFilterSet(filterSet, root);
        mTreeView.setRoot(root);
    }

    /**
     * Tree view, for testing.
     */
    TreeView<Object> getTreeView()
    {
        return mTreeView;
    }

    private void addFilterSet(FilterSet<?> filterSet, CheckBoxTreeItem<Object> parent)
    {
        for(IFilter<?> filter: filterSet.getFilters())
        {
            CheckBoxTreeItem<Object> item = new CheckBoxTreeItem<>(filter);
            parent.getChildren().add(item);

            if(filter instanceof FilterSet<?> childFilterSet)
            {
                addFilterSet(childFilterSet, item);
            }
            else if(filter instanceof Filter<?,?> childFilter)
            {
                addFilter(childFilter, item);
            }
        }
    }

    private void addFilter(Filter<?,?> filter, CheckBoxTreeItem<Object> parent)
    {
        List<FilterElement<?>> elements = new ArrayList<>(filter.getFilterElements());
        elements.sort(Comparator.comparing(FilterElement::getName));

        for(FilterElement<?> element: elements)
        {
            CheckBoxTreeItem<Object> item = new CheckBoxTreeItem<>(element);
            parent.getChildren().add(item);

            //Set the state after adding the item, so that the parent check boxes follow their elements
            item.setSelected(element.isEnabled());
            item.selectedProperty().addListener((observable, wasSelected, selected) -> {
                element.setEnabled(selected);
                //Update the enabled counts shown on the filters
                mTreeView.refresh();
            });
        }
    }

    private static void setExpanded(TreeItem<Object> item, boolean expanded)
    {
        if(item != null)
        {
            item.setExpanded(expanded);
            item.getChildren().forEach(child -> setExpanded(child, expanded));
        }
    }

    /**
     * Label for each tree item: a filter's name and enabled count, or a filter element's name.
     */
    private static class LabelConverter extends StringConverter<TreeItem<Object>>
    {
        @Override
        public String toString(TreeItem<Object> item)
        {
            Object value = item != null ? item.getValue() : null;

            if(value instanceof IFilter<?> filter)
            {
                return filter.getName() + "  (" + filter.getEnabledCount() + "/" + filter.getElementCount() + ")";
            }
            else if(value instanceof FilterElement<?> element)
            {
                return element.getName();
            }

            return value != null ? value.toString() : "";
        }

        @Override
        public TreeItem<Object> fromString(String string)
        {
            return null;
        }
    }
}
