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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.settings.Setting;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Resource status item: a short name, a small level bar and the value, for example: CPU ▮▮▯ 23%.  The bar turns
 * amber and then red as the level reaches the item's warning and error thresholds.
 */
public abstract class ResourceStatusItem extends StatusItem
{
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};
    private final ProgressBar mBar = new ProgressBar(0);
    private final Label mValue = new Label();
    private StatusSeverity mSeverity;

    /**
     * Constructs an instance.
     * @param title for the status bar's context menu
     * @param name shown in the status bar
     * @param shownSetting that stores whether the item is shown
     */
    protected ResourceStatusItem(String title, String name, Setting shownSetting)
    {
        super(title, shownSetting, false);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("status-name");
        nameLabel.setMinWidth(Label.USE_PREF_SIZE);
        mBar.getStyleClass().add("status-level");
        mBar.setFocusTraversable(false);
        mValue.getStyleClass().add("status-value");
        mValue.setMinWidth(Label.USE_PREF_SIZE);
        getChildren().addAll(nameLabel, mBar, mValue);
        show(0, StatusSeverity.OK, "", "");
    }

    /**
     * Marks the value as a percentage, which has a fixed width so that the bar doesn't move as the value changes.
     */
    protected void setPercentValue()
    {
        mValue.getStyleClass().add("status-percent");
    }

    /**
     * Shows the level.  JavaFX thread only.
     * @param level for the bar, from 0.0 to 1.0, or larger to show a full bar
     * @param severity for the bar color
     * @param value text after the bar
     * @param tooltip with the details
     */
    protected void show(double level, StatusSeverity severity, String value, String tooltip)
    {
        mBar.setProgress(Double.isFinite(level) ? Math.max(0, Math.min(1.0, level)) : 0);

        if(mSeverity != severity)
        {
            mSeverity = severity;
            severity.apply(mBar);
        }

        mValue.setText(value);
        setTooltipText(tooltip);
    }

    /**
     * Current severity
     */
    public StatusSeverity getSeverity()
    {
        return mSeverity;
    }

    /**
     * Current value text
     */
    public String getValueText()
    {
        return mValue.getText();
    }

    /**
     * Formats a fraction as a whole percentage, for example 23%.
     */
    public static String percent(double fraction)
    {
        return Math.round(fraction * 100.0) + "%";
    }

    /**
     * Formats a byte count with one decimal place for values under 100, for example 850 MB, 12.4 GB or 1.2 TB.
     */
    public static String bytes(long bytes)
    {
        double value = Math.max(0, bytes);
        int unit = 0;

        while(value >= 1024 && unit < UNITS.length - 1)
        {
            value /= 1024;
            unit++;
        }

        if(unit == 0 || value >= 100)
        {
            return Math.round(value) + " " + UNITS[unit];
        }

        return String.format(Locale.US, "%.1f %s", value, UNITS[unit]);
    }
}
