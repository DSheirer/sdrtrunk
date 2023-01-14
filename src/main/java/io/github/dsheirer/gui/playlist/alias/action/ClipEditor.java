/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.action;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasFactory;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.util.ThreadPool;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Audio clip action editor
 */
public class ClipEditor extends ActionEditor<ClipAction>
{
    private static final Logger mLog = LoggerFactory.getLogger(ClipEditor.class);
    private static final String UNTIL_DISMISSED_LABEL = "second intervals (1-60)";
    private static final String DELAY_LABEL = "second delay (1-60)";
    private static final String PLEASE_SELECT_A_FILE = "(please select a file)";
    private ComboBoxChangeListener mComboBoxChangeListener = new ComboBoxChangeListener();
    private PeriodChangeListener mPeriodChangeListener = new PeriodChangeListener();
    private ComboBox<RecurringAction.Interval> mIntervalComboBox;
    private TextField mPeriodTextField;
    private IntegerFormatter mPeriodFormatter;
    private Label mSecondsLabel;
    private Button mFileChooser;
    private Button mTestButton;
    private TextField mFilePath;

    public ClipEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;

        Label fileLabel = new Label("Audio File");
        GridPane.setHalignment(fileLabel, HPos.RIGHT);
        GridPane.setConstraints(fileLabel, 0, row);
        gridPane.getChildren().add(fileLabel);

        GridPane.setConstraints(getFilePath(), 1, row, 3, 1);
        GridPane.setHgrow(getFilePath(), Priority.ALWAYS);
        gridPane.getChildren().add(getFilePath());

        GridPane.setConstraints(getFileChooser(), 4, row);
        gridPane.getChildren().add(getFileChooser());

        Label intervalLabel = new Label(("Play Audio Clip"));
        GridPane.setHalignment(intervalLabel, HPos.RIGHT);
        GridPane.setConstraints(intervalLabel, 0, ++row);
        gridPane.getChildren().add(intervalLabel);

        GridPane.setConstraints(getIntervalComboBox(), 1, row);
        gridPane.getChildren().add(getIntervalComboBox());

        GridPane.setConstraints(getPeriodTextField(), 2, row);
        gridPane.getChildren().add(getPeriodTextField());

        GridPane.setHalignment(getSecondsLabel(), HPos.LEFT);
        GridPane.setConstraints(getSecondsLabel(), 3, row);
        GridPane.setHgrow(getSecondsLabel(), Priority.ALWAYS);
        gridPane.getChildren().add(getSecondsLabel());

