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

import io.github.dsheirer.settings.Setting;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * Item in the main window's status bar.
 *
 * Service items (streaming, tuners, channels) sit on the left and show a status dot.  They stay in the compact view,
 * as the dot only.  Resource items (CPU, memory, storage) sit on the right and are left out of the compact view.
 *
 * Each item can be shown or hidden from the status bar's context menu, and remembers that choice.
 */
public abstract class StatusItem extends HBox
{
    private final String mTitle;
    private final Setting mShownSetting;
    private final boolean mService;
    private final BooleanProperty mShown = new SimpleBooleanProperty();
    private final AtomicBoolean mUpdateScheduled = new AtomicBoolean();
    private final Tooltip mTooltip = new Tooltip();
    private Runnable mAction;
    private boolean mCompact;

    /**
     * Constructs an instance.
     * @param title for the status bar's context menu
     * @param shownSetting that stores whether the item is shown
     * @param service true for a service item, false for a resource item
     */
    protected StatusItem(String title, Setting shownSetting, boolean service)
    {
        mTitle = title;
        mShownSetting = shownSetting;
        mService = service;
        mShown.set(shownSetting.getBoolean(true));
        mShown.addListener((observable, oldValue, shown) -> mShownSetting.putBoolean(shown));
        getStyleClass().add("status-item");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(5);
        mTooltip.setShowDelay(Duration.millis(300));
        mTooltip.setShowDuration(Duration.INDEFINITE);
        Tooltip.install(this, mTooltip);

        //A filter, not a handler, since the resource items' progress bar consumes the clicks on it
        addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if(event.getButton() == MouseButton.PRIMARY && mAction != null)
            {
                mAction.run();
                event.consume();
            }
        });
    }

    /**
     * Title for the status bar's context menu.
     */
    public String getTitle()
    {
        return mTitle;
    }

    /**
     * Shown state, stored in the settings.
     */
    public BooleanProperty shownProperty()
    {
        return mShown;
    }

    /**
     * Indicates a service item, which shows a status dot and stays in the compact view.
     */
    public boolean isService()
    {
        return mService;
    }

    /**
     * Action for a click on the item, or null for none.
     */
    public void setAction(Runnable action)
    {
        mAction = action;
        setCursor(action != null ? Cursor.HAND : Cursor.DEFAULT);

        if(action != null && !getStyleClass().contains("status-clickable"))
        {
            getStyleClass().add("status-clickable");
        }
        else if(action == null)
        {
            getStyleClass().remove("status-clickable");
        }
    }

    /**
     * Shows the item for the compact view, where service items show their status dot only.
     */
    public void setCompact(boolean compact)
    {
        if(mCompact != compact)
        {
            mCompact = compact;
            compactChanged(compact);
        }
    }

    /**
     * Indicates if the item shows the compact view.
     */
    protected boolean isCompact()
    {
        return mCompact;
    }

    /**
     * Updates the item for the compact view.  Does nothing by default.
     */
    protected void compactChanged(boolean compact)
    {
    }

    /**
     * Sets the tooltip text.  JavaFX thread only.
     */
    protected void setTooltipText(String text)
    {
        if(!text.equals(mTooltip.getText()))
        {
            mTooltip.setText(text);
        }
    }

    /**
     * Schedules an update on the JavaFX thread.  Any thread.  Changes often arrive several at a time from the
     * decoder and tuner threads, so the updates are coalesced.
     */
    protected void scheduleUpdate()
    {
        if(mUpdateScheduled.compareAndSet(false, true))
        {
            Platform.runLater(() -> {
                mUpdateScheduled.set(false);
                update();
            });
        }
    }

    /**
     * Updates the item from its model.  JavaFX thread only.
     */
    protected abstract void update();

    /**
     * Stops listening to the model.  JavaFX thread only.
     */
    public void dispose()
    {
    }
}
