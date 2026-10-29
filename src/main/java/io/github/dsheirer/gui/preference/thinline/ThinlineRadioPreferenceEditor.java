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

package io.github.dsheirer.gui.preference.thinline;

import io.github.dsheirer.audio.broadcast.thinline.ThinlineRadioSocket;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.thinline.ThinlineRadioPreference;
import io.github.dsheirer.util.ThreadPool;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * User preference editor for the Thinline Radio site-health report.
 */
public class ThinlineRadioPreferenceEditor extends HBox
{
    private final ThinlineRadioPreference mPreference;
    private GridPane mEditorPane;

    public ThinlineRadioPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getThinlineRadioPreference();
        setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox();
        box.setMaxHeight(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(getEditorPane());
        HBox.setHgrow(box, Priority.ALWAYS);
        getChildren().add(box);
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setMaxWidth(Double.MAX_VALUE);
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(8);
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            int row = 0;
            Label intro = new Label("Opens one websocket to Thinline Radio. This SDRTrunk sends site health and upload check-ins. Thinline Radio admins can control this SDRTrunk remotely, limited by the choices at the bottom of this page.");
            intro.setWrapText(true);
            mEditorPane.add(intro, 0, row, 2, 1);

            ToggleSwitch enabled = new ToggleSwitch();
            enabled.setSelected(mPreference.isEnabled());
            enabled.selectedProperty().addListener((observable, oldValue, value) -> mPreference.setEnabled(value));
            GridPane.setHalignment(enabled, HPos.RIGHT);
            mEditorPane.add(enabled, 0, ++row);
            mEditorPane.add(new Label("Report site health to Thinline Radio"), 1, row);

            mEditorPane.add(new Label("Thinline Radio URL"), 0, ++row);
            TextField url = new TextField(mPreference.getUrl());
            url.setPromptText("https://scanner.example.com");
            url.setMaxWidth(Double.MAX_VALUE);
            url.focusedProperty().addListener((observable, oldValue, focused) -> {
                if(!focused)
                {
                    mPreference.setUrl(url.getText());
                }
            });
            mEditorPane.add(url, 1, row);

            mEditorPane.add(new Label("Health API key"), 0, ++row);
            PasswordField apiKey = new PasswordField();
            apiKey.setText(mPreference.getApiKey());
            apiKey.setPromptText("API key used only for site health");
            apiKey.setMaxWidth(Double.MAX_VALUE);
            apiKey.focusedProperty().addListener((observable, oldValue, focused) -> {
                if(!focused)
                {
                    mPreference.setApiKey(apiKey.getText());
                }
            });
            mEditorPane.add(apiKey, 1, row);

            Button test = new Button("Test Connection");
            Label result = new Label();
            result.setWrapText(true);
            result.setStyle("-fx-text-fill: -fx-text-background-color;");
            test.setOnAction(event -> {
                mPreference.setUrl(url.getText());
                mPreference.setApiKey(apiKey.getText());
                String host = url.getText();
                String key = apiKey.getText();
                test.setDisable(true);
                result.setText("Connecting…");
                ThreadPool.CACHED.submit(() -> {
                    String error = ThinlineRadioSocket.testConnection(host, key);
                    Platform.runLater(() -> {
                        test.setDisable(false);
                        result.setText(error == null ? "Connected." : error);
                    });
                });
            });
            HBox testRow = new HBox(8, test, result);
            testRow.setAlignment(Pos.CENTER_LEFT);
            mEditorPane.add(testRow, 1, ++row);

            mEditorPane.add(new Label("Push these errors"), 0, ++row, 2, 1);
            mEditorPane.add(check("Tuner error", mPreference.isAlertTuner(), mPreference::setAlertTuner), 1, ++row);
            mEditorPane.add(check("Control channel lost", mPreference.isAlertControl(), mPreference::setAlertControl), 1, ++row);
            mEditorPane.add(check("Uploader error", mPreference.isAlertUploader(), mPreference::setAlertUploader), 1, ++row);
            mEditorPane.add(check("Sample overflow", mPreference.isAlertOverflow(), mPreference::setAlertOverflow), 1, ++row);
            mEditorPane.add(check("SDRTrunk offline", mPreference.isAlertOffline(), mPreference::setAlertOffline), 1, ++row);

            mEditorPane.add(new Label("Let Thinline Radio admins"), 0, ++row, 2, 1);
            Label remoteHint = new Label("Status and settings are always visible. Uncheck anything Thinline Radio must not change from the admin page.");
            remoteHint.setWrapText(true);
            mEditorPane.add(remoteHint, 1, ++row);
            for(ThinlineRadioPreference.Remote remote: ThinlineRadioPreference.Remote.values())
            {
                mEditorPane.add(check(remote.toString(), mPreference.isRemoteAllowed(remote),
                    allowed -> mPreference.setRemoteAllowed(remote, allowed)), 1, ++row);
            }

            ColumnConstraints labels = new ColumnConstraints();
            labels.setPercentWidth(28);
            ColumnConstraints fields = new ColumnConstraints();
            fields.setHgrow(Priority.ALWAYS);
            mEditorPane.getColumnConstraints().addAll(labels, fields);
        }

        return mEditorPane;
    }

    private CheckBox check(String label, boolean selected, Consumer<Boolean> save)
    {
        CheckBox box = new CheckBox(label);
        box.setSelected(selected);
        box.selectedProperty().addListener((observable, oldValue, value) -> save.accept(value));
        return box;
    }
}
