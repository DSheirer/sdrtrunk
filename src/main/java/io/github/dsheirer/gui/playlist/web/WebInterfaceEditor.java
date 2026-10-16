package io.github.dsheirer.gui.playlist.web;

import io.github.dsheirer.web.SdrTrunkWebServer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Playlist editor controls for the embedded web interface. */
public class WebInterfaceEditor extends VBox
{
    private final PasswordField mPassword = new PasswordField();
    private final Label mStatus = new Label();

    public WebInterfaceEditor()
    {
        setPadding(new Insets(20));
        setSpacing(14);
        Label title = new Label("Web Interface");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label help = new Label("Manage browser access and restart the embedded web server. " +
            "The SDRTRUNK_WEB_PASSWORD environment variable overrides this saved password.");
        help.setWrapText(true);

        mPassword.setText(SdrTrunkWebServer.getSavedPassword());
        mPassword.setPromptText("Web console password");
        mPassword.setPrefColumnCount(40);

        Button save = new Button("Save Password");
        save.setOnAction(event -> savePassword());
        Button restart = new Button("Restart Web Interface");
        restart.setOnAction(event -> restart());

        GridPane controls = new GridPane();
        controls.setHgap(10);
        controls.setVgap(10);
        controls.add(new Label("Password:"), 0, 0);
        controls.add(mPassword, 1, 0);
        controls.add(save, 0, 1);
        controls.add(restart, 1, 1);

        refreshStatus();
        getChildren().addAll(title, help, controls, mStatus,
            new Label("Web address: http://<this-computer-address>:" + SdrTrunkWebServer.DEFAULT_PORT + "/"));
    }

    private void savePassword()
    {
        String password = mPassword.getText() == null ? "" : mPassword.getText().trim();
        if(password.isBlank())
        {
            mStatus.setText("Password cannot be empty.");
            return;
        }
        SdrTrunkWebServer.savePassword(password);
        mStatus.setText("Password saved and applied. Existing browser sessions were signed out.");
    }

    private void restart()
    {
        try
        {
            SdrTrunkWebServer.restartActive();
            mStatus.setText("Web interface restarted successfully.");
        }
        catch(Exception e)
        {
            mStatus.setText("Unable to restart: " + e.getMessage());
        }
    }

    private void refreshStatus()
    {
        mStatus.setText(SdrTrunkWebServer.isRunning() ? "Status: Running" : "Status: Stopped");
    }
}
