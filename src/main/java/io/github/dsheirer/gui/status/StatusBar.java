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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.monitor.ResourceMonitor;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Main window status bar.  Service items (streaming, tuners, channels) are on the left and resource items (CPU,
 * memory, storage) on the right.  The context menu shows or hides each item.
 *
 * The compact view shows the service items' status dots only.  The resource monitor only runs while a resource
 * item is showing.
 */
public class StatusBar extends HBox
{
    private final ResourceMonitor mResourceMonitor;
    private final List<StatusItem> mServiceItems;
    private final List<StatusItem> mResourceItems;
    private final ContextMenu mContextMenu = new ContextMenu();
    private final Region mSpacer = new Region();
    private Runnable mHideAction;
    private boolean mActive;
    private boolean mCompact;

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param resourceMonitor to start while a resource item is showing
     * @param serviceItems for the left side, in order
     * @param resourceItems for the right side, in order
     */
    public StatusBar(ResourceMonitor resourceMonitor, List<StatusItem> serviceItems, List<StatusItem> resourceItems)
    {
        mResourceMonitor = resourceMonitor;
        mServiceItems = List.copyOf(serviceItems);
        mResourceItems = List.copyOf(resourceItems);
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mSpacer, Priority.ALWAYS);
        mSpacer.setMinWidth(8);

        for(StatusItem item: getItems())
        {
            item.shownProperty().addListener((observable, oldValue, shown) -> updateItems());
        }

        setOnContextMenuRequested(event -> {
            updateContextMenu();
            mContextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        updateItems();
    }

    /**
     * All items: service items, then resource items.
     */
    public List<StatusItem> getItems()
    {
        List<StatusItem> items = new ArrayList<>(mServiceItems);
        items.addAll(mResourceItems);
        return items;
    }

    /**
     * Action for the context menu's Hide Status Bar item.
     */
    public void setHideAction(Runnable action)
    {
        mHideAction = action;
    }

    /**
     * Sets whether the status bar is in the window and whether the window shows the compact view.  Starts or stops
     * the resource monitor to match.  JavaFX thread only.
     * @param active true when the status bar is in the window
     * @param compact true for the compact view
     */
    public void setMode(boolean active, boolean compact)
    {
        mActive = active;

        if(mCompact != compact)
        {
            mCompact = compact;

            for(StatusItem item: getItems())
            {
                item.setCompact(compact);
            }

            updateItems();
        }

        updateMonitoring();
    }

    /**
     * Indicates if the bar shows the compact view.
     */
    public boolean isCompact()
    {
        return mCompact;
    }

    /**
     * Stops the items listening to their models and stops the resource monitor.  JavaFX thread only.
     */
    public void dispose()
    {
        mActive = false;
        updateMonitoring();

        for(StatusItem item: getItems())
        {
            item.dispose();
        }
    }

    /**
     * Lays out the shown items: service items, a spacer, then resource items, with separators between them.  The
     * compact view leaves out the resource items.
     */
    private void updateItems()
    {
        List<Node> children = new ArrayList<>();
        addGroup(children, mServiceItems);
        children.add(mSpacer);

        if(!mCompact)
        {
            addGroup(children, mResourceItems);
        }

        getChildren().setAll(children);
        updateMonitoring();
    }

    private void addGroup(List<Node> children, List<StatusItem> items)
    {
        boolean first = true;

        for(StatusItem item: items)
        {
            if(item.shownProperty().get())
            {
                if(!first && !mCompact)
                {
                    Separator separator = new Separator(Orientation.VERTICAL);
                    separator.getStyleClass().add("status-separator");
                    children.add(separator);
                }

                children.add(item);
                first = false;
            }
        }
    }

    /**
     * Runs the resource monitor only while a resource item is showing.
     */
    private void updateMonitoring()
    {
        boolean needed = mActive && !mCompact && mResourceItems.stream().anyMatch(item -> item.shownProperty().get());

        if(needed)
        {
            mResourceMonitor.start();
        }
        else
        {
            mResourceMonitor.stop();
        }
    }

    private void updateContextMenu()
    {
        mContextMenu.getItems().clear();
        addMenuItems(mServiceItems);
        mContextMenu.getItems().add(new SeparatorMenuItem());
        addMenuItems(mResourceItems);

        if(mHideAction != null)
        {
            mContextMenu.getItems().add(new SeparatorMenuItem());
            MenuItem hide = new MenuItem("Hide Status Bar");
            hide.setOnAction(event -> mHideAction.run());
            mContextMenu.getItems().add(hide);
        }
    }

    private void addMenuItems(List<StatusItem> items)
    {
        for(StatusItem item: items)
        {
            CheckMenuItem menuItem = new CheckMenuItem(item.getTitle());
            menuItem.setSelected(item.shownProperty().get());
            menuItem.selectedProperty().addListener((observable, oldValue, selected) ->
                item.shownProperty().set(selected));
            //The compact view doesn't show the resource items, so their choices wait for the normal view
            menuItem.setDisable(mCompact && !item.isService());
            mContextMenu.getItems().add(menuItem);
        }
    }
}
