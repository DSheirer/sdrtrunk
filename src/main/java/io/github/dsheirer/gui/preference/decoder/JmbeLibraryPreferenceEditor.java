/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.preference.decoder;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.decoder.JmbeLibraryPreference;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;


/**
 * Preference settings for decoders
 */
public class JmbeLibraryPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibraryPreferenceEditor.class);

    private static final String PATH_NOT_SET = "(not set)";
    private JmbeLibraryPreference mJmbeLibraryPreference;
    private GridPane mEditorPane;
    private Label mJmbeLibraryLabel;
    private Button mPathToJmbeLibraryButton;
    private Button mPathToJmbeLibraryResetButton;
    private Label mPathToJmbeLibraryLabel;

    public JmbeLibraryPreferenceEditor(UserPreferences userPreferences)
    {
        mJmbeLibraryPreference = userPreferences.getJmbeLibraryPreference();

        //Register to receive directory preference update notifications so we can update the path labels
        MyEventBus.getEventBus().register(this);

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            int row = 0;

            GridPane.setMargin(getJmbeLibraryLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getJmbeLibraryLabel(), 0, row);

            GridPane.setMargin(getPathToJmbeLibraryLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getPathToJmbeLibraryLabel(), 1, row);

            GridPane.setMargin(getPathToJmbeLibraryButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getPathToJmbeLibraryButton(), 2, row);

            GridPane.setMargin(getPathToJmbeLibraryResetButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getPathToJmbeLibraryResetButton(), 3, row++);
        }

        return mEditorPane;
    }

    private Label getJmbeLibraryLabel()
    {
        if(mJmbeLibraryLabel == null)
        {
            mJmbeLibraryLabel = new Label("JMBE Audio Library:");
        }

        return mJmbeLibraryLabel;
    }

    private Button getPathToJmbeLibraryButton()
    {
        if(mPathToJmbeLibraryButton == null)
        {
            mPathToJmbeLibraryButton = new Button("Change...");
            mPathToJmbeLibraryButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select JMBE Audio Library Location");
                    Stage stage = (Stage)getPathToJmbeLibraryButton().getScene().getWindow();
                    File selected = fileChooser.showOpenDialog(stage);

                    if(selected != null)
                    {
                        mJmbeLibraryPreference.setPathJmbeLibrary(selected.toPath());
                    }
                }
            });
        }

        return mPathToJmbeLibraryButton;
    }

    private Button getPathToJmbeLibraryResetButton()
    {
        if(mPathToJmbeLibraryResetButton == null)
        {
            mPathToJmbeLibraryResetButton = new Button("Reset");
            mPathToJmbeLibraryResetButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mJmbeLibraryPreference.resetPathJmbeLibrary();
                }
            });
        }

        return mPathToJmbeLibraryResetButton;
    }

    private Label getPathToJmbeLibraryLabel()
    {
        if(mPathToJmbeLibraryLabel == null)
        {
            Path path = mJmbeLibraryPreference.getPathJmbeLibrary();
            mPathToJmbeLibraryLabel = new Label(path != null ? path.toString() : PATH_NOT_SET);
        }

        return mPathToJmbeLibraryLabel;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType != null && preferenceType == PreferenceType.JMBE_LIBRARY)
        {
            Path path = mJmbeLibraryPreference.getPathJmbeLibrary();
            getPathToJmbeLibraryLabel().setText(path != null ? path.toString() : PATH_NOT_SET);
        }
    }
}
