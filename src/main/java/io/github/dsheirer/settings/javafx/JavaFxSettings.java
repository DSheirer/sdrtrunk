/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.settings.javafx;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.decoder.JmbeLibrarySettings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages settings for JavaFX elements (e.g. stages).
 */
public class JavaFxSettings
{
    private final static Logger mLog = LoggerFactory.getLogger(JavaFxSettings.class);

    private List<StageMonitor> mStageMonitors = new ArrayList<>();

    public JavaFxSettings()
    {
    }

    /**
     * Monitors a stage size and location so that it can be restored to the same location on application startup.
     * @param stage to monitor
     * @param window that identifies the stored size and location
     */
    public void monitor(Stage stage, Setting.Window window)
    {
        mStageMonitors.add(new StageMonitor(stage, window.getKey()));
    }

    /**
     * Indicates if a size and location is stored for the window.
     */
    public boolean hasStoredLocation(Setting.Window window)
    {
        return Setting.JAVAFX_STAGE_WIDTH.with(window.getKey()).get(null) != null;
    }

    /**
     * Stores a size and location for the window, for example to migrate a window's location from an older
     * setting.  Call before {@link #monitor(Stage, Setting.Window)}.
     */
    public void storeLocation(Setting.Window window, double x, double y, double width, double height, boolean maximized)
    {
        Setting.JAVAFX_STAGE_X.with(window.getKey()).putDouble(x);
        Setting.JAVAFX_STAGE_Y.with(window.getKey()).putDouble(y);
        Setting.JAVAFX_STAGE_WIDTH.with(window.getKey()).putDouble(width);
        Setting.JAVAFX_STAGE_HEIGHT.with(window.getKey()).putDouble(height);
        Setting.JAVAFX_STAGE_MAXIMIZED.with(window.getKey()).putBoolean(maximized);
    }

    /**
     * Removes monitoring for the specified stage
     */
    public void unmonitor(Stage stage)
    {
        Iterator<StageMonitor> it = mStageMonitors.iterator();

        while(it.hasNext())
        {
            StageMonitor next = it.next();

            if(next.equals(stage))
            {
                it.remove();
                next.dispose();
            }
        }
    }

    /**
     * Removes all stage monitors and prepares for shutdown.
     */
    public void clearStageMonitors()
    {
        Iterator<StageMonitor> it = mStageMonitors.iterator();

        while(it.hasNext())
        {
            StageMonitor next = it.next();
            it.remove();
            next.dispose();
        }
    }

    /**
     * Monitors a stage's coordinates and stores location.  On construction, applies stored coordinates to the stage.
     */
    public class StageMonitor
    {
        private Stage mStage;
        private String mKey;
        private NumberMonitor mX;
        private NumberMonitor mY;
        private NumberMonitor mHeight;
        private NumberMonitor mWidth;
        private BooleanMonitor mMaximized;

        public StageMonitor(Stage stage, String key)
        {
            mStage = stage;
            mKey = key;

            //Position the stage before we add the coordinate monitoring
            reposition(stage);

            mX = new NumberMonitor(Setting.JAVAFX_STAGE_X.with(key), stage);
            mY = new NumberMonitor(Setting.JAVAFX_STAGE_Y.with(key), stage);
            mHeight = new NumberMonitor(Setting.JAVAFX_STAGE_HEIGHT.with(key), stage);
            mWidth = new NumberMonitor(Setting.JAVAFX_STAGE_WIDTH.with(key), stage);
            mMaximized = new BooleanMonitor(Setting.JAVAFX_STAGE_MAXIMIZED.with(key));

            mStage.xProperty().addListener(mX);
            mStage.yProperty().addListener(mY);
            mStage.heightProperty().addListener(mHeight);
            mStage.widthProperty().addListener(mWidth);
            mStage.maximizedProperty().addListener(mMaximized);
        }

        public void dispose()
        {
            mStage.xProperty().removeListener(mX);
            mStage.yProperty().removeListener(mY);
            mStage.heightProperty().removeListener(mHeight);
            mStage.widthProperty().removeListener(mWidth);
            mStage.maximizedProperty().removeListener(mMaximized);
            mStage = null;
        }

        /**
         * Repositions a stage to the last stored display location if there is currently a monitor that can display
         * those coordinates.  Otherwise, uses the stage's default coordinates.
         *
         * @param stage to reposition
         */
        private void reposition(Stage stage)
        {
            double x = Setting.JAVAFX_STAGE_X.with(mKey).getDouble(stage.getX());
            double y = Setting.JAVAFX_STAGE_Y.with(mKey).getDouble(stage.getY());
            double height = Setting.JAVAFX_STAGE_HEIGHT.with(mKey).getDouble(stage.getHeight());
            double width = Setting.JAVAFX_STAGE_WIDTH.with(mKey).getDouble(stage.getWidth());
            boolean maximized = Setting.JAVAFX_STAGE_MAXIMIZED.with(mKey).getBoolean(stage.isMaximized());

            //If there is a screen (ie monitor) available to display the stage at these coordinates, then move the
            // stage, otherwise let the stage display in the default coordinates
            ObservableList<Screen> screens = Screen.getScreensForRectangle(x, y, width, height);

            if(!screens.isEmpty())
            {
                stage.setX(x);
                stage.setY(y);
                stage.setHeight(height);
                stage.setWidth(width);
                stage.setMaximized(maximized);
            }
        }
    }

    /**
     * Monitors a stage coordinate and stores the value to settings as it changes.
     */
    public class NumberMonitor implements ChangeListener<Number>
    {
        private Setting.Value mValue;
        private Stage mStage;

        /**
         * Constructs an instance
         * @param value that stores the coordinate
         * @param stage that is monitored.  Values aren't stored while the stage is maximized, so that the stage
         * returns to its normal size and location when the user restores it.
         */
        public NumberMonitor(Setting.Value value, Stage stage)
        {
            mValue = value;
            mStage = stage;
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            if(newValue != null && (mStage == null || !mStage.isMaximized()))
            {
                mValue.putDouble(newValue.doubleValue());
            }
        }
    }

    public class BooleanMonitor implements ChangeListener<Boolean>
    {
        private Setting.Value mValue;

        public BooleanMonitor(Setting.Value value)
        {
            mValue = value;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            mValue.putBoolean(newValue);
        }
    }
}
