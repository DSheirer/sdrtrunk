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
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/**
 * Service status item: a colored status dot and a short text, for example: ● Streaming 2/2.  The compact view
 * shows the dot only, with the text in the tooltip.
 */
public abstract class ServiceStatusItem extends StatusItem
{
    private final Circle mDot = new Circle(4.5);
    private final Label mLabel = new Label();
    private StatusSeverity mSeverity;
    private String mTooltipText = "";

    /**
     * Constructs an instance.
     * @param title for the status bar's context menu
     * @param shownSetting that stores whether the item is shown
     */
    protected ServiceStatusItem(String title, Setting shownSetting)
    {
        super(title, shownSetting, true);
        mDot.getStyleClass().add("status-dot");
        mLabel.getStyleClass().add("status-text");
        mLabel.setMinWidth(Label.USE_PREF_SIZE);
        getChildren().addAll(mDot, mLabel);
        show(StatusSeverity.NEUTRAL, "", "");
    }

    /**
     * Shows the status.  JavaFX thread only.
     * @param severity for the status dot
     * @param text for the label
     * @param tooltip for the details
     */
    protected void show(StatusSeverity severity, String text, String tooltip)
    {
        if(mSeverity != severity)
        {
            mSeverity = severity;
            severity.apply(mDot);
        }

        mLabel.setText(text);
        mTooltipText = tooltip;
        updateTooltip();
    }

    /**
     * Current severity
     */
    public StatusSeverity getSeverity()
    {
        return mSeverity;
    }

    /**
     * Current text
     */
    public String getText()
    {
        return mLabel.getText();
    }

    @Override
    protected void compactChanged(boolean compact)
    {
        mLabel.setVisible(!compact);
        mLabel.setManaged(!compact);
        updateTooltip();
    }

    private void updateTooltip()
    {
        //The compact view doesn't show the text, so the tooltip leads with it
        setTooltipText(isCompact() && !mLabel.getText().isEmpty() ? mLabel.getText() + "\n\n" + mTooltipText :
            mTooltipText);
    }
}
