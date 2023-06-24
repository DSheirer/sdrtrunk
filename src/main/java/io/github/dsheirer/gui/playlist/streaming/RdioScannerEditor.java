/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RdioScanner calls API configuration editor
 */
public class RdioScannerEditor extends AbstractBroadcastEditor<RdioScannerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(RdioScannerEditor.class);
    private static final String API_PATH = "/api/call-upload";
    private IntegerTextField mSystemIdTextField;
    private IntegerTextField mMaxAgeTextField;
    private TextField mApiKeyTextField;
    private TextField mHostTextField;
    private GridPane mEditorPane;

    /**
     * Constructs an instance
     * @param playlistManager for accessing the broadcast model
     */
    public RdioScannerEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(RdioScannerConfiguration item)
    {
        super.setItem(item);

        getSystemIdTextField().setDisable(item == null);
        getApiKeyTextField().setDisable(item == null);
        getHostTextField().setDisable(item == null);
        getMaxAgeTextField().setDisable(item == null);

        if(item != null)
        {
            getSystemIdTextField().set(item.getSystemID());
            getApiKeyTextField().setText(item.getApiKey());
            String url = item.getHost();

            if(url != null)
            {
                url = url.replace(API_PATH, "");
            }

            getHostTextField().setText(url);
            getMaxAgeTextField().set((int)(item.getMaximumRecordingAge() / 1000));
        }
        else
        {
            getSystemIdTextField().set(0);
            getApiKeyTextField().setText(null);
            getHostTextField().setText(null);
            getMaxAgeTextField().set(0);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            int systemID = getSystemIdTextField().get() != null ? getSystemIdTextField().get() : 0;
            getItem().setSystemID(systemID);
            String host = getHostTextField().getText();
            if(host != null)
            {
                host = host.replace(API_PATH, "");
                host += API_PATH;
                getItem().setHost(host);
            }
            getItem().setApiKey(getApiKeyTextField().getText());
            getItem().setMaximumRecordingAge(getMaxAgeTextField().get() * 1000);
        }

        super.save();
    }

    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.RDIOSCANNER_CALL;
    }

    protected GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 5, 10,10));
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(5);

            int row = 0;

            Label formatLabel = new Label("Format");
            GridPane.setHalignment(formatLabel, HPos.RIGHT);
            GridPane.setConstraints(formatLabel, 0, row);
            mEditorPane.getChildren().add(formatLabel);

            GridPane.setConstraints(getFormatField(), 1, row);
            mEditorPane.getChildren().add(getFormatField());

            Label enabledLabel = new Label("Enabled");
            GridPane.setHalignment(enabledLabel, HPos.RIGHT);
            GridPane.setConstraints(enabledLabel, 2, row);
            mEditorPane.getChildren().add(enabledLabel);

            GridPane.setConstraints(getEnabledSwitch(), 3, row);
            mEditorPane.getChildren().add(getEnabledSwitch());

            Label systemLabel = new Label("Name");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, ++row);
            mEditorPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getNameTextField(), 1, row);
            mEditorPane.getChildren().add(getNameTextField());

            Label apiKeyLabel = new Label("API Key");
            GridPane.setHalignment(apiKeyLabel, HPos.RIGHT);
            GridPane.setConstraints(apiKeyLabel, 0, ++row);
            mEditorPane.getChildren().add(apiKeyLabel);

            GridPane.setConstraints(getApiKeyTextField(), 1, row);
            mEditorPane.getChildren().add(getApiKeyTextField());

            Label systemIdLabel = new Label("System ID");
            GridPane.setHalignment(systemIdLabel, HPos.RIGHT);
            GridPane.setConstraints(systemIdLabel, 0, ++row);
            mEditorPane.getChildren().add(systemIdLabel);

            GridPane.setConstraints(getSystemIdTextField(), 1, row);
            mEditorPane.getChildren().add(getSystemIdTextField());

            Label hostLabel = new Label("RdioScanner URL");
            GridPane.setHalignment(hostLabel, HPos.RIGHT);
            GridPane.setConstraints(hostLabel, 0, ++row);
            mEditorPane.getChildren().add(hostLabel);

            GridPane.setConstraints(getHostTextField(), 1, row);
            mEditorPane.getChildren().add(getHostTextField());

            Label apiPath = new Label("/api/call-upload");
            GridPane.setHalignment(apiPath, HPos.LEFT);
            GridPane.setConstraints(apiPath, 2, row);
            mEditorPane.getChildren().add(apiPath);

            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, ++row);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, row);
            mEditorPane.getChildren().add(getMaxAgeTextField());

        }

        return mEditorPane;
    }

    private IntegerTextField getMaxAgeTextField()
    {
        if(mMaxAgeTextField == null)
        {
            mMaxAgeTextField = new IntegerTextField();
            mMaxAgeTextField.setDisable(true);
            mMaxAgeTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaxAgeTextField;
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

    private TextField getApiKeyTextField()
    {
        if(mApiKeyTextField == null)
        {
            mApiKeyTextField = new TextField();
            mApiKeyTextField.setDisable(true);
            mApiKeyTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mApiKeyTextField;
    }

    private IntegerTextField getSystemIdTextField()
    {
        if(mSystemIdTextField == null)
        {
            mSystemIdTextField = new IntegerTextField();
            mSystemIdTextField.setDisable(true);
            mSystemIdTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mSystemIdTextField;
    }


}
