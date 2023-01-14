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
import io.github.dsheirer.alias.action.beep.BeepAction;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Beep action editor
 */
public class BeepEditor extends ActionEditor<BeepAction>
{
    private static final Logger mLog = LoggerFactory.getLogger(BeepEditor.class);
    private static final String UNTIL_DISMISSED_LABEL = "second intervals (1-60)";
    private static final String DELAY_LABEL = "second delay (1-60)";

    private ComboBoxChangeListener mComboBoxChangeListener = new ComboBoxChangeListener();
    private PeriodChangeListener mPeriodChangeListener = new PeriodChangeListener();
    private ComboBox<RecurringAction.Interval> mIntervalComboBox;
    private TextField mPeriodTextField;
    private IntegerFormatter mPeriodFormatter;
    private Label mSecondsLabel;
    private Button mTestButton;

    public BeepEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;

        Label intervalLabel = new Label(("Beep"));
        GridPane.setHalignment(intervalLabel, HPos.RIGHT);
        GridPane.setConstraints(intervalLabel, 0, row);
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
    public void setItem(BeepAction beepAction)
    {
        super.setItem(beepAction);

        mComboBoxChangeListener.disable();
        mPeriodChangeListener.disable();

        getIntervalComboBox().setDisable(beepAction == null);

        if(beepAction != null)
        {
            RecurringAction.Interval interval = beepAction.getInterval();
            getIntervalComboBox().getSelectionModel().select(interval);
            getPeriodFormatter().setValue(beepAction.getPeriod());
        }
        else
        {
            getIntervalComboBox().getSelectionModel().select(null);
            getPeriodFormatter().setValue(null);
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
     * Tests the currently loaded clip action
     */
    private void test()
    {
        if(getItem() != null)
        {
            final AliasAction action = AliasFactory.copyOf(getItem());
            final TestMessage testMessage = new TestMessage();
            final Alias testAlias = new Alias("Test Alias");

            ThreadPool.CACHED.submit(() -> {
                try
                {
                    action.execute(testAlias, testMessage);
                }
                catch(Exception e)
                {
                    mLog.error("Error testing beep action", e);
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
            mTestButton.setOnAction(event -> test());
        }

        return mTestButton;
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
                    getItem().setPeriod(5);
                }
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
            mLog.debug("I Changed - new value: " + newValue + " not null:" + (getItem() != null));
            if(mEnabled)
            {
                RecurringAction.Interval selected = getIntervalComboBox().getSelectionModel().getSelectedItem();

                if(selected != null && getItem() != null)
                {
                    mLog.debug("Setting interval to " + selected);
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
