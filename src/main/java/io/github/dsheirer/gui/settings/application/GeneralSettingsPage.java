/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.gui.settings.application;

import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.application.ApplicationSettings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * General application settings: diagnostic monitoring and the channel auto-start timeout.
 */
public class GeneralSettingsPage extends SettingsPage
{
    private ApplicationSettings mApplicationSettings;
    private Spinner<Integer> mTimeoutSpinner;
    private ToggleSwitch mAutomaticDiagnosticMonitoringToggle;

    /**
     * Constructs an instance
     * @param settings for obtaining reference to setting.
     */
    public GeneralSettingsPage(Settings settings)
    {
        super(SettingsPageType.GENERAL, settings);
        mApplicationSettings = settings.getApplicationSettings();

        SettingsSection monitoring = addSection("Diagnostics");
        monitoring.addRow("Enable diagnostic monitoring", getAutomaticDiagnosticMonitoringToggle())
            .defaultValue(getAutomaticDiagnosticMonitoringToggle().selectedProperty(),
                ApplicationSettings.DEFAULT_AUTOMATIC_DIAGNOSTIC_MONITORING,
                getAutomaticDiagnosticMonitoringToggle()::setSelected)
            .help("Monitors application health and writes diagnostic reports when it detects a problem.");

        SettingsSection channels = addSection("Channels");
        HBox timeout = new HBox(6, getTimeoutSpinner(), new Label("seconds"));
        timeout.setAlignment(Pos.CENTER_RIGHT);
        channels.addRow("Channel auto-start timeout", timeout)
            .defaultValue(getTimeoutSpinner().valueProperty(), ApplicationSettings.DEFAULT_CHANNEL_AUTO_START_TIMEOUT,
                value -> getTimeoutSpinner().getValueFactory().setValue(value))
            .help("Countdown at application start-up before the channels that are set to auto-start are started.");
    }
    /**
     * Spinner to select channel auto-start timeout value in range 0-30 seconds.
     * @return spinner
     */
    private Spinner<Integer> getTimeoutSpinner()
    {
        if(mTimeoutSpinner == null)
        {
            mTimeoutSpinner = new Spinner<>(0, 30, mApplicationSettings.getChannelAutoStartTimeout(), 1);
            mTimeoutSpinner.setPrefWidth(80);
            mTimeoutSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mApplicationSettings.setChannelAutoStartTimeout(newValue));
        }

        return mTimeoutSpinner;
    }

    /**
     * Toggle switch to enable/disable automatic diagnostic monitoring.
     */
    private ToggleSwitch getAutomaticDiagnosticMonitoringToggle()
    {
        if(mAutomaticDiagnosticMonitoringToggle == null)
        {
            mAutomaticDiagnosticMonitoringToggle = new ToggleSwitch();
            mAutomaticDiagnosticMonitoringToggle.setSelected(mApplicationSettings.isAutomaticDiagnosticMonitoring());
            mAutomaticDiagnosticMonitoringToggle.selectedProperty().addListener((observable, oldValue, enabled) ->
                    mApplicationSettings.setAutomaticDiagnosticMonitoring(enabled));
        }

        return mAutomaticDiagnosticMonitoringToggle;
    }
}
