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

package io.github.dsheirer.gui.settings.web;

import io.github.dsheirer.gui.map.MapView;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.web.WebServerSettings;
import io.github.dsheirer.web.WebServer;
import io.github.dsheirer.web.WebServerStatus;
import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.controlsfx.control.ToggleSwitch;

/**
 * Web server settings: enable, listen address and port, TLS keystore, session timeout, and the server status with the
 * initial administrator's one-time password.
 */
public class WebServerSettingsPage extends SettingsPage
{
    private static final String ADDRESS_LOCAL = "This computer only (127.0.0.1)";
    private static final String ADDRESS_ALL = "All network interfaces (0.0.0.0)";

    private final WebServerSettings mWebSettings;
    private final WebServer mWebServer;
    private final Label mStatusLabel = new Label();
    private final Hyperlink mStatusLink = new Hyperlink();
    private final TextField mInitialPasswordField = new TextField();
    private final Label mInitialPasswordHelp = new Label();
    private final HBox mInitialPasswordBox = new HBox(6, mInitialPasswordField);

    /**
     * Constructs an instance
     * @param settings to edit
     * @param webServer for the status, or null when there isn't one
     */
    public WebServerSettingsPage(Settings settings, WebServer webServer)
    {
        super(SettingsPageType.WEB_SERVER, settings);
        mWebSettings = settings.getWebServerSettings();
        mWebServer = webServer;

        SettingsSection server = addSection("Server");
        ToggleSwitch enabled = new ToggleSwitch();
        enabled.setSelected(mWebSettings.isStoredEnabled());
        enabled.selectedProperty().addListener((observable, oldValue, selected) -> mWebSettings.setEnabled(selected));
        server.addRow("Enable the web server", enabled)
            .defaultValue(enabled.selectedProperty(), WebServerSettings.DEFAULT_ENABLED, enabled::setSelected)
            .help("Serves the web API, so browsers and scripts can use sdrtrunk.  Users sign in with accounts that " +
                "are stored in the call database.");

        ComboBox<String> address = createAddressComboBox();
        address.setPrefWidth(260);
        server.addRow("Listen on", address)
            .defaultValue(address.valueProperty(), ADDRESS_LOCAL, address::setValue)
            .help("'This computer only' is the safe choice.  To reach sdrtrunk from other computers, choose all " +
                "network interfaces or type an address, and turn on TLS or use a reverse proxy with TLS.");

        Spinner<Integer> port = new Spinner<>(WebServerSettings.MIN_PORT, WebServerSettings.MAX_PORT,
            mWebSettings.getStoredPort(), 1);
        port.setEditable(true);
        port.setPrefWidth(100);
        port.valueProperty().addListener((observable, oldValue, value) -> mWebSettings.setPort(value));
        server.addRow("Port", port)
            .defaultValue(port.valueProperty(), WebServerSettings.DEFAULT_PORT,
                value -> port.getValueFactory().setValue(value));

        Spinner<Integer> timeout = new Spinner<>(WebServerSettings.MIN_SESSION_TIMEOUT_MINUTES,
            WebServerSettings.MAX_SESSION_TIMEOUT_MINUTES, mWebSettings.getSessionTimeoutMinutes(), 5);
        timeout.setEditable(true);
        timeout.setPrefWidth(100);
        timeout.valueProperty().addListener((observable, oldValue, value) ->
            mWebSettings.setSessionTimeoutMinutes(value));
        HBox timeoutBox = new HBox(6, timeout, new Label("minutes"));
        timeoutBox.setAlignment(Pos.CENTER_RIGHT);
        server.addRow("Sign out browsers after", timeoutBox)
            .defaultValue(timeout.valueProperty(), WebServerSettings.DEFAULT_SESSION_TIMEOUT_MINUTES,
                value -> timeout.getValueFactory().setValue(value))
            .help("A browser session ends after this long without a request.");

        SettingsSection tls = addSection("HTTPS (TLS)");
        ToggleSwitch tlsEnabled = new ToggleSwitch();
        tlsEnabled.setSelected(mWebSettings.isTlsEnabled());
        tlsEnabled.selectedProperty().addListener((observable, oldValue, selected) ->
            mWebSettings.setTlsEnabled(selected));
        tls.addRow("Use HTTPS", tlsEnabled)
            .defaultValue(tlsEnabled.selectedProperty(), false, tlsEnabled::setSelected)
            .help("Encrypts passwords, cookies and data.  Needs a PKCS12 keystore (.p12) with the server " +
                "certificate and private key.");

        TextField keystore = new TextField(mWebSettings.getTlsKeystorePath());
        keystore.setPrefColumnCount(24);
        keystore.setOnAction(event -> mWebSettings.setTlsKeystorePath(keystore.getText()));
        keystore.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused && !keystore.getText().equals(mWebSettings.getTlsKeystorePath()))
            {
                mWebSettings.setTlsKeystorePath(keystore.getText());
            }
        });
        Button browse = new Button("Browse...");
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select the PKCS12 Keystore");
            chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PKCS12 keystore",
                "*.p12", "*.pfx"), new FileChooser.ExtensionFilter("All files", "*.*"));
            File file = chooser.showOpenDialog(getScene().getWindow());

            if(file != null)
            {
                keystore.setText(file.getAbsolutePath());
                mWebSettings.setTlsKeystorePath(file.getAbsolutePath());
            }
        });
        tls.addRow("Keystore file", keystore, browse);

        PasswordField password = new PasswordField();
        password.setText(mWebSettings.getTlsKeystorePassword());
        password.setPrefColumnCount(16);
        password.setOnAction(event -> mWebSettings.setTlsKeystorePassword(password.getText()));
        password.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused && !password.getText().equals(mWebSettings.getTlsKeystorePassword()))
            {
                mWebSettings.setTlsKeystorePassword(password.getText());
            }
        });
        tls.addRow("Keystore password", password)
            .help("Stored in plain text in the Java preferences store, like the database password.");

        SettingsSection status = addSection("Status");
        mStatusLabel.setWrapText(true);
        mStatusLabel.setMaxWidth(420);
        mStatusLink.setPadding(Insets.EMPTY);
        mStatusLink.setOnAction(event -> {
            MapView.browse(mStatusLink.getText());
            mStatusLink.setVisited(false);
        });
        showLink(null);
        HBox statusBox = new HBox(4, mStatusLabel, mStatusLink);
        statusBox.setAlignment(Pos.BASELINE_LEFT);
        status.addRow("Web server", statusBox);

        mInitialPasswordField.setEditable(false);
        mInitialPasswordField.setPrefColumnCount(20);
        mInitialPasswordHelp.setWrapText(true);
        status.addRow("Initial admin password", mInitialPasswordBox)
            .help("Sign in as 'admin' with this one-time password and choose a new password.  It is also in " +
                "the application log.");

        if(mWebSettings.hasOverrides())
        {
            status.addHelp("Launcher options (--web, --web-bind, --web-port) override these settings until the " +
                "next start.");
        }

        if(mWebServer != null)
        {
            mWebServer.addStatusListener(serverStatus -> Platform.runLater(() -> showStatus(serverStatus)));
            showStatus(mWebServer.getStatus());
        }
        else
        {
            mStatusLabel.setText("Not available");
        }
    }

    private ComboBox<String> createAddressComboBox()
    {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(ADDRESS_LOCAL, ADDRESS_ALL);
        comboBox.setEditable(true);
        HBox.setHgrow(comboBox, Priority.NEVER);
        String stored = mWebSettings.getStoredBindAddress();
        comboBox.setValue(toLabel(stored));
        comboBox.valueProperty().addListener((observable, oldValue, value) -> {
            String address = toAddress(value);

            if(address != null && !address.equals(mWebSettings.getStoredBindAddress()))
            {
                mWebSettings.setBindAddress(address);
            }
        });
        return comboBox;
    }

    private static String toLabel(String address)
    {
        return switch(address)
        {
            case WebServerSettings.LOOPBACK_ADDRESS -> ADDRESS_LOCAL;
            case WebServerSettings.ALL_ADDRESSES -> ADDRESS_ALL;
            default -> address;
        };
    }

    private static String toAddress(String label)
    {
        if(label == null || label.isBlank())
        {
            return null;
        }

        return switch(label)
        {
            case ADDRESS_LOCAL -> WebServerSettings.LOOPBACK_ADDRESS;
            case ADDRESS_ALL -> WebServerSettings.ALL_ADDRESSES;
            default -> label.trim();
        };
    }

    private void showStatus(WebServerStatus status)
    {
        mStatusLabel.setText(switch(status.state())
        {
            case RUNNING -> status.url() != null ? "Running at" : "Running";
            case FAILED -> "Couldn't start: " + status.error();
            case STOPPED -> "Off";
        });
        showLink(status.state() == WebServerStatus.State.RUNNING ? status.url() : null);

        String initialPassword = mWebServer.getAccountService().getPendingInitialPassword().orElse(null);
        mInitialPasswordField.setText(initialPassword != null ? initialPassword : "");
        mInitialPasswordBox.setDisable(initialPassword == null);
        mInitialPasswordField.setPromptText(initialPassword == null ? "Already changed" : null);
    }

    /**
     * Shows the server's address as a link that opens it in the browser, or hides the link when url is null
     */
    private void showLink(String url)
    {
        mStatusLink.setText(url);
        mStatusLink.setVisible(url != null);
        mStatusLink.setManaged(url != null);
    }
}
