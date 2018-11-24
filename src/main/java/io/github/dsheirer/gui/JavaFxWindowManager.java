/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.gui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.gui.preference.PreferenceEditorViewRequest;
import io.github.dsheirer.gui.preference.PreferencesEditor;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class JavaFxWindowManager
{
    private final static Logger mLog = LoggerFactory.getLogger(JavaFxWindowManager.class);

    private static EventBus sEventBus = new EventBus();
    private UserPreferences mUserPreferences;
    private PreferencesEditor mPreferencesEditor;
    private JFXPanel mJFXPanel;

    public JavaFxWindowManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        //Register this class to receive events via each method annotated with @Subscribe
        sEventBus.register(this);
    }

    /**
     * Event bus for requesting java fx window coordination.
     */
    public static EventBus getEventBus()
    {
        return sEventBus;
    }

    /**
     * Closes all JavaFX windows and shutsdown the FX thread
     */
    public void shutdown()
    {
        Platform.exit();
    }

    @Subscribe
    public void process(final PreferenceEditorViewRequest request)
    {
        if(mJFXPanel == null)
        {
            mJFXPanel = new JFXPanel();
        }

        if(mPreferencesEditor == null)
        {
            mPreferencesEditor = new PreferencesEditor(mUserPreferences);

            Platform.runLater(() -> {
                try
                {
                    mPreferencesEditor.start(new Stage());
                }
                catch(Exception e)
                {
                    mLog.error("Error launching user preferences window");
                }
            });
        }

        mLog.debug("Showing ....");

        Platform.runLater(() -> {
            mLog.debug("Pref Window - null:" + (mPreferencesEditor == null));
            mLog.debug("Pref Window - showing:" + mPreferencesEditor.getStage().isShowing());
            mPreferencesEditor.getStage().show();
            mPreferencesEditor.getStage().toFront();
            mPreferencesEditor.showEditor(request);
        });
    }
}
