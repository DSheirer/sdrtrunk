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
package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.pcmlan.PcmLanConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Stream configuration editor for the PCM-over-LAN broadcaster.  Streams raw 16-bit signed
 * little-endian mono PCM at 8 kHz to a remote TCP listener (e.g. OpenToneDetect).
 */
public class PcmLanStreamEditor extends AbstractBroadcastEditor<PcmLanConfiguration>
{
    private GridPane mEditorPane;
    private TextField mHostTextField;
    private IntegerTextField mPortTextField;
    private IntegerTextField mOutputSampleRateTextField;

    public PcmLanStreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.PCM_LAN;
    }

    @Override
    public void setItem(PcmLanConfiguration item)
    {
        super.setItem(item);

        getHostTextField().setDisable(item == null);
        getPortTextField().setDisable(item == null);
        getOutputSampleRateTextField().setDisable(item == null);

        if(item != null)
        {
            getHostTextField().setText(item.getHost());
            getPortTextField().set(item.getPort());
            getOutputSampleRateTextField().set(item.getOutputSampleRate());
        }
        else
        {
            getHostTextField().setText(null);
            getPortTextField().set(0);
            getOutputSampleRateTextField().set(8000);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        BroadcastConfiguration configuration = getItem();
        if(configuration != null)
        {
            configuration.setHost(getHostTextField().getText());
            configuration.setPort(getPortTextField().get());
            if(configuration instanceof PcmLanConfiguration pcmConfig)
            {
                pcmConfig.setOutputSampleRate(getOutputSampleRateTextField().get());
            }
        }
        super.save();
    }

    @Override
    public void dispose()
    {
    }

    @Override
    protected GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 5, 10, 10));
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(5);

            Label formatLabel = new Label("Format");
            GridPane.setHalignment(formatLabel, HPos.RIGHT);
            GridPane.setConstraints(formatLabel, 0, 0);
            mEditorPane.getChildren().add(formatLabel);

            getFormatField().setText(getBroadcastServerType().toString());
            GridPane.setConstraints(getFormatField(), 1, 0);
            mEditorPane.getChildren().add(getFormatField());

            Label enabledLabel = new Label("Enabled");
            GridPane.setHalignment(enabledLabel, HPos.RIGHT);
            GridPane.setConstraints(enabledLabel, 2, 0);
            mEditorPane.getChildren().add(enabledLabel);

            GridPane.setConstraints(getEnabledSwitch(), 3, 0);
            mEditorPane.getChildren().add(getEnabledSwitch());

            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, 1);
            mEditorPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameTextField(), 1, 1);
            mEditorPane.getChildren().add(getNameTextField());

            Label hostLabel = new Label("Host");
            GridPane.setHalignment(hostLabel, HPos.RIGHT);
            GridPane.setConstraints(hostLabel, 0, 2);
            mEditorPane.getChildren().add(hostLabel);

            GridPane.setConstraints(getHostTextField(), 1, 2);
            mEditorPane.getChildren().add(getHostTextField());

            Label portLabel = new Label("Port");
            GridPane.setHalignment(portLabel, HPos.RIGHT);
            GridPane.setConstraints(portLabel, 2, 2);
            mEditorPane.getChildren().add(portLabel);

            GridPane.setConstraints(getPortTextField(), 3, 2);
            mEditorPane.getChildren().add(getPortTextField());

            Label rateLabel = new Label("Output Rate (Hz)");
            GridPane.setHalignment(rateLabel, HPos.RIGHT);
            GridPane.setConstraints(rateLabel, 0, 3);
            mEditorPane.getChildren().add(rateLabel);

            GridPane.setConstraints(getOutputSampleRateTextField(), 1, 3);
            mEditorPane.getChildren().add(getOutputSampleRateTextField());

            TextArea info = new TextArea(
                "Streams 16-bit signed little-endian mono PCM to the configured TCP listener.\n" +
                "Output Rate controls the sample rate of the streamed audio. Default 8000 Hz (native passthrough).\n" +
                "Set to match the receiver's expected rate (e.g. 12000 for OpenToneDetect at 12 kHz).\n" +
                "SDRTrunk acts as the TCP client; the remote process must be the TCP server.");
            info.setEditable(false);
            info.setWrapText(true);
            info.setPrefRowCount(4);
            GridPane.setConstraints(info, 0, 4, 4, 1);
            mEditorPane.getChildren().add(info);
        }

        return mEditorPane;
    }

    private TextField getHostTextField()
    {
        if(mHostTextField == null)
        {
            mHostTextField = new TextField();
            mHostTextField.setDisable(true);
            mHostTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mHostTextField;
    }

    private IntegerTextField getPortTextField()
    {
        if(mPortTextField == null)
        {
            mPortTextField = new IntegerTextField();
            mPortTextField.setDisable(true);
            mPortTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mPortTextField;
    }

    private IntegerTextField getOutputSampleRateTextField()
    {
        if(mOutputSampleRateTextField == null)
        {
            mOutputSampleRateTextField = new IntegerTextField();
            mOutputSampleRateTextField.setDisable(true);
            mOutputSampleRateTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mOutputSampleRateTextField;
    }
}
