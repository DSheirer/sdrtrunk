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
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Window;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.Scene;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
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
        //Install the LAF synchronously on the calling thread so that any Swing components
        //constructed immediately afterwards (in particular the main JFrame) find a fully
        //populated UIDefaults set.  UIManager.setLookAndFeel is safe to call before the EDT
        //is running.
        LookAndFeel newLaf;
        try
        {
            newLaf = darkMode ? new FlatDarkLaf() : new FlatLightLaf();
            UIManager.setLookAndFeel(newLaf);
        }
        catch(Exception e)
        {
            mLog.error("Unable to apply Swing look-and-feel for dark mode = " + darkMode, e);
            return;
        }

        try
        {
            //JIDE 3.6.18 has compile-time `instanceof WindowsLookAndFeel` checks throughout
            //LookAndFeelFactory.  The 3-arg overload lets us hand JIDE an explicit Metal LAF
            //instance for type detection while leaving the real (FlatLaf) defaults table in
            //place; JIDE's Metal branch matches first.
            LookAndFeelFactory.installJideExtension(UIManager.getLookAndFeelDefaults(),
                    new MetalLookAndFeel(), LookAndFeelFactory.VSNET_STYLE);
        }
        catch(Throwable t)
        {
            mLog.warn("Unable to install JIDE LAF extension after theme change", t);
        }

        //JIDE's Metal-flavored extension overwrites FlatLaf's color/font/border defaults for
        //standard Swing component keys.  Re-apply FlatLaf's defaults on top of JIDE's install
        //so the inner panels pick up dark backgrounds.  JIDE's UI delegate registrations live
        //under Jide-prefixed keys that FlatLaf does not define, so they survive untouched.
        try
        {
            UIDefaults flatFresh = (darkMode ? new FlatDarkLaf() : new FlatLightLaf()).getDefaults();
            UIDefaults active = UIManager.getLookAndFeelDefaults();
            for(java.util.Map.Entry<Object, Object> entry: flatFresh.entrySet())
            {
                Object key = entry.getKey();
                Object value = entry.getValue();
                String keyName = String.valueOf(key);

                if(keyName.startsWith("Jide") || keyName.contains(".Jide"))
                {
                    continue;
                }

                if(value != null)
                {
                    active.put(key, value);
                }
            }
        }
        catch(Throwable t)
        {
            mLog.warn("Unable to re-apply FlatLaf defaults after JIDE install", t);
        }

        //FlatLaf's per-component foreground/background lookups go through derived/lazy values
        //that JIDE's Metal initializer was also feeding from, so even after the defaults copy
        //above some labels and buttons end up with Metal-era dark foregrounds on a dark
        //background.  Brute-force the standard component colour keys via UIManager.put which
        //writes to the user-defaults layer that takes precedence over the LAF defaults.  Use
        //near-white rather than FlatLaf's #bbbbbb so text reads sharply on the dark panels.
        applyExplicitOverrides(darkMode);

        //Re-rendering any already-realized Swing windows must happen on the EDT.
        Runnable updateRealized = () -> {
            for(Window window: Window.getWindows())
            {
                SwingUtilities.updateComponentTreeUI(window);
            }
        };

        if(EventQueue.isDispatchThread())
        {
            updateRealized.run();
        }
        else
        {
            EventQueue.invokeLater(updateRealized);
        }
    }

    private void applyExplicitOverrides(boolean darkMode)
    {
        //Colors stored under UIManager keys must be ColorUIResource (or null) for
        //LookAndFeel.installColorsAndFont() to overwrite a component's existing colour
        //on subsequent updateComponentTreeUI calls.  Using a plain java.awt.Color pins
        //the component foreground/background permanently and prevents the theme from
        //ever toggling back.
        ColorUIResource bgPanel;
        ColorUIResource bgRaised;
        ColorUIResource bgInput;
        ColorUIResource bgSelection;
        ColorUIResource fgPrimary;
        ColorUIResource fgSelection;
        ColorUIResource fgDisabled;
        ColorUIResource border;

        if(darkMode)
        {
            bgPanel = new ColorUIResource(0x2b2b2b);
            bgRaised = new ColorUIResource(0x3c3f41);
            bgInput = new ColorUIResource(0x313335);
            bgSelection = new ColorUIResource(0x214283);
            fgPrimary = new ColorUIResource(0xe6e6e6);
            fgSelection = new ColorUIResource(Color.WHITE);
            fgDisabled = new ColorUIResource(0x6a6a6a);
            border = new ColorUIResource(0x4f5356);
        }
        else
        {
            //Light mode mirrors FlatLightLaf's palette.
            bgPanel = new ColorUIResource(0xf2f2f2);
            bgRaised = new ColorUIResource(0xffffff);
            bgInput = new ColorUIResource(0xffffff);
            bgSelection = new ColorUIResource(0x2675bf);
            fgPrimary = new ColorUIResource(0x1e1e1e);
            fgSelection = new ColorUIResource(Color.WHITE);
            fgDisabled = new ColorUIResource(0x8c8c8c);
            border = new ColorUIResource(0xc4c4c4);
        }

        String[] simpleComponents = {
                "Button", "ToggleButton", "CheckBox", "RadioButton",
                "Label", "Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem",
                "MenuBar", "PopupMenu", "Panel", "Viewport", "ScrollPane",
                "Separator", "ToolBar", "ToolTip", "FormattedTextField",
                "TitledBorder", "TabbedPane", "OptionPane", "ProgressBar",
                "Spinner", "Slider", "ComboBox", "EditorPane", "PasswordField"
        };

        for(String c: simpleComponents)
        {
            UIManager.put(c + ".background", bgPanel);
            UIManager.put(c + ".foreground", fgPrimary);
            UIManager.put(c + ".disabledForeground", fgDisabled);
            UIManager.put(c + ".disabledText", fgDisabled);
        }

        //Buttons sit on a slightly raised tone than the panel they live on.
        for(String c: new String[]{"Button", "ToggleButton", "ComboBox", "Spinner"})
        {
            UIManager.put(c + ".background", bgRaised);
        }

        //Text inputs use their own background tone for affordance.
        for(String c: new String[]{"TextField", "TextArea", "TextPane", "EditorPane",
                "FormattedTextField", "PasswordField"})
        {
            UIManager.put(c + ".background", bgInput);
            UIManager.put(c + ".foreground", fgPrimary);
            UIManager.put(c + ".caretForeground", fgPrimary);
            UIManager.put(c + ".inactiveForeground", fgDisabled);
            UIManager.put(c + ".selectionBackground", bgSelection);
            UIManager.put(c + ".selectionForeground", fgSelection);
        }

        //Tables, trees and lists.
        for(String c: new String[]{"Table", "Tree", "List"})
        {
            UIManager.put(c + ".background", bgInput);
            UIManager.put(c + ".foreground", fgPrimary);
            UIManager.put(c + ".selectionBackground", bgSelection);
            UIManager.put(c + ".selectionForeground", fgSelection);
        }

        UIManager.put("Table.gridColor", border);
        UIManager.put("Table.alternateRowColor", new ColorUIResource(darkMode ? 0x353739 : 0xfafafa));
        UIManager.put("TableHeader.background", bgRaised);
        UIManager.put("TableHeader.foreground", fgPrimary);
        UIManager.put("TableHeader.cellBorder", javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, border));

        //Tab labels read black-on-dark with the JIDE-flavoured TabbedPane otherwise.
        UIManager.put("TabbedPane.foreground", fgPrimary);
        UIManager.put("TabbedPane.background", bgPanel);
        UIManager.put("TabbedPane.selectedForeground", fgSelection);
        UIManager.put("TabbedPane.selected", bgRaised);
        UIManager.put("TabbedPane.contentAreaColor", bgPanel);
        UIManager.put("TabbedPane.darkShadow", border);
        UIManager.put("TabbedPane.shadow", border);

        //Menu hover and selected states.
        UIManager.put("MenuItem.selectionBackground", bgSelection);
        UIManager.put("MenuItem.selectionForeground", fgSelection);
        UIManager.put("Menu.selectionBackground", bgSelection);
        UIManager.put("Menu.selectionForeground", fgSelection);

        //Borders and split-pane divider colour.
        UIManager.put("SplitPane.background", bgPanel);
        UIManager.put("SplitPane.dividerFocusColor", bgSelection);
        UIManager.put("SplitPaneDivider.draggingColor", bgSelection);
        UIManager.put("Component.borderColor", border);
        UIManager.put("Component.disabledBorderColor", border);
        UIManager.put("Separator.foreground", border);
        UIManager.put("Separator.background", bgPanel);

        //ToolTip needs to stand out from the panel.
        UIManager.put("ToolTip.background", bgRaised);
        UIManager.put("ToolTip.foreground", fgPrimary);

        //JIDE components (JideTabbedPane, JideSplitPane, JideButton, etc.) read their colors
        //from JIDE-specific UIManager keys, not the standard Swing keys.  Set those too so
        //tabs and other JIDE widgets render with the same palette.
        UIManager.put("JideTabbedPane.background", bgPanel);
        UIManager.put("JideTabbedPane.foreground", fgPrimary);
        UIManager.put("JideTabbedPane.tabAreaBackground", bgPanel);
        UIManager.put("JideTabbedPane.selectedTabBackground", bgRaised);
        UIManager.put("JideTabbedPane.activeTabBackground", bgRaised);
        UIManager.put("JideTabbedPane.tabListBackground", bgRaised);
        UIManager.put("JideTabbedPane.selectedTabTextForeground", fgSelection);
        UIManager.put("JideTabbedPane.unselectedTabTextForeground", fgPrimary);
        UIManager.put("JideTabbedPane.activeTabTextForeground", fgSelection);
        UIManager.put("JideTabbedPane.shadow", border);
        UIManager.put("JideTabbedPane.darkShadow", border);
        UIManager.put("JideTabbedPane.light", bgRaised);
        UIManager.put("JideTabbedPane.highlight", bgRaised);

        UIManager.put("JideSplitPane.background", bgPanel);
        UIManager.put("JideSplitPaneDivider.background", bgPanel);
        UIManager.put("JideSplitPane.dividerColor", border);

        UIManager.put("JideButton.background", bgRaised);
        UIManager.put("JideButton.foreground", fgPrimary);
        UIManager.put("JideButton.selectedBackground", bgSelection);
        UIManager.put("JideButton.selectedForeground", fgSelection);
        UIManager.put("JideButton.shadow", border);
        UIManager.put("JideButton.darkShadow", border);
        UIManager.put("JideButton.light", bgRaised);
        UIManager.put("JideButton.highlight", bgRaised);

        UIManager.put("JideLabel.background", bgPanel);
        UIManager.put("JideLabel.foreground", fgPrimary);
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
