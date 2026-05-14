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
 * Centralized theme manager that drives both Swing and JavaFX appearance from the user
 * preference selection.
 *
 * <p>Each {@link Theme} pairs a FlatLaf look-and-feel class with a dark/light flag.  When a
 * theme is applied:
 * <ul>
 *   <li>The Swing LAF is installed (reflected from {@link Theme#getLafClassName()}).</li>
 *   <li>JIDE's LAF extension is reinstalled with an explicit Metal LAF for type detection so
 *       JIDE's compile-time {@code WindowsLookAndFeel} {@code instanceof} checks do not hit
 *       the missing-class branch on Linux/macOS JDKs.</li>
 *   <li>FlatLaf's standard defaults are re-applied on top of JIDE's clobber, and a set of
 *       JIDE-specific colour keys (JideTabbedPane.*, JideSplitPane.*, JideButton.*) are seeded
 *       from FlatLaf's just-installed values so each theme's palette propagates to JIDE widgets
 *       automatically.</li>
 *   <li>The dark JavaFX stylesheet is applied to (or removed from) every registered Scene
 *       depending on the theme's dark flag.</li>
 * </ul>
 */
public class ThemeManager
{
    private final static Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String DARK_STYLESHEET = "/sdrtrunk_dark.css";
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final Set<WeakReference<Scene>> mScenes = new LinkedHashSet<>();
    private final Set<WeakReference<java.awt.Component>> mSwingRoots = new LinkedHashSet<>();
    private final String mDarkStylesheetUrl;
    private volatile UserPreferences mUserPreferences;
    private volatile Theme mCurrentTheme = Theme.LIGHT;

    private ThemeManager()
    {
        java.net.URL url = ThemeManager.class.getResource(DARK_STYLESHEET);
        mDarkStylesheetUrl = (url != null) ? url.toExternalForm() : null;

        if(mDarkStylesheetUrl == null)
        {
            mLog.warn("Dark stylesheet [{}] not found on classpath - dark themes will only affect Swing.",
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
    public void initialize(UserPreferences userPreferences)
    {
        if(userPreferences == null)
        {
            return;
        }

        boolean firstBind = (mUserPreferences == null);
        mUserPreferences = userPreferences;
        mCurrentTheme = userPreferences.getApplicationPreference().getTheme();

        if(firstBind)
        {
            MyEventBus.getGlobalEventBus().register(this);
            attachJavaFxWindowListener();
        }

        applySwingLookAndFeel(mCurrentTheme);
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
        Runnable attach = () -> {
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
                //FX toolkit not started yet - first JFXPanel construction will start it; we
                //re-attempt the attach lazily inside applyAll() via the Window.getWindows() walk.
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

        applyToScene(scene, isDarkMode());
    }

    /**
     * Register a Swing component that may be temporarily detached from any Window's component tree
     * (e.g. swappable right-pane components in the Channel tab).  Registered components get
     * {@link SwingUtilities#updateComponentTreeUI(java.awt.Component)} called on them whenever the
     * theme is applied, so they pick up the current palette even if they weren't attached when the
     * user toggled.
     */
    public void registerSwing(java.awt.Component component)
    {
        if(component == null)
        {
            return;
        }

        synchronized(mSwingRoots)
        {
            Iterator<WeakReference<java.awt.Component>> it = mSwingRoots.iterator();
            while(it.hasNext())
            {
                java.awt.Component existing = it.next().get();
                if(existing == null || existing == component)
                {
                    it.remove();
                }
            }
            mSwingRoots.add(new WeakReference<>(component));
        }
    }

    @Subscribe
    public void onPreferenceUpdated(PreferenceType type)
    {
        if(type != PreferenceType.APPLICATION || mUserPreferences == null)
        {
            return;
        }

        Theme updated = mUserPreferences.getApplicationPreference().getTheme();

        if(updated == mCurrentTheme)
        {
            return;
        }

        mCurrentTheme = updated;
        applyAll(updated);
    }

    private void applyAll(Theme theme)
    {
        applySwingLookAndFeel(theme);

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
                        applyToScene(scene, theme.isDark());
                    }
                }
            }

            for(javafx.stage.Window window: javafx.stage.Window.getWindows())
            {
                Scene scene = window.getScene();
                if(scene != null)
                {
                    applyToScene(scene, theme.isDark());
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

    private void applySwingLookAndFeel(Theme theme)
    {
        //Install the LAF synchronously on the calling thread so any Swing components constructed
        //immediately afterwards (in particular the main JFrame) find a fully populated UIDefaults
        //set.  UIManager.setLookAndFeel is safe to call before the EDT is running.
        LookAndFeel newLaf = instantiate(theme);
        try
        {
            UIManager.setLookAndFeel(newLaf);
        }
        catch(Exception e)
        {
            mLog.error("Unable to apply Swing look-and-feel for theme = " + theme, e);
            return;
        }

        try
        {
            //JIDE 3.6.18 has compile-time `instanceof WindowsLookAndFeel` checks throughout
            //LookAndFeelFactory.  The 3-arg overload lets us hand JIDE an explicit Metal LAF
            //instance for type detection while leaving the real (FlatLaf) defaults table in place;
            //JIDE's Metal branch matches first.
            LookAndFeelFactory.installJideExtension(UIManager.getLookAndFeelDefaults(),
                    new MetalLookAndFeel(), LookAndFeelFactory.VSNET_STYLE);
        }
        catch(Throwable t)
        {
            mLog.warn("Unable to install JIDE LAF extension after theme change", t);
        }

        //JIDE's Metal-flavored extension overwrites FlatLaf's color/font/border defaults for
        //standard Swing component keys.  Re-apply FlatLaf's defaults on top of JIDE's install so
        //the inner panels pick up the right colours.  JIDE's UI delegate registrations live under
        //Jide-prefixed keys that FlatLaf does not define, so they survive untouched.
        try
        {
            UIDefaults flatFresh = instantiate(theme).getDefaults();
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

        //Brute-force the standard component colour keys via UIManager.put which writes to the
        //user-defaults layer that takes precedence over the LAF defaults.  Colours are read from
        //the FlatLaf-installed defaults so each theme contributes its own palette.
        applyExplicitOverrides(theme);

        //Re-rendering already-realized Swing windows must happen on the EDT.  Also walk any Swing
        //components explicitly registered via registerSwing(...) - they may be detached from any
        //Window's tree at toggle time (e.g. swappable channel-tab right components).
        Runnable updateRealized = () -> {
            for(Window window: Window.getWindows())
            {
                SwingUtilities.updateComponentTreeUI(window);
            }

            synchronized(mSwingRoots)
            {
                Iterator<WeakReference<java.awt.Component>> it = mSwingRoots.iterator();
                while(it.hasNext())
                {
                    java.awt.Component c = it.next().get();
                    if(c == null)
                    {
                        it.remove();
                    }
                    else
                    {
                        SwingUtilities.updateComponentTreeUI(c);
                    }
                }
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

    /**
     * Reflectively instantiate the FlatLaf class for the given theme.  Falls back to
     * {@link FlatLightLaf} if the class is missing - that should not happen in a normal build but
     * keeps the app usable if a theme jar is removed.
     */
    private LookAndFeel instantiate(Theme theme)
    {
        try
        {
            Class<?> cls = Class.forName(theme.getLafClassName());
            return (LookAndFeel) cls.getDeclaredConstructor().newInstance();
        }
        catch(Throwable t)
        {
            mLog.error("Unable to instantiate LAF [{}] for theme {} - falling back to FlatLightLaf",
                    theme.getLafClassName(), theme, t);
            return new FlatLightLaf();
        }
    }

    private void applyExplicitOverrides(Theme theme)
    {
        boolean darkMode = theme.isDark();

        //CRITICAL: read from UIManager.getLookAndFeelDefaults() (LAF layer only) rather than
        //UIManager.getColor() / UIManager.getDefaults() (which is the merged view).  The merged
        //view returns user-defaults entries from earlier applyExplicitOverrides calls instead of
        //the fresh LAF values, which means a second theme switch picks up the previous theme's
        //palette and the toggle appears broken after the first change.
        UIDefaults laf = UIManager.getLookAndFeelDefaults();

        ColorUIResource bgPanel = uir(laf.getColor("Panel.background"),
                darkMode ? 0x2b2b2b : 0xf2f2f2);
        ColorUIResource bgRaised = uir(laf.getColor("Button.background"),
                darkMode ? 0x3c3f41 : 0xffffff);
        ColorUIResource bgInput = uir(laf.getColor("TextField.background"),
                darkMode ? 0x313335 : 0xffffff);
        ColorUIResource bgSelection = uir(laf.getColor("List.selectionBackground"),
                darkMode ? 0x214283 : 0x2675bf);
        ColorUIResource fgPrimary = uir(laf.getColor("Label.foreground"),
                darkMode ? 0xe6e6e6 : 0x1e1e1e);
        ColorUIResource fgSelection = uir(laf.getColor("List.selectionForeground"),
                0xffffff);
        ColorUIResource fgDisabled = uir(laf.getColor("Label.disabledForeground"),
                darkMode ? 0x6a6a6a : 0x8c8c8c);
        ColorUIResource border = uir(laf.getColor("Component.borderColor"),
                darkMode ? 0x4f5356 : 0xc4c4c4);
        ColorUIResource altRow = uir(laf.getColor("Table.alternateRowColor"),
                darkMode ? 0x353739 : 0xfafafa);

        //Use a slightly brighter foreground than FlatLaf's default in dark mode so text reads
        //sharper on the JIDE-flavoured surfaces.  Light themes keep their own contrast.
        if(darkMode && fgPrimary.getRed() < 0xd0)
        {
            fgPrimary = new ColorUIResource(0xe6e6e6);
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

        for(String c: new String[]{"Button", "ToggleButton", "ComboBox", "Spinner"})
        {
            UIManager.put(c + ".background", bgRaised);
        }

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

        for(String c: new String[]{"Table", "Tree", "List"})
        {
            UIManager.put(c + ".background", bgInput);
            UIManager.put(c + ".foreground", fgPrimary);
            UIManager.put(c + ".selectionBackground", bgSelection);
            UIManager.put(c + ".selectionForeground", fgSelection);
        }

        UIManager.put("Table.gridColor", border);
        UIManager.put("Table.alternateRowColor", altRow);
        UIManager.put("TableHeader.background", bgRaised);
        UIManager.put("TableHeader.foreground", fgPrimary);
        UIManager.put("TableHeader.cellBorder",
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, border));

        //Tab labels: selected tabs sit on the raised surface (which is white in light themes),
        //not on the blue selection background, so the selected-tab text is the primary
        //foreground - not the white selection foreground that is right for row selections.
        UIManager.put("TabbedPane.foreground", fgPrimary);
        UIManager.put("TabbedPane.background", bgPanel);
        UIManager.put("TabbedPane.selectedForeground", fgPrimary);
        UIManager.put("TabbedPane.selected", bgRaised);
        UIManager.put("TabbedPane.contentAreaColor", bgPanel);
        UIManager.put("TabbedPane.darkShadow", border);
        UIManager.put("TabbedPane.shadow", border);

        UIManager.put("MenuItem.selectionBackground", bgSelection);
        UIManager.put("MenuItem.selectionForeground", fgSelection);
        UIManager.put("Menu.selectionBackground", bgSelection);
        UIManager.put("Menu.selectionForeground", fgSelection);

        UIManager.put("SplitPane.background", bgPanel);
        UIManager.put("SplitPane.dividerFocusColor", bgSelection);
        UIManager.put("SplitPaneDivider.draggingColor", bgSelection);
        UIManager.put("Component.borderColor", border);
        UIManager.put("Component.disabledBorderColor", border);
        UIManager.put("Separator.foreground", border);
        UIManager.put("Separator.background", bgPanel);

        UIManager.put("ToolTip.background", bgRaised);
        UIManager.put("ToolTip.foreground", fgPrimary);

        //JIDE components read their colours from JIDE-specific UIManager keys, not the standard
        //Swing keys.  Seed those from the same palette so JIDE widgets follow the theme.
        UIManager.put("JideTabbedPane.background", bgPanel);
        UIManager.put("JideTabbedPane.foreground", fgPrimary);
        UIManager.put("JideTabbedPane.tabAreaBackground", bgPanel);
        UIManager.put("JideTabbedPane.selectedTabBackground", bgRaised);
        UIManager.put("JideTabbedPane.activeTabBackground", bgRaised);
        UIManager.put("JideTabbedPane.tabListBackground", bgRaised);
        UIManager.put("JideTabbedPane.selectedTabTextForeground", fgPrimary);
        UIManager.put("JideTabbedPane.unselectedTabTextForeground", fgPrimary);
        UIManager.put("JideTabbedPane.activeTabTextForeground", fgPrimary);
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

    /**
     * Convert a {@link Color} to a {@link ColorUIResource}, falling back to {@code fallbackRgb} if
     * the input is null.  Existing UIResource colours are returned as-is.
     */
    private static ColorUIResource uir(Color c, int fallbackRgb)
    {
        if(c == null)
        {
            return new ColorUIResource(fallbackRgb);
        }
        if(c instanceof ColorUIResource cuir)
        {
            return cuir;
        }
        return new ColorUIResource(c);
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
