/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.channelMap.ChannelMapEditor;
import io.github.dsheirer.gui.channelMap.ChannelMapEditorViewRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorViewRequest;
import io.github.dsheirer.gui.preference.PreferencesEditor;
import io.github.dsheirer.gui.radioreference.LoginDialog;
import io.github.dsheirer.gui.radioreference.LoginDialogViewRequest;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java FX window manager.  Handles all secondary Java FX windows that are used within this primarily
 * Swing application.
 */
public class JavaFxWindowManager
{
    private final static Logger mLog = LoggerFactory.getLogger(JavaFxWindowManager.class);

    private JFXPanel mJFXPanel;
    private UserPreferences mUserPreferences;
    private ChannelMapModel mChannelMapModel;
    private PreferencesEditor mPreferencesEditor;
    private LoginDialog mLoginDialog;
    private ChannelMapEditor mChannelMapEditor;

    public JavaFxWindowManager(UserPreferences userPreferences, ChannelMapModel channelMapModel)
    {
        mUserPreferences = userPreferences;
        mChannelMapModel = channelMapModel;

        //Register this class to receive events via each method annotated with @Subscribe
        MyEventBus.getEventBus().register(this);
    }

    private void createJFXPanel()
    {
        if(mJFXPanel == null)
        {
            mJFXPanel = new JFXPanel();
        }
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
        if(mPreferencesEditor == null)
        {
            createJFXPanel();
            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                try
                {
                    mPreferencesEditor = new PreferencesEditor(mUserPreferences);

                    Stage stage = new Stage();
                    stage.setOnHidden(event -> mPreferencesEditor = null);
                    mPreferencesEditor.start(stage);
                    mPreferencesEditor.showEditor(request);
                }
                catch(Throwable e)
                {
                    mLog.error("Error launching user preferences window", e);
                }
            });
        }
        else
        {
            Platform.runLater(() -> {
                try
                {
                    Stage stage = mPreferencesEditor.getStage();
                    stage.show();
                    mPreferencesEditor.showEditor(request);
                }
                catch(Throwable t)
                {
                    mLog.error("Error showing existing preferences editor window", t);
                }
            });
        }
    }

    /**
     * Processes a request to show the radioreference.com login credentials editor
     * @param request to view with a callback listener to receive the login credentials
     */
    @Subscribe
    public void process(final LoginDialogViewRequest request)
    {
        if(mLoginDialog != null)
        {
            mLoginDialog.show();
        }
        else
        {
            createJFXPanel();
            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                try
                {
                    mLoginDialog = new LoginDialog(mUserPreferences, request.getListener());

                    Stage stage = new Stage();
                    stage.setOnHidden(event -> mLoginDialog = null);
                    mLoginDialog.start(stage);
                }
                catch(Throwable e)
                {
                    mLog.error("Error launching user preferences window", e);
                }
            });
        }
    }

    @Subscribe
    public void process(final ChannelMapEditorViewRequest request)
    {
        if(mChannelMapEditor != null)
        {
            mChannelMapEditor.show(request.getChannelMapName());
        }
        else
        {
            createJFXPanel();
            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                try
                {
                    mChannelMapEditor = new ChannelMapEditor(mChannelMapModel);
                    Stage stage = new Stage();
                    mChannelMapEditor.start(stage);

                    if(request.hasChannelMapName())
                    {
                        System.out.println("Requesting show channel map: " + request.getChannelMapName());
                        mChannelMapEditor.show(request.getChannelMapName());
                    }
                }
                catch(Throwable e)
                {
                    mLog.error("Error launching channel map editor window", e);
                }
            });
        }
    }
}
