/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.colortheme;

import io.github.dsheirer.preference.UserPreferences;
import javafx.scene.Scene;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the application's color theme (light/dark mode).
 * This class handles applying the appropriate theme to both JavaFX and Swing components.
 */
public class ColorThemeManager
{
    private final static Logger mLog = LoggerFactory.getLogger(ColorThemeManager.class);
    private static final String DARK_MODE_CSS = "/colortheme/dark-theme.css";
    
    /**
     * Applies the user's preferred color theme to a JavaFX scene.
     * @param scene to apply the theme to
     * @param userPreferences containing the theme preference
     */
    public static void applyThemeToScene(Scene scene, UserPreferences userPreferences)
    {
        if(scene != null && userPreferences != null)
        {
            boolean darkMode = userPreferences.getColorThemePreference().isDarkModeEnabled();
            applyThemeToScene(scene, darkMode);
        }
    }

    /**
     * Applies the specified theme to a JavaFX scene.
     * @param scene to apply the theme to
     * @param darkMode true for dark mode, false for light mode
     */
    public static void applyThemeToScene(Scene scene, boolean darkMode)
    {
        if(scene != null)
        {
            scene.getStylesheets().clear();
            
            if(darkMode)
            {
                try
                {
                    String css = ColorThemeManager.class.getResource(DARK_MODE_CSS).toExternalForm();
                    scene.getStylesheets().add(css);
                }
                catch(Exception e)
                {
                    mLog.error("Error loading dark mode CSS", e);
                }
            }
        }
    }

    /**
     * Applies Swing dark mode theme colors to the UIManager defaults.
     * This should be called early in the application lifecycle, before any Swing components are created.
     * @param userPreferences containing the theme preference
     */
    public static void applySwingTheme(UserPreferences userPreferences)
    {
        if(userPreferences != null && userPreferences.getColorThemePreference().isDarkModeEnabled())
        {
            applySwingDarkMode();
        }
    }

