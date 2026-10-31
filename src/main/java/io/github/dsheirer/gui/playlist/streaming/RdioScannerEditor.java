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

import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration;
import io.github.dsheirer.audio.broadcast.thinline.ThinlineRadioConfiguration;
import io.github.dsheirer.audio.broadcast.thinline.ThinlineRadioCoreConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
    private Button mSwitchToThinlineButton;
    private Button mSwitchToCoreButton;
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

        if(mSwitchToThinlineButton != null)
        {
            mSwitchToThinlineButton.setDisable(item == null);
        }

        if(mSwitchToCoreButton != null)
        {
            mSwitchToCoreButton.setDisable(item == null);
        }

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
            getItem().setApiKey(showsApiKey() ? getApiKeyTextField().getText() : "");
            getItem().setMaximumRecordingAge(getMaxAgeTextField().get() * 1000);
        }

        super.save();
    }

    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.RDIOSCANNER_CALL;
    }

    /**
     * Label for the server URL field. Subclasses can override this for another product that uses the same API.
     */
    protected String getHostLabel()
    {
        return "RdioScanner URL";
    }

    /**
     * Label for the credential field.
     */
    protected String getApiKeyLabel()
    {
        return "API Key";
    }

    /**
     * Core uses the site key from User Preferences and does not store one on the stream.
     */
    protected boolean showsApiKey()
    {
        return true;
    }

    /**
     * True when User Preferences has the Thinline Radio site key.
     */
    static boolean siteKeyReady(PlaylistManager playlistManager)
    {
        if(playlistManager == null || playlistManager.getUserPreferences() == null)
        {
            return false;
        }
        String key = playlistManager.getUserPreferences().getThinlineRadioPreference().getApiKey();
        return key != null && !key.isBlank();
    }

    protected boolean hasSiteKey()
    {
        return siteKeyReady(getPlaylistManager());
    }

    /**
     * Optional control under the URL row.
     */
    protected Node getTrailingNode()
    {
        return null;
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

            if(showsApiKey())
            {
                Label apiKeyLabel = new Label(getApiKeyLabel());
                GridPane.setHalignment(apiKeyLabel, HPos.RIGHT);
                GridPane.setConstraints(apiKeyLabel, 0, ++row);
                mEditorPane.getChildren().add(apiKeyLabel);

                GridPane.setConstraints(getApiKeyTextField(), 1, row);
                mEditorPane.getChildren().add(getApiKeyTextField());
            }

            Label systemIdLabel = new Label("System ID");
            GridPane.setHalignment(systemIdLabel, HPos.RIGHT);
            GridPane.setConstraints(systemIdLabel, 0, ++row);
            mEditorPane.getChildren().add(systemIdLabel);

            GridPane.setConstraints(getSystemIdTextField(), 1, row);
            mEditorPane.getChildren().add(getSystemIdTextField());

            Label hostLabel = new Label(getHostLabel());
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

            Node trailing = getTrailingNode();
            if(trailing != null)
            {
                GridPane.setConstraints(trailing, 1, ++row);
                mEditorPane.getChildren().add(trailing);
            }

            if(getBroadcastServerType() == BroadcastServerType.RDIOSCANNER_CALL)
            {
                GridPane.setConstraints(getSwitchToThinlineButton(), 1, ++row);
                mEditorPane.getChildren().add(getSwitchToThinlineButton());
            }

            if(getBroadcastServerType() == BroadcastServerType.RDIOSCANNER_CALL ||
                getBroadcastServerType() == BroadcastServerType.THINLINE_RADIO)
            {
                GridPane.setConstraints(getSwitchToCoreButton(), 1, ++row);
                mEditorPane.getChildren().add(getSwitchToCoreButton());
            }

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

    protected TextField getHostTextField()
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

    private Button getSwitchToThinlineButton()
    {
        if(mSwitchToThinlineButton == null)
        {
            mSwitchToThinlineButton = new Button("Switch to Thinline Radio");
            mSwitchToThinlineButton.setDisable(true);
            mSwitchToThinlineButton.setTooltip(new Tooltip("Upload calls from this stream to Thinline Radio instead of Rdio Scanner"));
            mSwitchToThinlineButton.setOnAction(event -> switchTo(BroadcastServerType.THINLINE_RADIO, mSwitchToThinlineButton));
        }

        return mSwitchToThinlineButton;
    }

    private Button getSwitchToCoreButton()
    {
        if(mSwitchToCoreButton == null)
        {
            mSwitchToCoreButton = new Button("Switch to Thinline Radio Core");
            mSwitchToCoreButton.setDisable(getItem() == null);
            mSwitchToCoreButton.setTooltip(new Tooltip("Hold each call on this site until ThinLine chooses which recording to upload"));
            mSwitchToCoreButton.setOnAction(event -> switchTo(BroadcastServerType.THINLINE_RADIO_CORE, mSwitchToCoreButton));
        }

        return mSwitchToCoreButton;
    }

    /**
     * Replaces this stream with another Thinline stream type and keeps the name, system, and URL.
     */
    protected void switchTo(BroadcastServerType target, Node owner)
    {
        RdioScannerConfiguration current = getItem();

        if(current == null || current.getBroadcastServerType() == target || owner == null || owner.getScene() == null)
        {
            return;
        }

        boolean core = target == BroadcastServerType.THINLINE_RADIO_CORE;
        if(core && !hasSiteKey())
        {
            Alert missing = new Alert(Alert.AlertType.INFORMATION,
                "Open User Preferences, choose Thinline Radio, and enter the site key. Thinline Radio Core uses that key.",
                ButtonType.OK);
            missing.setTitle("Site key required");
            missing.setHeaderText("Add the site key before using Thinline Radio Core");
            missing.initOwner(owner.getScene().getWindow());
            missing.showAndWait();
            return;
        }
        String message = core
            ? "The name, system ID, and URL stay with this stream. It uses the site key from User Preferences. Calls stay on this computer until ThinLine asks for one of them."
            : "The name, system ID, and URL stay with this stream. Put an upload API key in the key field, then save the playlist. Calls upload as soon as they finish.";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.NO, ButtonType.YES);
        alert.setTitle(core ? "Switch to Thinline Radio Core" : "Switch to Thinline Radio");
        alert.setHeaderText(core ? "Hold calls until ThinLine chooses one?" : "Upload each call immediately?");
        alert.initOwner(owner.getScene().getWindow());
        alert.setResizable(true);
        alert.onShownProperty().addListener(e -> javafx.application.Platform.runLater(() -> alert.setResizable(false)));

        alert.showAndWait().ifPresent(buttonType -> {
            if(buttonType != ButtonType.YES)
            {
                return;
            }

            RdioScannerConfiguration replacement = core ? toCoreConfiguration(current) : toThinlineConfiguration(current);
            String previousName = current.getName();
            String updatedName = replacement.getName();

            if(previousName != null && !previousName.isEmpty() && updatedName != null &&
                !updatedName.contentEquals(previousName) &&
                getPlaylistManager().getAliasModel().hasAliasesWithBroadcastChannel(previousName))
            {
                Alert aliasAlert = new Alert(Alert.AlertType.CONFIRMATION);
                aliasAlert.getButtonTypes().clear();
                aliasAlert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
                aliasAlert.setTitle("Update Aliases");
                aliasAlert.setHeaderText("Rename requires updating aliases for this stream");
                aliasAlert.setContentText("Do you want to update aliases to new stream name?");
                aliasAlert.initOwner(owner.getScene().getWindow());
                aliasAlert.setResizable(true);
                aliasAlert.onShownProperty().addListener(e ->
                    javafx.application.Platform.runLater(() -> aliasAlert.setResizable(false)));
                aliasAlert.showAndWait().ifPresent(aliasButton -> {
                    if(aliasButton == ButtonType.YES)
                    {
                        getPlaylistManager().getAliasModel().updateBroadcastChannel(previousName, updatedName);
                    }
                });
            }

            modifiedProperty().set(false);

            StreamingEditor streamingEditor = getStreamingEditor(owner);

            if(streamingEditor != null)
            {
                streamingEditor.replaceBroadcastConfiguration(current, replacement);
            }
            else
            {
                getPlaylistManager().getBroadcastModel().removeBroadcastConfiguration(current);
                getPlaylistManager().getBroadcastModel().addBroadcastConfiguration(replacement);
            }
        });
    }

    private ThinlineRadioConfiguration toThinlineConfiguration(RdioScannerConfiguration current)
    {
        BroadcastFormat format = current.getBroadcastFormat() != null ? current.getBroadcastFormat() : BroadcastFormat.MP3;
        ThinlineRadioConfiguration thinline = new ThinlineRadioConfiguration(format);
        thinline.setName(getNameTextField().getText());
        thinline.setEnabled(getEnabledSwitch().isSelected());
        thinline.setPassword(current.getPassword());
        thinline.setPort(current.getPort());
        thinline.setDelay(current.getDelay());

        int systemID = getSystemIdTextField().get() != null ? getSystemIdTextField().get() : 0;
        thinline.setSystemID(systemID);
        thinline.setApiKey(getApiKeyTextField().getText());

        String host = getHostTextField().getText();

        if(host != null)
        {
            host = host.replace(API_PATH, "");
            host += API_PATH;
            thinline.setHost(host);
        }
        else
        {
            thinline.setHost(current.getHost());
        }

        Integer maxAgeSeconds = getMaxAgeTextField().get();

        if(maxAgeSeconds != null)
        {
            thinline.setMaximumRecordingAge(maxAgeSeconds * 1000L);
        }
        else
        {
            thinline.setMaximumRecordingAge(current.getMaximumRecordingAge());
        }

        return thinline;
    }

    private ThinlineRadioCoreConfiguration toCoreConfiguration(RdioScannerConfiguration current)
    {
        BroadcastFormat format = current.getBroadcastFormat() != null ? current.getBroadcastFormat() : BroadcastFormat.MP3;
        ThinlineRadioCoreConfiguration core = new ThinlineRadioCoreConfiguration(format);
        core.setName(getNameTextField().getText());
        core.setEnabled(getEnabledSwitch().isSelected());
        core.setPassword(current.getPassword());
        core.setPort(current.getPort());
        core.setDelay(current.getDelay());

        int systemID = getSystemIdTextField().get() != null ? getSystemIdTextField().get() : 0;
        core.setSystemID(systemID);
        core.setApiKey("");

        String host = getHostTextField().getText();

        if(host != null)
        {
            host = host.replace(API_PATH, "");
            host += API_PATH;
            core.setHost(host);
        }
        else
        {
            core.setHost(current.getHost());
        }

        Integer maxAgeSeconds = getMaxAgeTextField().get();

        if(maxAgeSeconds != null)
        {
            core.setMaximumRecordingAge(maxAgeSeconds * 1000L);
        }
        else
        {
            core.setMaximumRecordingAge(current.getMaximumRecordingAge());
        }

        return core;
    }

    private StreamingEditor getStreamingEditor(Node start)
    {
        Node node = start;

        while(node != null)
        {
            if(node instanceof StreamingEditor streamingEditor)
            {
                return streamingEditor;
            }

            node = node.getParent();
        }

        return null;
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
