/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.jmbe;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.jmbe.github.Version;
import io.github.dsheirer.preference.UserPreferences;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Editor pane for JMBE Library creator
 */
public class JmbeEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeEditor.class);
    private UserPreferences mUserPreferences;
    private Release mCurrentRelease;
    private Label mCurrentVersionLabel;
    private Label mUpdatedVersionLabel;
    private Label mLibraryPathLabel;
    private Button mChangeDirectoryButton;
    private GridPane mLabelGridPane;
    private Path mLibraryDirectoryPath;
    private Button mCreateButton;
    private TextArea mConsoleTextArea;

    /**
     * Constructs an instance
     * @param userPreferences
     */
    public JmbeEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        setPadding(new Insets(10,10,10,10));
        setSpacing(10);
        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(getConsoleTextArea(), Priority.ALWAYS);
        getChildren().addAll(getLabelGridPane(), separator, getCreateButton(), getConsoleTextArea());
    }

    /**
     * Processes a request to configure the editor to build the release version specified in the request.
     * @param request to build a specific release version
     */
    public void process(JmbeEditorRequest request)
    {
        if(request != null)
        {
            setCurrentRelease(request.getCurrentRelease());
        }
    }

    private TextArea getConsoleTextArea()
    {
        if(mConsoleTextArea == null)
        {
            mConsoleTextArea = new TextArea();
            mConsoleTextArea.setMaxHeight(Double.MAX_VALUE);
            mConsoleTextArea.setMaxWidth(Double.MAX_VALUE);
        }

        return mConsoleTextArea;
    }

    private Button getCreateButton()
    {
        if(mCreateButton == null)
        {
            mCreateButton = new Button("Create Library");
            mCreateButton.setOnAction(event -> {
                String noticeText = "The JMBE library is available as source code for educational purposes " +
                    "only.  It is a written description of how certain voice encoding/decoding algorithms " +
                    "could be implemented. Executable objects compiled or derived from this package may " +
                    "be covered by one or more patents. Users are strongly advised to check for any " +
                    "patent restrictions or licensing requirements before compiling or using this " +
                    "source code.\n\nClicking the YES button indicates that you are requesting to download " +
                    "the JMBE source code and compile the JMBE library.";

                Alert alert = new Alert(Alert.AlertType.WARNING, noticeText, ButtonType.YES, ButtonType.NO);
                alert.setResizable(true);

                //Workaroud for dialog sizing issue on Windows
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                alert.setTitle("Download and Compile JMBE Library?");
                alert.setHeaderText("Do you want to continue?");
                alert.initOwner(getCreateButton().getScene().getWindow());
                alert.showAndWait().ifPresent(buttonType -> {
                    if(buttonType == ButtonType.YES)
                    {
                        Path library = getLibraryDirectoryPath().resolve(getJarName(mCurrentRelease.getVersion()));
                        getCreateButton().setDisable(true);
                        createJmbeLibrary(library);
                    }
                });
            });
        }

        return mCreateButton;
    }

    /**
     * Downloads the JMBE creator and creates the library using the specified path argument.
     * @param library path
     */
    private void createJmbeLibrary(Path library)
    {
        JmbeCreator jmbeCreator = new JmbeCreator(mCurrentRelease, library);
        getConsoleTextArea().textProperty().bind(jmbeCreator.consoleOutputProperty());
        jmbeCreator.completeProperty().addListener((observable, oldValue, newValue) -> {
            getCreateButton().setDisable(false);
            boolean failed = jmbeCreator.hasErrors();
            String content = null;
            if(failed)
            {
                content = "JMBE library creation attempt failed.  Please update the library manually by " +
                    "downloading the JMBE creator application.";
            }
            else
            {
                content = "JMBE library successfully created/updated";
                mUserPreferences.getJmbeLibraryPreference().setPathJmbeLibrary(jmbeCreator.getLibraryPath());
                getCurrentVersionLabel().setText(mCurrentRelease.getVersion().toString());
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
            alert.setTitle("JMBE Library Creator");
            alert.setHeaderText((failed ? "Attempt failed." : "Success!") + " Please click OK to close this window");
            alert.initOwner(getCreateButton().getScene().getWindow());
            alert.showAndWait().ifPresent(buttonType -> {
                jmbeCreator.completeProperty().unbind();
                MyEventBus.getGlobalEventBus().post(new JmbeEditorRequest(true));
            });
        });
        jmbeCreator.execute();
    }

    /**
     * Creates a jar name for the specified release version
     * @param version of the GitHub JMBE release
     * @return JMBE library jar name
     */
    public static String getJarName(Version version)
    {
        String name = version.toString();
        name = name.replace("v", "");
        return "jmbe-" + name + ".jar";
    }

    private GridPane getLabelGridPane()
    {
        if(mLabelGridPane == null)
        {
            mLabelGridPane = new GridPane();
            mLabelGridPane.setHgap(10);
            mLabelGridPane.setVgap(10);

            int row = 0;

            Label topLabel = new Label("Create or Update JMBE Library");
            mLabelGridPane.add(topLabel, 0, row, 2, 1);

            Label currentLabel = new Label("Current Version:");
            GridPane.setHalignment(currentLabel, HPos.RIGHT);
            mLabelGridPane.add(currentLabel, 0, ++row);
            mLabelGridPane.add(getCurrentVersionLabel(), 1, row);

            Label updateLabel = new Label("Updating To Version:");
            GridPane.setHalignment(updateLabel, HPos.RIGHT);
            mLabelGridPane.add(updateLabel, 0, ++row);
            mLabelGridPane.add(getUpdatedVersionLabel(), 1, row);

            Label pathLabel = new Label("Store Library At:");
            GridPane.setHalignment(pathLabel, HPos.RIGHT);
            mLabelGridPane.add(pathLabel, 0, ++row);
            mLabelGridPane.add(getLibraryPathLabel(), 1, row);

            mLabelGridPane.add(getChangeDirectoryButton(), 1, ++row);
        }

        return mLabelGridPane;
    }

    private Path getLibraryDirectoryPath()
    {
        if(mLibraryDirectoryPath == null)
        {
            mLibraryDirectoryPath = mUserPreferences.getDirectoryPreference().getDefaultJmbeDirectory();
        }

        return mLibraryDirectoryPath;
    }

    private Button getChangeDirectoryButton()
    {
        if(mChangeDirectoryButton == null)
        {
            mChangeDirectoryButton = new Button("Change Directory ...");
            mChangeDirectoryButton.setOnAction(event -> {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setInitialDirectory(getLibraryDirectoryPath().toFile());
                chooser.setTitle("JMBE Library Directory");
                File newDirectory = chooser.showDialog(getChangeDirectoryButton().getScene().getWindow());
                setLibraryDirectoryPath(newDirectory.toPath());
            });
        }

        return mChangeDirectoryButton;
    }

    /**
     * Sets the library path and updates the display label
     */
    private void setLibraryDirectoryPath(Path path)
    {
        if(path != null)
        {
            mLibraryDirectoryPath = path;
            getLibraryPathLabel().setText(mLibraryDirectoryPath.toString());
        }
    }

    private Label getLibraryPathLabel()
    {
        if(mLibraryPathLabel == null)
        {
            mLibraryPathLabel = new Label();

            if(getLibraryDirectoryPath() != null)
            {
                mLibraryPathLabel.setText(getLibraryDirectoryPath().toString());
            }
        }

        return mLibraryPathLabel;
    }

    private Label getCurrentVersionLabel()
    {
        if(mCurrentVersionLabel == null)
        {
            mCurrentVersionLabel = new Label("(empty)");

            Version currentVersion = mUserPreferences.getJmbeLibraryPreference().getCurrentVersion();

            if(currentVersion != null)
            {
                mCurrentVersionLabel.setText(currentVersion.toString());
            }
        }

        return mCurrentVersionLabel;
    }

    private Label getUpdatedVersionLabel()
    {
        if(mUpdatedVersionLabel == null)
        {
            mUpdatedVersionLabel = new Label("(empty)");
        }

        return mUpdatedVersionLabel;
    }

    /**
     * Current release version available from GitHub.
     * @return current release or null if the release hasn't been set.
     */
    private Release getCurrentRelease()
    {
        return mCurrentRelease;
    }

    /**
     * Sets the current release version available from GitHub.
     * @param release version
     */
    public void setCurrentRelease(Release release)
    {
        mCurrentRelease = release;

        if(release != null)
        {
            Version updateVersion = release.getVersion();

            if(updateVersion != null)
            {
                getUpdatedVersionLabel().setText(updateVersion.toString());
            }
        }
    }
}
