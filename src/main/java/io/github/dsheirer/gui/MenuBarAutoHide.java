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

package io.github.dsheirer.gui;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Hides a window's menu bar until the user asks for it.  When enabled, the menu bar is hidden and is revealed by
 * pressing and releasing the Alt key on its own, which also opens the first menu for keyboard use, or by moving the
 * mouse to the top edge of the window.  It hides again shortly after its menus close and the mouse moves away.
 *
 * Menu item accelerators keep working while the menu bar is hidden.  Not for a menu bar that uses the system menu
 * bar (macOS), since that menu bar isn't part of the window.  JavaFX thread only.
 */
public class MenuBarAutoHide
{
    private static final double REVEAL_EDGE = 4.0;
    private static final double HIDE_MARGIN = 12.0;

    private final MenuBar mMenuBar;
    private final BooleanProperty mEnabled = new SimpleBooleanProperty(false);
    private final PauseTransition mHideDelay = new PauseTransition(Duration.millis(500));
    private boolean mRevealed;
    private boolean mAltAlone;
    //Last mouse position over the window, or infinity when it's elsewhere or unknown
    private double mMouseY = Double.POSITIVE_INFINITY;

    /**
     * Constructs an instance.
     * @param menuBar to hide
     * @param scene that holds the menu bar, for the key and mouse handling
     */
    public MenuBarAutoHide(MenuBar menuBar, Scene scene)
    {
        mMenuBar = menuBar;
        mHideDelay.setOnFinished(event -> hideIfIdle());
        mEnabled.addListener((observable, oldValue, enabled) -> {
            mRevealed = false;
            update();
        });

        for(Menu menu: menuBar.getMenus())
        {
            menu.showingProperty().addListener((observable, oldValue, showing) -> {
                if(!showing)
                {
                    scheduleHide();
                }
            });
        }

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> mAltAlone = event.getCode() == KeyCode.ALT);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> mAltAlone = false);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::keyReleased);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> {
            if(event.getTarget() == scene.getRoot())
            {
                mMouseY = Double.POSITIVE_INFINITY;
                scheduleHide();
            }
        });

        update();
    }

    /**
     * Enables auto-hide.
     */
    public BooleanProperty enabledProperty()
    {
        return mEnabled;
    }

    /**
     * Indicates if the menu bar is currently shown.
     */
    public boolean isMenuBarShown()
    {
        return mMenuBar.isVisible();
    }

    private void keyReleased(KeyEvent event)
    {
        if(event.getCode() != KeyCode.ALT || !mAltAlone || !mEnabled.get())
        {
            return;
        }

        mAltAlone = false;
        event.consume();

        //An open menu has the keyboard focus in its own popup, where Escape closes it
        if(!isMenuShowing() && !mMenuBar.getMenus().isEmpty())
        {
            reveal();
            //Open the first menu once the menu bar has a position, so the arrow keys move through the menus
            javafx.application.Platform.runLater(() -> mMenuBar.getMenus().get(0).show());
        }
    }

    private void mouseMoved(MouseEvent event)
    {
        mMouseY = event.getSceneY();

        if(!mEnabled.get())
        {
            return;
        }

        if(!mRevealed)
        {
            if(event.getSceneY() <= REVEAL_EDGE)
            {
                reveal();
            }
        }
        else if(!isMouseNear())
        {
            scheduleHide();
        }
        else
        {
            mHideDelay.stop();
        }
    }

    private void reveal()
    {
        mHideDelay.stop();
        mRevealed = true;
        update();
    }

    private void scheduleHide()
    {
        if(mEnabled.get() && mRevealed)
        {
            mHideDelay.playFromStart();
        }
    }

    private void hideIfIdle()
    {
        if(mEnabled.get() && mRevealed && !isMenuShowing() && !isMouseNear())
        {
            mRevealed = false;
            update();
        }
    }

    /**
     * Indicates if the mouse was last seen over or just below the menu bar.  Uses the position from the mouse
     * events instead of the hover state, which only updates when the mouse moves.
     */
    private boolean isMouseNear()
    {
        return mMouseY <= mMenuBar.getHeight() + HIDE_MARGIN;
    }

    private boolean isMenuShowing()
    {
        return mMenuBar.getMenus().stream().anyMatch(Menu::isShowing);
    }

    private void update()
    {
        boolean shown = !mEnabled.get() || mRevealed;
        mMenuBar.setVisible(shown);
        mMenuBar.setManaged(shown);
    }
}
