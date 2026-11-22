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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.Settings;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the user's theme and GUI scale to every JavaFX scene.
 *
 * <p>Each {@link Theme} has a palette and a dark/light flag.  The palette becomes a small stylesheet that sets
 * Modena's looked-up colors and the scaled root font size.  Dark themes also get the dark stylesheet.  Registered
 * scenes, and every JavaFX window that opens (including ad-hoc alerts and dialogs), are updated when the theme
 * changes.</p>
 */
public class ThemeManager
{
    private final static Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String DARK_STYLESHEET = "/sdrtrunk_dark.css";
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final Set<WeakReference<Scene>> mScenes = new LinkedHashSet<>();
    private final AtomicBoolean mWindowListenerAttached = new AtomicBoolean();
    private final String mDarkStylesheetUrl;
    private volatile Settings mSettings;
    private volatile Theme mCurrentTheme = Theme.LIGHT;
    private volatile double mCurrentGuiScale = 1.0d;
    private volatile boolean mComplementaryAliasColors = false;
    /** Data URL of the currently-active per-theme accent stylesheet, or null. */
    private volatile String mAccentStylesheetUrl;

    private ThemeManager()
    {
        java.net.URL url = ThemeManager.class.getResource(DARK_STYLESHEET);
        mDarkStylesheetUrl = (url != null) ? url.toExternalForm() : null;

        if(mDarkStylesheetUrl == null)
        {
            mLog.warn("Dark stylesheet [{}] not found on classpath - dark themes won't be applied.",
                    DARK_STYLESHEET);
        }
    }

    public static ThemeManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Bind the theme manager to the user preferences and apply the current preference immediately.
     * Safe to call multiple times - subsequent calls update the reference and re-apply.
     */
    public void initialize(Settings settings)
    {
        if(settings == null)
        {
            return;
        }

        boolean firstBind = (mSettings == null);
        mSettings = settings;
        mCurrentTheme = settings.getApplicationSettings().getTheme();
        mCurrentGuiScale = settings.getApplicationSettings().getGuiScale();
        mComplementaryAliasColors = settings.getApplicationSettings().isComplementaryAliasColors();

        if(firstBind)
        {
            MyEventBus.getGlobalEventBus().register(this);
            attachJavaFxWindowListener();
        }

        //Compute the per-theme accent stylesheet URL so it is ready before any Scene.register call.  At init
        //time the scene walk is a no-op because nothing has registered yet.
        applyAll(mCurrentTheme);
    }

    /**
     * Install a listener on {@link javafx.stage.Window#getWindows()} so that every JavaFX window
     * created in the application (including ad-hoc {@code Alert}, {@code TextInputDialog} and
     * other transient dialogs spawned by the playlist editor and similar) automatically picks up
     * the current theme without each call site needing to remember to register its Scene.  Idle
     * cost is one listener invocation per window opened; the listener short-circuits when the FX
     * toolkit is not yet started.
     */
    private void attachJavaFxWindowListener()
    {
        if(mWindowListenerAttached.get())
        {
            return;
        }

        Runnable attach = () -> {
            //Attach once, even if several attempts were scheduled before the first one ran
            if(!mWindowListenerAttached.compareAndSet(false, true))
            {
                return;
            }

            try
            {
                javafx.stage.Window.getWindows().addListener(
                        (javafx.collections.ListChangeListener<javafx.stage.Window>) change -> {
                    while(change.next())
                    {
                        if(change.wasAdded())
                        {
                            for(javafx.stage.Window w: change.getAddedSubList())
                            {
                                themeWindowWhenSceneReady(w);
                            }
                        }
                    }
                });

                //Also cover any windows that already existed when we installed the listener.
                for(javafx.stage.Window w: javafx.stage.Window.getWindows())
                {
                    themeWindowWhenSceneReady(w);
                }
            }
            catch(Throwable t)
            {
                mLog.warn("Unable to attach JavaFX Window list listener for auto-theming", t);
            }
        };

        if(Platform.isFxApplicationThread())
        {
            attach.run();
        }
        else
        {
            try
            {
                Platform.runLater(attach);
            }
            catch(IllegalStateException ignored)
            {
                //FX toolkit not started yet, which is the case when initialize() runs at application
                //startup, before the toolkit starts.  register() attaches the listener once the
                //first JavaFX scene is registered, which happens after the toolkit starts.
            }
        }
    }

