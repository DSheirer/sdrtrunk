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

package io.github.dsheirer.gui.settings.tuner;

import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.source.ChannelizerType;
import io.github.dsheirer.settings.source.TunerSettings;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

/**
 * Tuner settings: the channelizer type and the RSPduo selection mode
 */
public class TunerSettingsPage extends SettingsPage
{
    private static final String HELP_TEXT_POLYPHASE = "Polyphase (default) processes all channels from the tuner.  " +
        "This channelizer is more efficient when decoding 3 or more channels.";
    private static final String HELP_TEXT_HETERODYNE = "Heterodyne processes each channel on-demand.  This " +
        "channelizer may work better for computers with constrained resources when processing a small number of channels.";

    private TunerSettings mTunerSettings;
    private ChoiceBox<ChannelizerType> mChannelizerTypeChoiceBox;
    private ChoiceBox<RspDuoSelectionMode> mRspDuoTunerModeChoiceBox;

    public TunerSettingsPage(Settings settings)
    {
        super(SettingsPageType.TUNERS, settings);
        mTunerSettings = settings.getTunerSettings();

        SettingsSection channelizer = addSection("Channelizer");
        channelizer.addRow("Channelizer type", getChannelizerTypeChoiceBox()).requiresRestart()
            .defaultValue(getChannelizerTypeChoiceBox().valueProperty(), TunerSettings.DEFAULT_CHANNELIZER_TYPE,
                getChannelizerTypeChoiceBox()::setValue)
            .help(HELP_TEXT_POLYPHASE).help(HELP_TEXT_HETERODYNE);

        SettingsSection rspDuo = addSection("SDRplay RSPduo");
        rspDuo.addRow("RSPduo selection mode", getRspDuoTunerModeChoiceBox()).requiresRestart()
            .defaultValue(getRspDuoTunerModeChoiceBox().valueProperty(), TunerSettings.DEFAULT_RSP_DUO_TUNER_MODE,
                getRspDuoTunerModeChoiceBox()::setValue);
    }

    private ChoiceBox<ChannelizerType> getChannelizerTypeChoiceBox()
    {
        if(mChannelizerTypeChoiceBox == null)
        {
            mChannelizerTypeChoiceBox = new ChoiceBox<>();
            mChannelizerTypeChoiceBox.getItems().addAll(ChannelizerType.values());

            ChannelizerType current = mTunerSettings.getChannelizerType();
            mChannelizerTypeChoiceBox.getSelectionModel().select(current);

            mChannelizerTypeChoiceBox.setOnAction(event -> {
                ChannelizerType selected = mChannelizerTypeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerSettings.setChannelizerType(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Restart Required");
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getChannelizerTypeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mChannelizerTypeChoiceBox;
    }

    private ChoiceBox<RspDuoSelectionMode> getRspDuoTunerModeChoiceBox()
    {
        if(mRspDuoTunerModeChoiceBox == null)
        {
            mRspDuoTunerModeChoiceBox = new ChoiceBox<>();
            mRspDuoTunerModeChoiceBox.getItems().addAll(RspDuoSelectionMode.values());

            RspDuoSelectionMode current = mTunerSettings.getRspDuoTunerMode();
            mRspDuoTunerModeChoiceBox.getSelectionModel().select(current);

            mRspDuoTunerModeChoiceBox.setOnAction(event -> {
                RspDuoSelectionMode selected = mRspDuoTunerModeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerSettings.setRspDuoTunerMode(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Restart Required");
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getRspDuoTunerModeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mRspDuoTunerModeChoiceBox;
    }
}
