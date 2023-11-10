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

package io.github.dsheirer.preference.javafx;

import io.github.dsheirer.preference.decoder.JmbeLibraryPreference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages user preferences for JavaFX elements (e.g. stages).
 */
public class JavaFxPreferences
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibraryPreference.class);
    private static final String PREFERENCE_STAGE_HEIGHT_PREFIX = "stage.height.";
    private static final String PREFERENCE_STAGE_WIDTH_PREFIX = "stage.width.";
    private static final String PREFERENCE_STAGE_X_PREFIX = "stage.x.";
    private static final String PREFERENCE_STAGE_Y_PREFIX = "stage.y.";
    private static final String PREFERENCE_STAGE_MAXIMIZED = "state.maximized";

    private Preferences mPreferences = Preferences.userNodeForPackage(JavaFxPreferences.class);
    private List<StageMonitor> mStageMonitors = new ArrayList<>();

    public JavaFxPreferences()
    {
    }

    /**
     * Monitors a stage size and location so that it can be restored to the same location on application startup.
     * @param stage to monitor
     * @param key to label corresponding preference settings
     */
    public void monitor(Stage stage, String key)
    {
        mStageMonitors.add(new StageMonitor(stage, key));
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

            mX = new NumberMonitor(PREFERENCE_STAGE_X_PREFIX + key);
            mY = new NumberMonitor(PREFERENCE_STAGE_Y_PREFIX + key);
            mHeight = new NumberMonitor(PREFERENCE_STAGE_HEIGHT_PREFIX + key);
            mWidth = new NumberMonitor(PREFERENCE_STAGE_WIDTH_PREFIX + key);
            mMaximized = new BooleanMonitor(PREFERENCE_STAGE_MAXIMIZED + key);

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
            double x = mPreferences.getDouble(PREFERENCE_STAGE_X_PREFIX + mKey, stage.getX());
            double y = mPreferences.getDouble(PREFERENCE_STAGE_Y_PREFIX + mKey, stage.getY());
            double height = mPreferences.getDouble(PREFERENCE_STAGE_HEIGHT_PREFIX + mKey, stage.getHeight());
            double width = mPreferences.getDouble(PREFERENCE_STAGE_WIDTH_PREFIX + mKey, stage.getWidth());
            boolean maximized = mPreferences.getBoolean(PREFERENCE_STAGE_MAXIMIZED, stage.isMaximized());

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
     * Monitors a stage coordinate and stores the value to user preferences as it changes.
     */
    public class NumberMonitor implements ChangeListener<Number>
    {
        private String mKey;

        public NumberMonitor(String key)
        {
            mKey = key;
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            if(newValue != null)
            {
                mPreferences.putDouble(mKey, newValue.doubleValue());
            }
        }
    }

    public class BooleanMonitor implements ChangeListener<Boolean>
    {
        private String mKey;

        public BooleanMonitor(String key)
        {
            mKey = key;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            mPreferences.putBoolean(mKey, newValue);
        }
    }
}
