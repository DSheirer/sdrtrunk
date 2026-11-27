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
package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.gui.control.AccessibilityUtil;
import io.github.dsheirer.gui.control.AccessibleToggleSwitch;
import io.github.dsheirer.module.decode.config.AccessCodeFilter;
import io.github.dsheirer.module.decode.config.AccessCodeFilter.Format;
import io.github.dsheirer.module.decode.config.AccessCodeFilter.Mode;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.ToggleSwitch;

/**
 * Channel editor control for an access code filter (P25 NAC, DMR color code, NXDN RAN): an enable switch, an
 * include/exclude choice, a list of values and, optionally, a hex/decimal entry format choice.
 */
public class AccessCodeFilterEditor extends HBox
{
    private final String mName;
    private final int mMaximum;
    private final int mHexDigits;
    private final boolean mFormatSelectable;
    private final Format mDefaultFormat;
    private final BooleanProperty mModifiedProperty;

    private final ToggleSwitch mEnabledSwitch = new AccessibleToggleSwitch();
    private final ToggleButton mIncludeButton = new ToggleButton(Mode.INCLUDE.toString());
    private final ToggleButton mExcludeButton = new ToggleButton(Mode.EXCLUDE.toString());
    private final SegmentedButton mModeButton = new SegmentedButton();
    private final ToggleButton mHexButton = new ToggleButton(Format.HEX.toString());
    private final ToggleButton mDecimalButton = new ToggleButton(Format.DECIMAL.toString());
    private final SegmentedButton mFormatButton = new SegmentedButton();
    private final TextField mValuesField = new TextField();
    private final Label mStatusLabel = new Label();
    private boolean mLoading = false;
    private boolean mRevertingFormat = false;
    private boolean mEditorDisabled = false;
    private String mUnavailableReason = null;

