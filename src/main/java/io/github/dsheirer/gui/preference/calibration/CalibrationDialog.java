/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.calibration;

import io.github.dsheirer.preference.UserPreferences;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Dialog to prompt the user to run the calibrations.
 */
public class CalibrationDialog extends Dialog
{
    private UserPreferences mUserPreferences;

    public CalibrationDialog(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        setTitle("Calibrate CPU");
        setHeaderText("Perform CPU Calibration?");

        Label instructionsLabel = new Label("The sdrtrunk application can run more efficiently by using Vector or Single " +
                "Instruction Multiple Data (SIMD) CPU operations.  sdrtrunk must calibrate your CPU in order " +
                "to determine the best implementation.\n\n" +
                "Calibration takes a few minutes to complete.\n\n" +
                "If you click 'Cancel', you will be prompted next time you run sdrtrunk, or you can open the " +
                "User Preferences dialog from the View menu and manually start the calibration process.");
        instructionsLabel.setPrefWidth(300);
        instructionsLabel.setWrapText(true);

        CheckBox doNotShowAgainCheckBox = new CheckBox("Do not show again");
        doNotShowAgainCheckBox.setOnAction(event ->
        {
            mUserPreferences.getVectorCalibrationPreference().setHideCalibrationDialog(doNotShowAgainCheckBox.isSelected());
        });

        VBox.setVgrow(instructionsLabel, Priority.ALWAYS);
        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(10));
        contentBox.setSpacing(20);
        contentBox.getChildren().addAll(instructionsLabel, doNotShowAgainCheckBox);

        getDialogPane().setContent(contentBox);

        ButtonType calibrateButtonType = new ButtonType("Calibrate", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(calibrateButtonType, cancelButtonType);
    }
}
