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

package io.github.dsheirer.gui.preference.application;

import io.github.dsheirer.gui.theme.Theme;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.application.ApplicationPreference;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Appearance preferences - selects the active UI {@link Theme} and the global GUI zoom factor for
 * both the Swing main window and the JavaFX panels.
 */
public class AppearancePreferenceEditor extends HBox
{
    private final ApplicationPreference mApplicationPreference;
    private GridPane mEditorPane;
    private VBox mThemeRadioGroup;
    private Slider mGuiScaleSlider;
    private Label mGuiScaleValueLabel;

    public AppearancePreferenceEditor(UserPreferences userPreferences)
    {
        mApplicationPreference = userPreferences.getApplicationPreference();
        setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.setMaxHeight(Double.MAX_VALUE);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.getChildren().add(getEditorPane());
        HBox.setHgrow(vbox, Priority.ALWAYS);
        getChildren().add(vbox);
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            int row = 0;
            mEditorPane = new GridPane();
            mEditorPane.setMaxWidth(Double.MAX_VALUE);
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(8);
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            mEditorPane.add(new Label("Theme"), 0, row, 3, 1);
            mEditorPane.add(getThemeRadioGroup(), 0, ++row, 3, 1);

            Separator separator = new Separator(Orientation.HORIZONTAL);
            GridPane.setHgrow(separator, Priority.ALWAYS);
            mEditorPane.add(separator, 0, ++row, 3, 1);

            mEditorPane.add(new Label("GUI Scale"), 0, ++row, 3, 1);

            HBox sliderRow = new HBox(8);
            sliderRow.setPadding(new Insets(2, 0, 2, 4));
            HBox.setHgrow(getGuiScaleSlider(), Priority.ALWAYS);
            Button resetButton = new Button("Reset");
            resetButton.setOnAction(e -> {
                getGuiScaleSlider().setValue(ApplicationPreference.DEFAULT_GUI_SCALE);
                mApplicationPreference.setGuiScale(ApplicationPreference.DEFAULT_GUI_SCALE);
            });
            sliderRow.getChildren().addAll(getGuiScaleSlider(), getGuiScaleValueLabel(), resetButton);
            mEditorPane.add(sliderRow, 0, ++row, 3, 1);

            Label hint = new Label("Adjusts the size of all UI components. Takes effect immediately.");
            hint.setWrapText(true);
            mEditorPane.add(hint, 0, ++row, 3, 1);

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(30);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setHgrow(Priority.ALWAYS);
            mEditorPane.getColumnConstraints().addAll(c1, c2);
        }

        return mEditorPane;
    }

    /**
     * Vertical group of radio buttons - one per {@link Theme}.  Selecting one persists the theme
     * via {@link ApplicationPreference#setTheme(Theme)} which triggers a global preference event
     * and a re-theme through the ThemeManager.
     */
    private VBox getThemeRadioGroup()
    {
        if(mThemeRadioGroup == null)
        {
            mThemeRadioGroup = new VBox(4);
            mThemeRadioGroup.setPadding(new Insets(2, 0, 2, 4));
            ToggleGroup group = new ToggleGroup();
            Theme current = mApplicationPreference.getTheme();

            for(Theme theme: Theme.values())
            {
                RadioButton rb = new RadioButton(theme.getDisplayName());
                rb.setToggleGroup(group);
                rb.setUserData(theme);
                rb.setSelected(theme == current);
                rb.setOnAction(e -> {
                    if(rb.isSelected())
                    {
                        mApplicationPreference.setTheme((Theme) rb.getUserData());
                    }
                });
                mThemeRadioGroup.getChildren().add(rb);
            }
        }

        return mThemeRadioGroup;
    }

    /**
     * Slider to adjust the global GUI zoom factor.  The slider snaps to 10% increments and only
     * persists the value when the user releases the slider so we do not thrash a LAF reinstall on
     * every intermediate value during a drag.
     */
    private Slider getGuiScaleSlider()
    {
        if(mGuiScaleSlider == null)
        {
            mGuiScaleSlider = new Slider(ApplicationPreference.MIN_GUI_SCALE,
                    ApplicationPreference.MAX_GUI_SCALE,
                    mApplicationPreference.getGuiScale());
            mGuiScaleSlider.setMajorTickUnit(0.25d);
            mGuiScaleSlider.setMinorTickCount(1);
            mGuiScaleSlider.setSnapToTicks(true);
            mGuiScaleSlider.setShowTickMarks(true);
            mGuiScaleSlider.setShowTickLabels(false);
            mGuiScaleSlider.setBlockIncrement(0.1d);

            //Update the live percentage label as the user drags.
            mGuiScaleSlider.valueProperty().addListener((obs, oldV, newV) -> {
                if(newV != null)
                {
                    getGuiScaleValueLabel().setText(formatScale(newV.doubleValue()));
                }
            });

            //Persist + apply only when the user finishes the drag - avoids reinstalling the LAF
            //on every intermediate frame.
            mGuiScaleSlider.valueChangingProperty().addListener((obs, wasChanging, changing) -> {
                if(wasChanging && !changing)
                {
                    mApplicationPreference.setGuiScale(mGuiScaleSlider.getValue());
                }
            });

            //Cover keyboard / scroll-wheel adjustments which never enter the changing=true state.
            mGuiScaleSlider.setOnMouseReleased(e -> {
                if(!mGuiScaleSlider.isValueChanging())
                {
                    mApplicationPreference.setGuiScale(mGuiScaleSlider.getValue());
                }
            });
        }

        return mGuiScaleSlider;
    }

    private Label getGuiScaleValueLabel()
    {
        if(mGuiScaleValueLabel == null)
        {
            mGuiScaleValueLabel = new Label(formatScale(mApplicationPreference.getGuiScale()));
            mGuiScaleValueLabel.setMinWidth(50);
        }

        return mGuiScaleValueLabel;
    }

    private static String formatScale(double scale)
    {
        return Math.round(scale * 100) + "%";
    }
}