    /**
     * Constructs an instance
     * @param name of the access code for labels, e.g. "NAC", "Color Code" or "RAN"
     * @param maximum allowed access code value, inclusive
     * @param hexDigits for zero-padded hex display, e.g. 3 for a P25 NAC
     * @param formatSelectable true to show the hex/decimal format choice
     * @param defaultFormat for a new filter, and the fixed format when the format isn't selectable
     * @param modifiedProperty of the parent editor, set to true when the user changes the filter
     */
    public AccessCodeFilterEditor(String name, int maximum, int hexDigits, boolean formatSelectable,
                                  Format defaultFormat, BooleanProperty modifiedProperty)
    {
        mName = name;
        mMaximum = maximum;
        mHexDigits = hexDigits;
        mFormatSelectable = formatSelectable;
        mDefaultFormat = defaultFormat;
        mModifiedProperty = modifiedProperty;

        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label(name + " Filter");
        filterLabel.setLabelFor(mEnabledSwitch);
        mEnabledSwitch.setTooltip(new Tooltip("Enable filtering of traffic by " + name));
        mEnabledSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateControlStates();
            modified();
        });

        mIncludeButton.setUserData(Mode.INCLUDE);
        mIncludeButton.setTooltip(new Tooltip("Only process traffic with a listed " + name));
        mExcludeButton.setUserData(Mode.EXCLUDE);
        mExcludeButton.setTooltip(new Tooltip("Drop traffic with a listed " + name + " and process all other traffic"));
        mModeButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
        mModeButton.getButtons().addAll(mIncludeButton, mExcludeButton);
        AccessibilityUtil.labelGroup(new Label(name + " Filter Mode"), mModeButton);
        mModeButton.getToggleGroup().selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null)
            {
                //Ensure that one toggle is always selected
                oldValue.setSelected(true);
            }
            else if(oldValue != null)
            {
                updateStatus();
                modified();
            }
        });

        mValuesField.setPrefWidth(220);
        mValuesField.setTooltip(new Tooltip("One or more " + name + " values, separated by commas or spaces"));
        mValuesField.setAccessibleText(name + " filter values");
        mValuesField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateStatus();
            modified();
        });

        mHexButton.setUserData(Format.HEX);
        mHexButton.setTooltip(new Tooltip("Enter and display values in hexadecimal"));
        mDecimalButton.setUserData(Format.DECIMAL);
        mDecimalButton.setTooltip(new Tooltip("Enter and display values in decimal"));
        mFormatButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
        mFormatButton.getButtons().addAll(mHexButton, mDecimalButton);
        AccessibilityUtil.labelGroup(new Label(name + " Format"), mFormatButton);
        mFormatButton.getToggleGroup().selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null)
            {
                //Ensure that one toggle is always selected
                oldValue.setSelected(true);
            }
            else if(oldValue != null && !mRevertingFormat)
            {
                if(reformatValues((Format)oldValue.getUserData(), (Format)newValue.getUserData()))
                {
                    modified();
                }
                else
                {
                    //The values can't be converted: keep the current format so that the values aren't saved in the
                    //other format
                    mRevertingFormat = true;
                    oldValue.setSelected(true);
                    mRevertingFormat = false;
                    mStatusLabel.setStyle("-fx-text-fill: red;");
                    mStatusLabel.setText("Correct the invalid " + mName + " values before changing the format");
                }
            }
        });

        getChildren().addAll(mEnabledSwitch, filterLabel, mModeButton, mValuesField);

        if(mFormatSelectable)
        {
            getChildren().add(mFormatButton);
        }

        getChildren().add(mStatusLabel);

        setFilter(null);
        setEditorDisabled(true);
    }

    /**
     * Loads the filter into the editor controls.
     * @param filter to load, or null to clear the controls
     */
    public void setFilter(AccessCodeFilter filter)
    {
        mLoading = true;

        AccessCodeFilter loaded = filter != null ? filter : new AccessCodeFilter(mDefaultFormat);
        Format format = mFormatSelectable ? loaded.getFormat() : mDefaultFormat;

        mEnabledSwitch.setSelected(loaded.isEnabled());
        selectToggle(mModeButton, loaded.getMode());
        selectToggle(mFormatButton, format);
        mValuesField.setText(loaded.format(format, mHexDigits));

        mLoading = false;
        updateControlStates();
    }

    /**
     * Creates a filter from the current editor control values.  Values that can't be parsed or that are out of
     * range are not included in the filter; the status label shows them to the user.
     */
    public AccessCodeFilter getFilter()
    {
        Format format = getSelectedFormat();
        AccessCodeFilter filter = new AccessCodeFilter(format);
        filter.setEnabled(mEnabledSwitch.isSelected());
        filter.setMode(getSelectedMode());
        filter.setValueSet(AccessCodeFilter.parse(mValuesField.getText(), format, mMaximum).values());
        return filter;
    }

    /**
     * Enables or disables the whole control, e.g. when there is no channel configuration to edit.
     */
    public void setEditorDisabled(boolean disabled)
    {
        mEditorDisabled = disabled;
        mEnabledSwitch.setDisable(mEditorDisabled || mUnavailableReason != null);
        updateControlStates();
    }

    /**
     * Marks the filter as unavailable for the current configuration (e.g. a mode where the access code isn't
     * decoded), disabling the controls and showing the reason in place of the filter status.
     * @param reason to show, or null when the filter is available
     */
    public void setUnavailable(String reason)
    {
        mUnavailableReason = reason;
        setEditorDisabled(mEditorDisabled);
    }

    private void updateControlStates()
    {
        boolean disabled = mEnabledSwitch.isDisabled() || !mEnabledSwitch.isSelected();
        mModeButton.setDisable(disabled);
        mFormatButton.setDisable(disabled);
        mValuesField.setDisable(disabled);
        updateStatus();
    }

    private void updateStatus()
    {
        mStatusLabel.setStyle(null);

        if(mUnavailableReason != null && !mEditorDisabled)
        {
            mStatusLabel.setText(mUnavailableReason);
            return;
        }

        if(mEnabledSwitch.isDisabled() || !mEnabledSwitch.isSelected())
        {
            mStatusLabel.setText("");
            return;
        }

        AccessCodeFilter.ParseResult result = AccessCodeFilter.parse(mValuesField.getText(), getSelectedFormat(),
            mMaximum);

        if(!result.isValid())
        {
            mStatusLabel.setStyle("-fx-text-fill: red;");
            mStatusLabel.setText("Invalid " + mName + ": " + String.join(", ", result.invalid()) + " (range " +
                AccessCodeFilter.format(List.of(0), getSelectedFormat(), mHexDigits) + "-" +
                AccessCodeFilter.format(List.of(mMaximum), getSelectedFormat(), mHexDigits) +
                ") - not saved");
        }
        else if(result.values().isEmpty())
        {
            mStatusLabel.setText("No " + mName + " values entered - filter is inactive");
        }
        else if(getSelectedMode() == Mode.INCLUDE)
        {
            mStatusLabel.setText("Only traffic with a listed " + mName + " is processed");
        }
        else
        {
            mStatusLabel.setText("Traffic with a listed " + mName + " is dropped");
        }
    }

    /**
     * Converts the entered values from one format to the other.
     * @return true if converted, or false when the current text has invalid values and is left unchanged so that the
     * user can correct it
     */
    private boolean reformatValues(Format from, Format to)
    {
        AccessCodeFilter.ParseResult result = AccessCodeFilter.parse(mValuesField.getText(), from, mMaximum);

        if(result.isValid())
        {
            mValuesField.setText(AccessCodeFilter.format(result.values(), to, mHexDigits));
            return true;
        }

        return false;
    }

    private Mode getSelectedMode()
    {
        Toggle selected = mModeButton.getToggleGroup().getSelectedToggle();
        return selected != null ? (Mode)selected.getUserData() : Mode.INCLUDE;
    }

    private Format getSelectedFormat()
    {
        if(!mFormatSelectable)
        {
            return mDefaultFormat;
        }

        Toggle selected = mFormatButton.getToggleGroup().getSelectedToggle();
        return selected != null ? (Format)selected.getUserData() : mDefaultFormat;
    }

    private static void selectToggle(SegmentedButton segmentedButton, Object userData)
    {
        for(ToggleButton button: segmentedButton.getButtons())
        {
            if(button.getUserData() == userData)
            {
                button.setSelected(true);
            }
        }
    }

    private void modified()
    {
        if(!mLoading && mModifiedProperty != null)
        {
            mModifiedProperty.set(true);
        }
    }
}