    /**
     * Applies dark mode colors to Swing components.
     */
    private static void applySwingDarkMode()
    {
        try
        {
            // Dark mode colors
            Color darkBackground = new Color(43, 43, 43);
            Color darkerBackground = new Color(30, 30, 30);
            Color lightBackground = new Color(50, 50, 50);
            Color darkForeground = new Color(187, 187, 187);
            Color darkSelectionBackground = new Color(75, 110, 175);
            Color darkSelectionForeground = Color.WHITE;
            Color darkBorder = new Color(60, 60, 60);
            Color darkDisabled = new Color(100, 100, 100);
            
            // Panel and general backgrounds
            UIManager.put("Panel.background", new ColorUIResource(darkBackground));
            UIManager.put("OptionPane.background", new ColorUIResource(darkBackground));
            UIManager.put("control", new ColorUIResource(darkBackground));
            UIManager.put("window", new ColorUIResource(darkerBackground));
            UIManager.put("desktop", new ColorUIResource(darkerBackground));
            
            // Text colors
            UIManager.put("text", new ColorUIResource(darkForeground));
            UIManager.put("textText", new ColorUIResource(darkForeground));
            UIManager.put("textForeground", new ColorUIResource(darkForeground));
            UIManager.put("textHighlight", new ColorUIResource(darkSelectionBackground));
            UIManager.put("textHighlightText", new ColorUIResource(darkSelectionForeground));
            UIManager.put("textInactiveText", new ColorUIResource(darkDisabled));
            UIManager.put("Label.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Label.disabledForeground", new ColorUIResource(darkDisabled));
            UIManager.put("Panel.foreground", new ColorUIResource(darkForeground));
            UIManager.put("info", new ColorUIResource(darkBackground));
            UIManager.put("infoText", new ColorUIResource(darkForeground));
            
            // Button colors
            UIManager.put("Button.background", new ColorUIResource(darkBackground));
            UIManager.put("Button.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Button.select", new ColorUIResource(darkerBackground));
            UIManager.put("Button.shadow", new ColorUIResource(darkerBackground));
            UIManager.put("Button.darkShadow", new ColorUIResource(darkerBackground));
            UIManager.put("Button.light", new ColorUIResource(lightBackground));
            UIManager.put("Button.highlight", new ColorUIResource(lightBackground));
            UIManager.put("Button.border", new ColorUIResource(darkBorder));
            UIManager.put("Button.disabledText", new ColorUIResource(darkDisabled));
            UIManager.put("Button.focus", new ColorUIResource(darkSelectionBackground));
            
            // TextField colors
            UIManager.put("TextField.background", new ColorUIResource(darkerBackground));
            UIManager.put("TextField.foreground", new ColorUIResource(darkForeground));
            UIManager.put("TextField.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("TextField.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("TextField.inactiveForeground", new ColorUIResource(darkForeground.darker()));
            UIManager.put("TextField.caretForeground", new ColorUIResource(darkForeground));
            UIManager.put("FormattedTextField.background", new ColorUIResource(darkerBackground));
            UIManager.put("FormattedTextField.foreground", new ColorUIResource(darkForeground));
            UIManager.put("PasswordField.background", new ColorUIResource(darkerBackground));
            UIManager.put("PasswordField.foreground", new ColorUIResource(darkForeground));
            
            // TextArea colors
            UIManager.put("TextArea.background", new ColorUIResource(darkerBackground));
            UIManager.put("TextArea.foreground", new ColorUIResource(darkForeground));
            UIManager.put("TextArea.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("TextArea.selectionForeground", new ColorUIResource(darkSelectionForeground));
            
            // List colors
            UIManager.put("List.background", new ColorUIResource(darkerBackground));
            UIManager.put("List.foreground", new ColorUIResource(darkForeground));
            UIManager.put("List.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("List.selectionForeground", new ColorUIResource(darkSelectionForeground));
            
            // Table colors
            UIManager.put("Table.background", new ColorUIResource(darkerBackground));
            UIManager.put("Table.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Table.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("Table.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("Table.gridColor", new ColorUIResource(darkBorder));
            UIManager.put("Table.focusCellBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("Table.focusCellForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("Table.dropLineColor", new ColorUIResource(darkSelectionBackground));
            UIManager.put("Table.dropLineShortColor", new ColorUIResource(darkSelectionBackground));
            UIManager.put("TableHeader.background", new ColorUIResource(darkBackground));
            UIManager.put("TableHeader.foreground", new ColorUIResource(darkForeground));
            UIManager.put("TableHeader.cellBorder", new ColorUIResource(darkBorder));
            
            // Tree colors
            UIManager.put("Tree.background", new ColorUIResource(darkerBackground));
            UIManager.put("Tree.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Tree.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("Tree.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("Tree.textBackground", new ColorUIResource(darkerBackground));
            UIManager.put("Tree.textForeground", new ColorUIResource(darkForeground));
            
            // Menu colors - force dark backgrounds
            UIManager.put("Menu.background", new ColorUIResource(darkBackground));
            UIManager.put("Menu.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Menu.opaque", true);
            UIManager.put("MenuBar.background", new ColorUIResource(darkBackground));
            UIManager.put("MenuBar.foreground", new ColorUIResource(darkForeground));
            UIManager.put("MenuBar.border", new ColorUIResource(darkBorder));
            UIManager.put("MenuBar.shadow", new ColorUIResource(darkerBackground));
            UIManager.put("MenuBar.highlight", new ColorUIResource(lightBackground));
            UIManager.put("MenuItem.background", new ColorUIResource(darkBackground));
            UIManager.put("MenuItem.foreground", new ColorUIResource(darkForeground));
            UIManager.put("MenuItem.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("MenuItem.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("MenuItem.opaque", true);
            UIManager.put("MenuItem.border", new ColorUIResource(darkBorder));
            UIManager.put("PopupMenu.background", new ColorUIResource(darkBackground));
            UIManager.put("PopupMenu.foreground", new ColorUIResource(darkForeground));
            UIManager.put("CheckBoxMenuItem.background", new ColorUIResource(darkBackground));
            UIManager.put("CheckBoxMenuItem.foreground", new ColorUIResource(darkForeground));
            UIManager.put("CheckBoxMenuItem.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("CheckBoxMenuItem.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("RadioButtonMenuItem.background", new ColorUIResource(darkBackground));
            UIManager.put("RadioButtonMenuItem.foreground", new ColorUIResource(darkForeground));
            UIManager.put("RadioButtonMenuItem.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("RadioButtonMenuItem.selectionForeground", new ColorUIResource(darkSelectionForeground));
            
            // ComboBox colors
            UIManager.put("ComboBox.background", new ColorUIResource(darkerBackground));
            UIManager.put("ComboBox.foreground", new ColorUIResource(darkForeground));
            UIManager.put("ComboBox.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("ComboBox.selectionForeground", new ColorUIResource(darkSelectionForeground));
            
            // Spinner styling
            UIManager.put("Spinner.background", new ColorUIResource(darkerBackground));
            UIManager.put("Spinner.foreground", new ColorUIResource(darkForeground));
            UIManager.put("Spinner.border", new ColorUIResource(darkBorder));
            
            // ProgressBar styling
            UIManager.put("ProgressBar.background", new ColorUIResource(darkerBackground));
            UIManager.put("ProgressBar.foreground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("ProgressBar.selectionBackground", new ColorUIResource(darkForeground));
            UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(darkForeground));
            
            // ScrollBar colors
            UIManager.put("ScrollBar.track", new ColorUIResource(darkerBackground));
            UIManager.put("ScrollBar.thumb", new ColorUIResource(darkBackground.brighter()));
            UIManager.put("ScrollBar.background", new ColorUIResource(darkBackground));
            
            // TabbedPane colors
            UIManager.put("TabbedPane.background", new ColorUIResource(darkBackground));
            UIManager.put("TabbedPane.foreground", new ColorUIResource(darkForeground));
            UIManager.put("TabbedPane.selected", new ColorUIResource(darkerBackground));
            UIManager.put("TabbedPane.contentAreaColor", new ColorUIResource(darkBackground));
            
            // ToolTip colors
            UIManager.put("ToolTip.background", new ColorUIResource(darkBackground.brighter()));
            UIManager.put("ToolTip.foreground", new ColorUIResource(darkForeground));
            
            // Border colors
            UIManager.put("TextField.border", new ColorUIResource(darkBorder));
            UIManager.put("ScrollPane.border", new ColorUIResource(darkBorder));
            
            // Split pane colors
            UIManager.put("SplitPane.background", new ColorUIResource(darkBackground));
            UIManager.put("SplitPane.foreground", new ColorUIResource(darkForeground));
            UIManager.put("SplitPane.dividerFocusColor", new ColorUIResource(darkSelectionBackground));
            UIManager.put("SplitPaneDivider.draggingColor", new ColorUIResource(darkSelectionBackground));
            
            // Viewport and ScrollPane specific
            UIManager.put("Viewport.background", new ColorUIResource(darkerBackground));
            UIManager.put("Viewport.foreground", new ColorUIResource(darkForeground));
            UIManager.put("ScrollPane.background", new ColorUIResource(darkerBackground));
            UIManager.put("ScrollPane.foreground", new ColorUIResource(darkForeground));
            
            // Additional component colors
            UIManager.put("Separator.foreground", new ColorUIResource(darkBorder));
            UIManager.put("Separator.background", new ColorUIResource(darkBorder));
            UIManager.put("Separator.shadow", new ColorUIResource(darkBorder));
            UIManager.put("Separator.highlight", new ColorUIResource(lightBackground));
            
            // EditorPane and TextPane
            UIManager.put("EditorPane.background", new ColorUIResource(darkerBackground));
            UIManager.put("EditorPane.foreground", new ColorUIResource(darkForeground));
            UIManager.put("EditorPane.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("EditorPane.selectionForeground", new ColorUIResource(darkSelectionForeground));
            UIManager.put("TextPane.background", new ColorUIResource(darkerBackground));
            UIManager.put("TextPane.foreground", new ColorUIResource(darkForeground));
            UIManager.put("TextPane.selectionBackground", new ColorUIResource(darkSelectionBackground));
            UIManager.put("TextPane.selectionForeground", new ColorUIResource(darkSelectionForeground));
            
            // InternalFrame
            UIManager.put("InternalFrame.background", new ColorUIResource(darkBackground));
            
            // Ensure nimbusBase and other Nimbus-specific properties don't override
            UIManager.put("nimbusBase", new ColorUIResource(darkBackground));
            UIManager.put("nimbusBlueGrey", new ColorUIResource(darkBackground));
            UIManager.put("control", new ColorUIResource(darkBackground));
            UIManager.put("controlHighlight", new ColorUIResource(lightBackground));
            UIManager.put("controlShadow", new ColorUIResource(darkerBackground));
            UIManager.put("controlDkShadow", new ColorUIResource(darkerBackground));
            UIManager.put("controlLtHighlight", new ColorUIResource(lightBackground));
            
            // Additional components that might have white backgrounds
            UIManager.put("DesktopPane.background", new ColorUIResource(darkerBackground));
            UIManager.put("InternalFrame.activeTitleBackground", new ColorUIResource(darkBackground));
            UIManager.put("InternalFrame.inactiveTitleBackground", new ColorUIResource(darkerBackground));
            UIManager.put("OptionPane.messageForeground", new ColorUIResource(darkForeground));
            
            // Label and text rendering
            UIManager.put("activeCaption", new ColorUIResource(darkBackground));
            UIManager.put("activeCaptionText", new ColorUIResource(darkForeground));
            UIManager.put("inactiveCaption", new ColorUIResource(darkerBackground));
            UIManager.put("inactiveCaptionText", new ColorUIResource(darkDisabled));
            
            mLog.info("Applied Swing dark mode theme");
        }
        catch(Exception e)
        {
            mLog.error("Error applying Swing dark mode theme", e);
        }
    }
    
    /**
     * Updates all components in a container to use the current theme.
     * This should be called after UIManager properties are set.
     * @param container the container whose components should be updated
     */
    public static void updateComponentTreeUI(java.awt.Container container)
    {
        if(container != null)
        {
            try
            {
                javax.swing.SwingUtilities.updateComponentTreeUI(container);

                // Force repaint
                container.invalidate();
                container.validate();
                container.repaint();
            }
            catch(Exception e)
            {
                mLog.error("Error updating component tree UI", e);
            }
        }
    }

    /**
     * Apply dark theme colors directly to a component (for when UIManager defaults don't work)
     */
    public static void applyDarkThemeToComponent(java.awt.Component component, UserPreferences userPreferences)
    {
        if(userPreferences.getColorThemePreference().isDarkModeEnabled() && component != null)
        {
            java.awt.Color darkBackground = new java.awt.Color(43, 43, 43);
            java.awt.Color darkerBackground = new java.awt.Color(30, 30, 30);
            java.awt.Color darkForeground = new java.awt.Color(187, 187, 187);
            
            if(component instanceof javax.swing.JSlider)
            {
                component.setBackground(darkBackground);
                component.setForeground(darkForeground);
            }
            else if(component instanceof javax.swing.JButton || component instanceof javax.swing.JToggleButton)
            {
                component.setBackground(darkBackground);
                component.setForeground(darkForeground);
            }
            else if(component instanceof javax.swing.JMenuBar || component instanceof javax.swing.JMenu)
            {
                component.setBackground(darkBackground);
                component.setForeground(darkForeground);
            }
        }
    }
}