    private void themeWindowWhenSceneReady(javafx.stage.Window window)
    {
        if(window == null)
        {
            return;
        }

        Scene scene = window.getScene();
        if(scene != null)
        {
            applyToScene(scene, isDarkMode());
        }
        else
        {
            //The Scene may be assigned after the Window is added to the list (the
            //typical Alert/Dialog lifecycle).  Listen once for the assignment.
            window.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if(newScene != null)
                {
                    applyToScene(newScene, isDarkMode());
                }
            });
        }
    }

    /**
     * @return the currently active theme.
     */
    public Theme getCurrentTheme()
    {
        return mCurrentTheme;
    }

    /**
     * @return true if the currently active theme is dark.
     */
    public boolean isDarkMode()
    {
        return mCurrentTheme != null && mCurrentTheme.isDark();
    }

    /**
     * Provides the color to render user-selected content (e.g. an alias) against a background in the current theme.
     * @param color selected by the user.
     * @param background that the color is rendered against.
     * @return the color, or a readable alternative when the user enabled complementary alias colors, dark mode is
     * active and the color lacks contrast with the background.
     */
    public Color getReadableColor(Color color, Color background)
    {
        return mComplementaryAliasColors && isDarkMode() ? ColorContrast.getReadable(color, background) : color;
    }

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

        //The toolkit is running once a scene exists, so attach the window listener if initialize()
        //couldn't, so that ad-hoc dialogs and alerts are themed
        attachJavaFxWindowListener();
        applyToScene(scene, isDarkMode());
    }

    @Subscribe
    public void onSettingsUpdated(SettingsType type)
    {
        if(type != SettingsType.APPLICATION || mSettings == null)
        {
            return;
        }

        Theme updated = mSettings.getApplicationSettings().getTheme();
        double updatedScale = mSettings.getApplicationSettings().getGuiScale();
        boolean updatedComplementary = mSettings.getApplicationSettings().isComplementaryAliasColors();

        mComplementaryAliasColors = updatedComplementary;

        if(updated == mCurrentTheme && Math.abs(updatedScale - mCurrentGuiScale) < 1e-6)
        {
            return;
        }

        mCurrentTheme = updated;
        mCurrentGuiScale = updatedScale;
        applyAll(updated);
    }

    private void applyAll(Theme theme)
    {
        //Compute the per-theme accent stylesheet on the calling thread so the data URL is ready
        //before the FX apply runs.  Capture the previous URL so we can remove it from each Scene.
        String previousAccentUrl = mAccentStylesheetUrl;
        String nextAccentUrl = buildAccentStylesheet(theme);
        mAccentStylesheetUrl = nextAccentUrl;

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
                        applyToScene(scene, theme.isDark(), previousAccentUrl, nextAccentUrl);
                    }
                }
            }

            for(javafx.stage.Window window: javafx.stage.Window.getWindows())
            {
                Scene scene = window.getScene();
                if(scene != null)
                {
                    applyToScene(scene, theme.isDark(), previousAccentUrl, nextAccentUrl);
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

    /**
     * Apply the current theme to a single Scene.  Used when a new Scene is registered or appears
     * mid-session.  The accent-stylesheet swap is handled by the (previousUrl, nextUrl) overload.
     */
    private void applyToScene(Scene scene, boolean darkMode)
    {
        applyToScene(scene, darkMode, null, mAccentStylesheetUrl);
    }

    /**
     * Update a Scene's stylesheets to match the current theme: ensures the dark-mode CSS is
     * present iff {@code darkMode}, removes {@code previousAccentUrl} if present, and ensures
     * {@code nextAccentUrl} is present so theme-specific accent colours are applied.
     */
    private void applyToScene(Scene scene, boolean darkMode, String previousAccentUrl,
                              String nextAccentUrl)
    {
        Runnable r = () -> {
            if(mDarkStylesheetUrl != null)
            {
                scene.getStylesheets().remove(mDarkStylesheetUrl);
                if(darkMode)
                {
                    scene.getStylesheets().add(mDarkStylesheetUrl);
                }
            }

            if(previousAccentUrl != null && !previousAccentUrl.equals(nextAccentUrl))
            {
                scene.getStylesheets().remove(previousAccentUrl);
            }

            if(nextAccentUrl != null && !scene.getStylesheets().contains(nextAccentUrl))
            {
                scene.getStylesheets().add(nextAccentUrl);
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

    /**
     * Build a small per-theme stylesheet that overrides Modena's accent and surface colours so
     * each theme reads visibly different in the JavaFX panels.  Returns a {@code data:} URL that
     * can be added to a Scene's stylesheets list directly.
     */
    private String buildAccentStylesheet(Theme theme)
    {
        try
        {
            Theme.Palette palette = theme.getPalette();
            String base = hex(palette.background());
            String inner = hex(palette.inner());
            String innerAlt = hex(palette.innerAlternate());
            String accent = hex(palette.accent());
            String selectionBg = hex(palette.selectionBackground());
            String selectionFg = hex(palette.selectionText());
            String text = hex(palette.text());
            String border = hex(palette.border());

            //JavaFX side of the GUI scale: setting -fx-font-size on .root cascades through
            //Modena because most sizes are em-relative.  Modena's default is 12px; multiplying
            //by mCurrentGuiScale and rounding to one decimal gives a clean scaled baseline.
            double scaledFontSize = Math.round(12.0d * mCurrentGuiScale * 10.0d) / 10.0d;

            StringBuilder css = new StringBuilder();
            css.append(".root {");
            css.append("-fx-font-size: ").append(scaledFontSize).append("px;");
            css.append("-fx-base: ").append(base).append(";");
            css.append("-fx-background: ").append(base).append(";");
            css.append("-fx-control-inner-background: ").append(inner).append(";");
            css.append("-fx-control-inner-background-alt: ").append(innerAlt).append(";");
            css.append("-fx-accent: ").append(accent).append(";");
            css.append("-fx-default-button: ").append(accent).append(";");
            css.append("-fx-focus-color: ").append(accent).append(";");
            css.append("-fx-faint-focus-color: ").append(accent).append("22;");
            css.append("-fx-selection-bar: ").append(selectionBg).append(";");
            css.append("-fx-selection-bar-text: ").append(selectionFg).append(";");
            css.append("-fx-selection-bar-non-focused: ").append(innerAlt).append(";");
            css.append("-fx-text-fill: ").append(text).append(";");
            css.append("-fx-text-base-color: ").append(text).append(";");
            css.append("-fx-text-background-color: ").append(text).append(";");
            css.append("-fx-mark-color: ").append(text).append(";");
            css.append("}");
            css.append(".table-row-cell:filled:selected, .list-cell:filled:selected,")
               .append(" .tree-cell:filled:selected, .tree-table-row-cell:filled:selected {")
               .append("-fx-background: ").append(selectionBg).append(";")
               .append("-fx-background-color: ").append(selectionBg).append(";")
               .append("-fx-text-fill: ").append(selectionFg).append(";")
               .append("-fx-table-cell-border-color: ").append(border).append(";")
               .append("}");
            css.append(".button:default {")
               .append("-fx-base: ").append(accent).append(";")
               .append("-fx-text-fill: ").append(selectionFg).append(";")
               .append("}");
            css.append(".scroll-bar .thumb {")
               .append("-fx-background-color: ").append(border).append(";")
               .append("-fx-background-radius: 3;")
               .append("}");
            css.append(".scroll-bar .thumb:hover {")
               .append("-fx-background-color: ").append(accent).append(";")
               .append("}");

            //Base64-encode the body.  Don't use URLEncoder: it is form encoding and turns spaces
            //into '+', which a data URL doesn't decode, so JavaFX silently drops the whole sheet.
            return "data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(
                    css.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        catch(Throwable t)
        {
            mLog.warn("Unable to build per-theme accent stylesheet for theme " + theme, t);
            return null;
        }
    }

    private static String hex(int rgb)
    {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    /**
     * Background color of text fields, tables and lists in the current theme, for checking the contrast of user
     * selected colors such as alias colors.
     */
    public Color getInnerBackground()
    {
        return new Color(mCurrentTheme.getPalette().inner());
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