        GridPane.setConstraints(getTestButton(), 4, row);
        gridPane.getChildren().add(getTestButton());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(ClipAction clipAction)
    {
        super.setItem(clipAction);

        mComboBoxChangeListener.disable();
        mPeriodChangeListener.disable();

        getIntervalComboBox().setDisable(clipAction == null);
        getFileChooser().setDisable(clipAction == null);

        if(clipAction != null)
        {
            RecurringAction.Interval interval = clipAction.getInterval();
            getIntervalComboBox().getSelectionModel().select(interval);
            getPeriodFormatter().setValue(clipAction.getPeriod());

            if(clipAction.getPath() != null)
            {
                getFilePath().setText(clipAction.getPath());
            }
            else
            {
                getFilePath().setText(PLEASE_SELECT_A_FILE);
            }
        }
        else
        {
            getIntervalComboBox().getSelectionModel().select(null);
            getPeriodFormatter().setValue(null);
            getFilePath().setText(PLEASE_SELECT_A_FILE);
        }

        updatePeriodAndLabel();

        mComboBoxChangeListener.enable();
        mPeriodChangeListener.enable();
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    /**
     * Updates the period text control and the accompanying seconds label.
     */
    private void updatePeriodAndLabel()
    {
        RecurringAction.Interval selected = getIntervalComboBox().getSelectionModel().getSelectedItem();

        if(selected != null)
        {
            switch(selected)
            {
                case ONCE:
                    getPeriodTextField().setVisible(false);
                    getSecondsLabel().setVisible(false);
                    break;
                case DELAYED_RESET:
                    getPeriodTextField().setVisible(true);
                    getSecondsLabel().setVisible(true);
                    getSecondsLabel().setText(DELAY_LABEL);
                    break;
                case UNTIL_DISMISSED:
                    getPeriodTextField().setVisible(true);
                    getSecondsLabel().setVisible(true);
                    getSecondsLabel().setText(UNTIL_DISMISSED_LABEL);
                    break;
            }
        }
        else
        {
            getPeriodTextField().setVisible(false);
            getSecondsLabel().setVisible(false);
        }
    }

    /**
     * Tests the currently loaded clip action
     */
    private void test()
    {
        if(getItem() != null)
        {
            final AliasAction clipAction = AliasFactory.copyOf(getItem());
            final TestMessage testMessage = new TestMessage();
            final Alias testAlias = new Alias("Test Alias");

            ThreadPool.CACHED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        clipAction.execute(testAlias, testMessage);
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error testing clip action", e);
                    }
                }
            });
        }
    }

    private Button getTestButton()
    {
        if(mTestButton == null)
        {
            mTestButton = new Button("Test");
            mTestButton.setMaxWidth(Double.MAX_VALUE);
            mTestButton.setDisable(true);
            mTestButton.setOnAction(event -> test());
        }

        return mTestButton;
    }

    private Button getFileChooser()
    {
        if(mFileChooser == null)
        {
            mFileChooser = new Button(("Select ..."));
            mFileChooser.setMaxWidth(Double.MAX_VALUE);
            mFileChooser.setDisable(true);
            mFileChooser.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Audio File");

                String selected = mFilePath.getText();

                if(selected != null && !selected.isEmpty() && !selected.contentEquals(PLEASE_SELECT_A_FILE))
                {
                    try
                    {
                        File testFile = new File(selected);
                        if(testFile.exists())
                        {
                            fileChooser.setInitialDirectory(testFile.getParentFile());
                            fileChooser.setInitialFileName(testFile.getName());
                        }
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error trying to set default directory for file chooser", e);
                    }
                }

                File audioFile = fileChooser.showOpenDialog(null);

                if(audioFile != null)
                {
                    getFilePath().setText(audioFile.getAbsolutePath());
                }
            });
        }

        return mFileChooser;
    }

    private TextField getFilePath()
    {
        if(mFilePath == null)
        {
            mFilePath = new TextField(PLEASE_SELECT_A_FILE);
            mFilePath.setDisable(true);
            mFilePath.setMaxWidth(Double.MAX_VALUE);
            mFilePath.textProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null && !newValue.isEmpty() && !newValue.contentEquals(PLEASE_SELECT_A_FILE))
                {
                    getItem().setPath(newValue);
                    getTestButton().setDisable(false);
                }
                else
                {
                    getItem().setPath(null);
                    getTestButton().setDisable(true);
                }

                modifiedProperty().set(true);
            });
        }

        return mFilePath;
    }

    private Label getSecondsLabel()
    {
        if(mSecondsLabel == null)
        {
            mSecondsLabel = new Label("second delay (1 - 60)");
            mSecondsLabel.setMaxWidth(Double.MAX_VALUE);
            mSecondsLabel.setVisible(false);
            mSecondsLabel.setAlignment(Pos.CENTER_LEFT);
        }

        return mSecondsLabel;
    }

    private ComboBox<RecurringAction.Interval> getIntervalComboBox()
    {
        if(mIntervalComboBox == null)
        {
            mIntervalComboBox = new ComboBox<>();
            mIntervalComboBox.setDisable(true);
            mIntervalComboBox.getItems().setAll(RecurringAction.Interval.values());
            mIntervalComboBox.getSelectionModel().selectedItemProperty().addListener(mComboBoxChangeListener);
        }

        return mIntervalComboBox;
    }

    private TextField getPeriodTextField()
    {
        if(mPeriodTextField == null)
        {
            mPeriodTextField = new TextField();
            mPeriodTextField.setPrefWidth(40);
            mPeriodTextField.setVisible(false);
            mPeriodTextField.setTextFormatter(getPeriodFormatter());
        }

        return mPeriodTextField;
    }

    private IntegerFormatter getPeriodFormatter()
    {
        if(mPeriodFormatter == null)
        {
            mPeriodFormatter = new IntegerFormatter(1,60);
            mPeriodFormatter.valueProperty().addListener(mPeriodChangeListener);
        }

        return mPeriodFormatter;
    }

    private class PeriodChangeListener implements ChangeListener<Integer>
    {
        private boolean mEnabled = false;

        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(mEnabled)
            {
                if(newValue != null)
                {
                    getItem().setPeriod(newValue);
                }
                else
                {
                    getItem().setPeriod(1);
                }

                modifiedProperty().set(true);
            }
        }

        public void enable()
        {
            mEnabled = true;
        }

        public void disable()
        {
            mEnabled = false;
        }
    }

    private class ComboBoxChangeListener implements ChangeListener<RecurringAction.Interval>
    {
        private boolean mEnabled = false;

        @Override
        public void changed(ObservableValue<? extends RecurringAction.Interval> observable, RecurringAction.Interval oldValue, RecurringAction.Interval newValue)
        {
            if(mEnabled)
            {
                RecurringAction.Interval selected = getIntervalComboBox().getSelectionModel().getSelectedItem();

                if(selected != null && getItem() != null)
                {
                    getItem().setInterval(selected);
                    modifiedProperty().set(true);
                }

                updatePeriodAndLabel();
            }
        }

        public void enable()
        {
            mEnabled = true;
        }

        public void disable()
        {
            mEnabled = false;
        }
    }
}
