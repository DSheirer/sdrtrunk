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

package io.github.dsheirer.gui.settings.decoder;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.jmbe.JmbeCreator;
import io.github.dsheirer.jmbe.JmbeEditorRequest;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.jmbe.github.Version;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.decoder.JmbeLibrarySettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.util.ThreadPool;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for the JMBE library that decodes digital voice audio
 */
public class AudioCodecSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioCodecSettingsPage.class);
    private static final String PATH_NOT_SET = "(not set)";
    private static final String CREATE_LIBRARY = "Create Library";
    private static final String CHECK_FOR_UPDATE = "Check For Library Update";
    private static final String LIBRARY_NOT_SETUP = "None.  Click Select or Create Library button to setup.";

    private Settings mSettings;
    private Label mJmbeVersionLabel;
    private Label mPathToJmbeLibraryLabel;
    private Button mSelectButton;
    private Button mResetButton;
    private Button mCreateButton;
    private ToggleSwitch mAlertUserWhenMissingSwitch;

    public AudioCodecSettingsPage(Settings settings)
    {
        super(SettingsPageType.AUDIO_CODECS, settings);
        mSettings = settings;

        //Register to receive JMBE library setting update notifications so we can update the version and path
        MyEventBus.getGlobalEventBus().register(this);

        SettingsSection library = addSection("JMBE Audio Library");
        library.addHelp("Create Library downloads the JMBE creator from GitHub and uses it to build the library on " +
            "this computer.");
        library.addRow("Current version", getJmbeVersionLabel());
        library.addRow("File", getPathToJmbeLibraryLabel());
        HBox buttons = new HBox(10, getCreateButton(), getSelectButton(), getResetButton());
        library.addContent(buttons);

        SettingsSection alerts = addSection("Alerts");
        alerts.addRow("Alert when a decoder requires the missing JMBE library", getAlertUserWhenMissingSwitch())
            .defaultValue(getAlertUserWhenMissingSwitch().selectedProperty(),
                JmbeLibrarySettings.DEFAULT_ALERT_IF_MISSING_LIBRARY, getAlertUserWhenMissingSwitch()::setSelected);
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    private ToggleSwitch getAlertUserWhenMissingSwitch()
    {
        if(mAlertUserWhenMissingSwitch == null)
        {
            mAlertUserWhenMissingSwitch = new ToggleSwitch();
            mAlertUserWhenMissingSwitch.setSelected(mSettings.getJmbeLibrarySettings().getAlertIfMissingLibraryRequired());
            mAlertUserWhenMissingSwitch.selectedProperty().addListener((observable, oldValue, alert) ->
                mSettings.getJmbeLibrarySettings().setAlertIfMissingLibraryRequired(alert));
        }

        return mAlertUserWhenMissingSwitch;
    }

    private Button getCreateButton()
    {
        if(mCreateButton == null)
        {
            mCreateButton = new Button();
            mCreateButton.setText(mSettings.getJmbeLibrarySettings()
                .getPathJmbeLibrary() != null ? CHECK_FOR_UPDATE : CREATE_LIBRARY);
            mCreateButton.setOnAction(event -> checkForUpdatedLibrary());
        }

        return mCreateButton;
    }

    /**
     * Checks for availability of an updated JMBE library
     */
    private void checkForUpdatedLibrary()
    {
        ThreadPool.CACHED.execute(() -> {
            try
            {
                Version current = mSettings.getJmbeLibrarySettings().getCurrentVersion();
                final Release release = GitHub.getLatestRelease(JmbeCreator.GITHUB_JMBE_RELEASES_URL,
                    JmbeCreator.JMBE_API_MAJOR_VERSION);

                mLog.info("Checking for JMBE Library Updates for API major version " +
                    JmbeCreator.JMBE_API_MAJOR_VERSION + " ...");
                mLog.info("Current: " + (current != null ? current.toString() : "empty"));

                //Offer the latest matching release when there is no current library, when the current library is a
                //different (incompatible) major version, or when the matching release is newer than the current one.
                final boolean canUpdate = (release != null) && ((current == null) ||
                    !current.hasMajor() || current.getMajor() != JmbeCreator.JMBE_API_MAJOR_VERSION ||
                    (release.getVersion().compareTo(current) > 0));

                if(release != null)
                {
                    mLog.info("Available: " + release.getVersion().toString());
                }

                if(canUpdate)
                {
                    mLog.info("JMBE Library update is available");
                }
                else
                {
                    mLog.info("No JMBE library update is available at this time");
                }

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            if(canUpdate)
                            {
                                String content = "JMBE library version " + release.getVersion().toString() +
                                    " is available.  Would you like to download the latest source code and " +
                                    "create an updated JMBE library?";
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.YES, ButtonType.NO);
                                alert.setResizable(true);

                                //Workaroud for dialog sizing issue on Windows
                                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                                alert.setTitle("JMBE Library Update Check");
                                alert.setHeaderText("Update is available");
                                alert.initOwner(getCreateButton().getScene().getWindow());
                                alert.showAndWait().ifPresent(buttonType -> {
                                    if(buttonType == ButtonType.YES)
                                    {
                                        mLog.info("Posting JMBE editor request to event bus");
                                        MyEventBus.getGlobalEventBus().post(new JmbeEditorRequest(release));
                                    }
                                });
                            }
                            else
                            {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                    "No JMBE library update is available.", ButtonType.OK);
                                alert.setTitle("JMBE Library Update Check");
                                if(release != null)
                                {
                                    alert.setHeaderText("No update available");
                                }
                                else
                                {
                                    alert.setHeaderText("Unable to determine latest release version.  Check network connection");
                                }
                                alert.initOwner(getCreateButton().getScene().getWindow());
                                alert.showAndWait();
                            }
                        }
                        catch(Throwable t)
                        {
                            mLog.error("Error during JavaFX portion of create library");
                        }
                    }
                });
            }
            catch(Throwable t)
            {
                mLog.error("Error during create library", t);
            }
        });
    }

    private Label getJmbeVersionLabel()
    {
        if(mJmbeVersionLabel == null)
        {
            mJmbeVersionLabel = new Label();
            Version version = mSettings.getJmbeLibrarySettings().getCurrentVersion();

            if(version != null)
            {
                mJmbeVersionLabel.setText(version.toString());
            }
            else
            {
                mJmbeVersionLabel.setText(LIBRARY_NOT_SETUP);
            }
        }

        return mJmbeVersionLabel;
    }

    private Button getSelectButton()
    {
        if(mSelectButton == null)
        {
            mSelectButton = new Button("Select...");
            mSelectButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select JMBE Audio Library Location");

                    Path path = mSettings.getDirectorySettings().getDefaultJmbeDirectory();

                    if(Files.exists(path))
                    {
                        fileChooser.setInitialDirectory(path.toFile());
                    }

                    Stage stage = (Stage)getSelectButton().getScene().getWindow();
                    File selected = fileChooser.showOpenDialog(stage);

                    if(selected != null)
                    {
                        mSettings.getJmbeLibrarySettings().setPathJmbeLibrary(selected.toPath());
                    }
                }
            });
        }

        return mSelectButton;
    }

    private Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.setOnAction(event -> mSettings.getJmbeLibrarySettings().resetPathJmbeLibrary());
        }

        return mResetButton;
    }

    private Label getPathToJmbeLibraryLabel()
    {
        if(mPathToJmbeLibraryLabel == null)
        {
            Path path = mSettings.getJmbeLibrarySettings().getPathJmbeLibrary();
            mPathToJmbeLibraryLabel = new Label(path != null ? path.toString() : PATH_NOT_SET);
            mPathToJmbeLibraryLabel.setWrapText(true);
        }

        return mPathToJmbeLibraryLabel;
    }

    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType != null && settingsType == SettingsType.JMBE_LIBRARY)
        {
            Path path = mSettings.getJmbeLibrarySettings().getPathJmbeLibrary();
            getPathToJmbeLibraryLabel().setText(path != null ? path.toString() : PATH_NOT_SET);
            Version version = mSettings.getJmbeLibrarySettings().getCurrentVersion();
            getJmbeVersionLabel().setText(version != null ? version.toString() : LIBRARY_NOT_SETUP);
            getCreateButton().setText(mSettings.getJmbeLibrarySettings()
                .getPathJmbeLibrary() != null ? CHECK_FOR_UPDATE : CREATE_LIBRARY);
            getAlertUserWhenMissingSwitch().setSelected(mSettings.getJmbeLibrarySettings()
                .getAlertIfMissingLibraryRequired());
        }
    }
}
