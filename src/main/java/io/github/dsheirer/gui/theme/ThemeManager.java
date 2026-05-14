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

package io.github.dsheirer.gui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.common.eventbus.Subscribe;
import com.jidesoft.plaf.LookAndFeelFactory;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import java.awt.EventQueue;
import java.awt.Window;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.Scene;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized light/dark theme manager.
 *
 * <p>Tracks every JavaFX {@link Scene} that opts in and applies (or removes) the dark stylesheet
 * when the user toggles the {@code Dark Mode} preference.  Also drives Swing look-and-feel
 * via {@link FlatDarkLaf} / {@link FlatLightLaf} so the main Swing window and all child windows
 * follow the same setting.
 *
 * <p>Subscribes to the global event bus for {@link PreferenceType#APPLICATION} updates.
 */
public class ThemeManager
{
    private final static Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String DARK_STYLESHEET = "/sdrtrunk_dark.css";
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final Set<WeakReference<Scene>> mScenes = new LinkedHashSet<>();
    private final String mDarkStylesheetUrl;
    private volatile UserPreferences mUserPreferences;
    private volatile boolean mDarkMode;

    private ThemeManager()
    {
        java.net.URL url = ThemeManager.class.getResource(DARK_STYLESHEET);
        mDarkStylesheetUrl = (url != null) ? url.toExternalForm() : null;

        if(mDarkStylesheetUrl == null)
        {
            mLog.warn("Dark stylesheet [{}] not found on classpath - dark mode will only affect Swing.",
                    DARK_STYLESHEET);
        }
    }

    /**
     * @return the singleton instance.
     */
    public static ThemeManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Bind the theme manager to the user preferences and apply the current preference immediately.
     * Safe to call multiple times - subsequent calls update the reference and re-apply.
     *
     * @param userPreferences source of the dark-mode preference.
     */
    public void initialize(UserPreferences userPreferences)
    {
        if(userPreferences == null)
        {
            return;
        }

        boolean firstBind = (mUserPreferences == null);
        mUserPreferences = userPreferences;
        mDarkMode = userPreferences.getApplicationPreference().isDarkMode();

        if(firstBind)
        {
            MyEventBus.getGlobalEventBus().register(this);
        }

        applySwingLookAndFeel(mDarkMode);
    }

    /**
     * @return current dark-mode state.
     */
    public boolean isDarkMode()
    {
        return mDarkMode;
    }

    /**
     * Register a Scene to receive theme updates.  The Scene is held via {@link WeakReference}
     * so callers do not need to unregister - garbage-collected Scenes are pruned on the next pass.
     *
     * @param scene to track.
     */
    public void register(Scene scene)
    {
        if(scene == null)
        {
            return;
        }

        synchronized(mScenes)
        {
            pruneDeadReferencesAndCheckPresent(scene);
            mScenes.add(new WeakReference<>(scene));
        }

        applyToScene(scene, mDarkMode);
    }

    /**
     * Receives notification when any preference changes.  Re-reads the dark-mode setting and
     * propagates if it differs from the last known value.
     */
    @Subscribe
    public void onPreferenceUpdated(PreferenceType type)
    {
        if(type != PreferenceType.APPLICATION || mUserPreferences == null)
        {
            return;
        }

        boolean updated = mUserPreferences.getApplicationPreference().isDarkMode();

        if(updated == mDarkMode)
        {
            return;
        }

        mDarkMode = updated;
        applyAll(updated);
    }

    private void applyAll(boolean darkMode)
    {
        applySwingLookAndFeel(darkMode);

        Runnable fxApply = () -> {
            synchronized(mScenes)
            {
                Iterator<WeakReference<Scene>> it = mScenes.iterator();
                while(it.hasNext())
                {
                    Scene scene = it.next().get();
                    if(scene == null)
                    {
                        it.remove();
                    }
                    else
                    {
                        applyToScene(scene, darkMode);
                    }
                }
            }

            //Also walk every currently-shown JavaFX window so that dialogs, alerts and
            //popups that were not explicitly registered still pick up the theme.
            for(javafx.stage.Window window: javafx.stage.Window.getWindows())
            {
                Scene scene = window.getScene();
                if(scene != null)
                {
                    applyToScene(scene, darkMode);
                }
            }
        };

        if(Platform.isFxApplicationThread())
        {
            fxApply.run();
        }
        else
        {
            try
            {
                Platform.runLater(fxApply);
            }
            catch(IllegalStateException ignored)
            {
                //FX toolkit not started yet - scenes will be themed at registration time instead.
            }
        }
    }

    private void applyToScene(Scene scene, boolean darkMode)
    {
        if(mDarkStylesheetUrl == null)
        {
            return;
        }

        Runnable r = () -> {
            scene.getStylesheets().remove(mDarkStylesheetUrl);
            if(darkMode)
            {
                scene.getStylesheets().add(mDarkStylesheetUrl);
            }
        };

        if(Platform.isFxApplicationThread())
        {
            r.run();
        }
        else
        {
            Platform.runLater(r);
        }
    }

    private void applySwingLookAndFeel(boolean darkMode)
    {
        Runnable r = () -> {
            try
            {
                if(darkMode)
                {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                }
                else
                {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }
            }
            catch(Exception e)
            {
                mLog.error("Unable to apply Swing look-and-feel for dark mode = " + darkMode, e);
                return;
            }

            try
            {
                //JIDE components (split panes, docking) need their UI delegates re-registered
                //after every LAF change.
                LookAndFeelFactory.installJideExtension();
            }
            catch(Exception e)
            {
                mLog.warn("Unable to install JIDE LAF extension after theme change", e);
            }

            for(Window window: Window.getWindows())
            {
                SwingUtilities.updateComponentTreeUI(window);
            }
        };

        if(EventQueue.isDispatchThread())
        {
            r.run();
        }
        else
        {
            EventQueue.invokeLater(r);
        }
    }

    private void pruneDeadReferencesAndCheckPresent(Scene scene)
    {
        Iterator<WeakReference<Scene>> it = mScenes.iterator();
        while(it.hasNext())
        {
            Scene existing = it.next().get();
            if(existing == null)
            {
                it.remove();
            }
            else if(existing == scene)
            {
                it.remove();
            }
        }
    }
}
