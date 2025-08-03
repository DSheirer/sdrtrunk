/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.squelch;

import io.github.dsheirer.dsp.squelch.INoiseSquelchController;
import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.dsp.squelch.NoiseSquelchState;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX panel for graphical view of noise squelch operating state and control.
 *
 * The NoiseSquelch control can be configured with noise and hysteresis open and close thresholds.  The two value ranges
 * for noise and hysteresis run in opposite directions relative to squelch open and close.  This view inverts the noise
 * value range from (0.5 to 0.0) to (0.0 to 10.0) by inverting the noise and noise threshold values and scaling those
 * values onto the range of 0-10 to present the user with a unified view of noise and hysteresis where both values can
 * be plotted onto an x/y chart.
 *
 * The noise threshold sliders have a value range of 0.0 to 0.15.  The open noise threshold slider inverts these values
 * to a range of 0.15 to 0.0 and adds a constant of 0.10, boosting the range to 0.25 to 0.10.  The close noise threshold
 * slider also operates on a range of 0.0 to 0.15 and inverts the value to a range of 0.15 to 0.00 which is added to
 * the open threshold for a noise squelch control range of (open + 0.15) to (open + 0.00).
 *
 * The hysteresis threshold sliders each use an operating range of 0-5.  The open slider adds a minimum hysteresis of
 * 1 for an operating range of 1-6.  The close slider uses a range of 0-5 that is combined with the open slider for a
 * range of (open + 0) to (open + 5).
 */
public class NoiseSquelchView extends VBox implements Listener<NoiseSquelchState>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NoiseSquelchView.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    /**
     * Scales the noise value (0.0 to 0.5) to align with the hysteresis value range (0.0 to 10.0).  Value is determined
     * by hysteresis-max (10.0) divided by noise-max (0.5).
     */
    private static final float NOISE_DISPLAY_SCALOR = 20.0f;

    /**
     * Noise inversion value for converting displayed values in range 0-10 to usable noise values in the range 10 - 0.
     */
    private static final float NOISE_INVERSION_BASE = 10.0f;

    /**
     * Noise inversion base to convert from the slider control range (0.0 to 0.15) to inverted range (0.15 to 0.00).
     */
    private static final float NOISE_CONTROL_INVERSION_BASE = 0.15f;

    /**
     * Minimum noise threshold constant.
     */
    private static final float NOISE_CONTROL_OPEN_OFFSET = 0.10f;

    /**
     * Noise squelch history buffer size for x-axis of the XY chart, in units of 10 milliseconds.
     */
    private final int HISTORY_BUFFER_SIZE = 200; //200 x 10ms = 2,000ms / 2-second history view

    private final String NOT_AVAILABLE = "not available";
    private final PlaylistManager mPlaylistManager;
    private final List<NoiseSquelchState> mSquelchStateHistory = new ArrayList<>();
    private INoiseSquelchController mController;
    private ScheduledFuture<?> mTimerFuture;

    private ToggleButton mSquelchOverrideButton;
    private Slider mOpenNoiseSlider;
    private Slider mCloseNoiseSlider;
    private Slider mOpenHysteresisSlider;
    private Slider mCloseHysteresisSlider;
    private int mOpenHysteresisShadow = 0;
    private int mCloseHysteresisShadow = 0;

    private LineChart<Number,Number> mActivityChart;
    private final XYChart.Series<Number,Number> mNoiseSeries = new XYChart.Series<>();
    private final XYChart.Series<Number,Number> mNoiseOpenThresholdSeries = new XYChart.Series<>();
    private final XYChart.Series<Number,Number> mNoiseCloseThresholdSeries = new XYChart.Series<>();
    private final XYChart.Series<Number,Number> mHysteresisSeries = new XYChart.Series<>();
    private final XYChart.Series<Number,Number> mHysteresisOpenThresholdSeries = new XYChart.Series<>();
    private final XYChart.Series<Number,Number> mHysteresisCloseThresholdSeries = new XYChart.Series<>();
    private Label mSquelchStateLabel;
    private Label mNoiseValueLabel;
    private Label mHysteresisValueLabel;
    private Button mResetButton;

    private boolean mShowing = false;
    private boolean mControlsUpdated = true;

    /**
     * Constructs an instance
     * @param playlistManager - to trigger channel configuration changes when the user adjusts the noise squelch values.
     */
    public NoiseSquelchView(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        init();
    }

    /**
     * Setup the user interface components.
     */
    private void init()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(3);
        gridPane.setPadding(new Insets(3));
        gridPane.setMaxWidth(Double.MAX_VALUE);

        //Header - Row 0
        int row = 0;
        Label squelchHeaderLabel = new Label("Audio Squelch");
        GridPane.setHalignment(squelchHeaderLabel, HPos.CENTER);
        gridPane.add(squelchHeaderLabel, 0, row);

        Separator verticalSeparate1 = new Separator(Orientation.VERTICAL);
        verticalSeparate1.setPadding(new Insets(0, 2, 0, 2));
        gridPane.add(verticalSeparate1, 1, row, 1, 3);

        Label noiseHeaderLabel = new Label("Noise:");
        GridPane.setHalignment(noiseHeaderLabel, HPos.RIGHT);
        gridPane.add(noiseHeaderLabel, 2, row);

        GridPane.setHgrow(getNoiseValueLabel(), Priority.NEVER);
        GridPane.setHalignment(getNoiseValueLabel(), HPos.LEFT);
        gridPane.add(getNoiseValueLabel(), 3, row);

        Label hysteresisHeaderLabel = new Label("Hysteresis:");
        GridPane.setHalignment(hysteresisHeaderLabel, HPos.RIGHT);
        gridPane.add(hysteresisHeaderLabel, 4, row);

        GridPane.setHgrow(getHysteresisValueLabel(), Priority.NEVER);
        GridPane.setHalignment(getHysteresisValueLabel(), HPos.LEFT);
        gridPane.add(getHysteresisValueLabel(), 5, row);

        //Values - Row 1
        row++;
        GridPane.setHalignment(getSquelchStateLabel(), HPos.CENTER);
        gridPane.add(getSquelchStateLabel(), 0, row);

        Label noiseCloseRowLabel = new Label("Close:");
        GridPane.setHalignment(noiseCloseRowLabel, HPos.RIGHT);
        gridPane.add(noiseCloseRowLabel, 2, row);

        GridPane.setHgrow(getCloseNoiseSlider(), Priority.ALWAYS);
        gridPane.add(getCloseNoiseSlider(), 3, row);

        Label hysteresisOpenLabel = new Label("Open:");
        GridPane.setHalignment(hysteresisOpenLabel, HPos.RIGHT);
        gridPane.add(hysteresisOpenLabel, 4, row);

        GridPane.setHgrow(getOpenHysteresisSlider(), Priority.ALWAYS);
        gridPane.add(getOpenHysteresisSlider(), 5, row);

        //Values - Row 2
        row++;

        HBox buttonsBox = new HBox();
        buttonsBox.setMaxWidth(Double.MAX_VALUE);
        buttonsBox.setSpacing(5);
        HBox.setHgrow(getSquelchOverrideButton(), Priority.ALWAYS);
        HBox.setHgrow(getResetButton(), Priority.ALWAYS);
        buttonsBox.getChildren().addAll(getSquelchOverrideButton(), getResetButton());

        GridPane.setHalignment(buttonsBox,  HPos.LEFT);
        GridPane.setHgrow(buttonsBox, Priority.SOMETIMES);
        gridPane.add(buttonsBox, 0, row);

        Label noiseOpenRowLabel = new Label("Open:");
        GridPane.setHalignment(noiseOpenRowLabel, HPos.RIGHT);
        gridPane.add(noiseOpenRowLabel, 2, row);

        GridPane.setHgrow(getOpenNoiseSlider(), Priority.ALWAYS);
        gridPane.add(getOpenNoiseSlider(), 3, row);

        Label hysterisCloseRowLabel = new Label("Close:");
        GridPane.setHalignment(hysterisCloseRowLabel, HPos.RIGHT);
        gridPane.add(hysterisCloseRowLabel, 4, row);

        GridPane.setHgrow(getCloseHysteresisSlider(), Priority.ALWAYS);
        gridPane.add(getCloseHysteresisSlider(), 5, row);

        VBox.setVgrow(gridPane, Priority.NEVER);
        VBox.setVgrow(getActivityChart(), Priority.ALWAYS);
        getChildren().addAll(gridPane, getActivityChart());
    }

    /**
     * Sets this view as showing and starts the chart update timer, or sets this view as hidden and stops the update timer
     * @param showing to indicate if this view is selected by the user and showing.
     */
    public void setShowing(boolean showing)
    {
        mShowing = showing;
        updateTimer();
    }

    /**
     * Cancels the chart update timer.
     */
    private synchronized void cancelTimer()
    {
        if(mTimerFuture != null)
        {
            mTimerFuture.cancel(true);
            mTimerFuture = null;
        }
    }

    /**
     * Updates the timer to process incoming decoder states and update the XY chart values.
     */
    private synchronized void updateTimer()
    {
        if(mShowing && mController != null && mTimerFuture == null)
        {
            //Start the timer
            mTimerFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::updateChart, 0, 50, TimeUnit.MILLISECONDS);
        }
        else if((!mShowing || mController == null) && mTimerFuture != null)
        {
            cancelTimer();
        }
    }

    /**
     * Primary method for receiving noise squelch state updates from the decoder.  Manages the squelch state history
     * buffer size.  This method is invoked by the channel buffer processing thread and cooperates with the chart
     * update timer thread by synchronizing/blocking on the squelch state history.
     * @param noiseSquelchState to add to the history.
     */
    @Override
    public void receive(NoiseSquelchState noiseSquelchState)
    {
        synchronized(mSquelchStateHistory)
        {
            mSquelchStateHistory.add(noiseSquelchState);

            while(mSquelchStateHistory.size() > HISTORY_BUFFER_SIZE)
            {
                mSquelchStateHistory.removeFirst();
            }
        }

        /**
         * If this is the first squelch state, update the view controls with these initial values.
         */
        if(!mControlsUpdated)
        {
            updateViewControls(noiseSquelchState);
        }
    }

    /**
     * Initializes the view controls to the latest received noise squelch state which should always be accurate with the
     * current state of the noise squelch.
     * @param noiseSquelchState for initializing the controls.
     */
    private void updateViewControls(NoiseSquelchState noiseSquelchState)
    {
        Platform.runLater(() -> {

            getOpenNoiseSlider().setDisable(false);
            getCloseNoiseSlider().setDisable(false);
            getNoiseValueLabel().setDisable(false);
            setNoiseSliderValues(noiseSquelchState.noiseOpenThreshold(), noiseSquelchState.noiseCloseThreshold());

            getOpenHysteresisSlider().setDisable(false);
            getCloseHysteresisSlider().setDisable(false);
            getHysteresisValueLabel().setDisable(false);
            getOpenHysteresisSlider().setValue(noiseSquelchState.hysteresisOpenThreshold());

            getCloseHysteresisSlider().setValue(noiseSquelchState.hysteresisCloseThreshold() - noiseSquelchState.hysteresisOpenThreshold());

            getSquelchStateLabel().setDisable(false);
            getActivityChart().setDisable(false);

            getSquelchOverrideButton().selectedProperty().setValue(noiseSquelchState.squelchOverride());
            getSquelchOverrideButton().setDisable(false);
            getResetButton().setDisable(false);

            mControlsUpdated = true;
        });
    }

    /**
     * Resets/clears chart and controls.
     */
    private void reset()
    {
        //Clear the squelch state history
        synchronized(mSquelchStateHistory)
        {
            mSquelchStateHistory.clear();
        }

        //Clear the chart axis
        getActivityChart().setDisable(true);
        for(int x = 0; x < HISTORY_BUFFER_SIZE; x++)
        {
            mNoiseSeries.getData().get(x).setYValue(0);
            mNoiseOpenThresholdSeries.getData().get(x).setYValue(0);
            mNoiseCloseThresholdSeries.getData().get(x).setYValue(0);
            mHysteresisSeries.getData().get(x).setYValue(0);
            mHysteresisOpenThresholdSeries.getData().get(x).setYValue(0);
            mHysteresisCloseThresholdSeries.getData().get(x).setYValue(0);
        }

        //Hysteresis controls
        getOpenHysteresisSlider().setDisable(true);
        getOpenHysteresisSlider().setValue(0);
        getCloseHysteresisSlider().setDisable(true);
        getCloseHysteresisSlider().setValue(0);
        getHysteresisValueLabel().setDisable(true);
        getHysteresisValueLabel().setText(NOT_AVAILABLE);

        //Noise controls
        getOpenNoiseSlider().setDisable(true);
        getOpenNoiseSlider().setValue(0);
        getCloseNoiseSlider().setValue(0);
        getCloseNoiseSlider().setDisable(true);
        getNoiseValueLabel().setDisable(true);
        getNoiseValueLabel().setText(NOT_AVAILABLE);

        getResetButton().setDisable(true);
        getSquelchOverrideButton().setDisable(true);
        getSquelchStateLabel().setDisable(true);
        getSquelchStateLabel().setText(NOT_AVAILABLE);

        mControlsUpdated = false;
    }

    /**
     * Updates the chart and label values from the noise squelch state history buffer.  This method is fired by the
     * scheduled timer process and cooperates squelch state history buffer thread access by synchronizing/blocking on the
     * squelch state history buffer.
     */
    private void updateChart()
    {
        final int[] hysteresis = new int[HISTORY_BUFFER_SIZE];
        final int[] hysteresisOpenThreshold = new int[HISTORY_BUFFER_SIZE];
        final int[] hysteresisCloseThreshold = new int[HISTORY_BUFFER_SIZE];
        final float[] noise = new float[HISTORY_BUFFER_SIZE];
        final float[] noiseOpenThreshold = new float[HISTORY_BUFFER_SIZE];
        final float[] noiseCloseThreshold = new float[HISTORY_BUFFER_SIZE];
        float noiseCurrent = 0f;
        int hysteresisCurrent = 0;

        boolean squelch = true;
        boolean squelchOverride = false;

        synchronized(mSquelchStateHistory)
        {
            if(!mSquelchStateHistory.isEmpty())
            {
                NoiseSquelchState latest = mSquelchStateHistory.getLast();
                squelch = latest.squelch();
                squelchOverride = latest.squelchOverride();
            }

            if(mSquelchStateHistory.size() == HISTORY_BUFFER_SIZE)
            {
                for(int x = 0; x < HISTORY_BUFFER_SIZE; x++)
                {
                    if(mSquelchStateHistory.size() > x)
                    {
                        NoiseSquelchState state = mSquelchStateHistory.get(x);

                        noise[x] = toDisplayNoise(state.noise());
                        noiseOpenThreshold[x] = toDisplayNoise(state.noiseOpenThreshold());
                        noiseCloseThreshold[x] = toDisplayNoise(state.noiseCloseThreshold());

                        hysteresis[x] = state.hysteresis();
                        hysteresisOpenThreshold[x] = state.hysteresisOpenThreshold();
                        hysteresisCloseThreshold[x] = state.hysteresisCloseThreshold();

                        if(x == HISTORY_BUFFER_SIZE - 1)
                        {
                            noiseCurrent = toDisplayNoise(state.noise());
                            hysteresisCurrent = state.hysteresis();
                        }
                    }
                }
            }
            else
            {
                //On startup, make the values stream in from the right instead of the left until the buffer is full.
                int offset = 0;

                for(int x = 0; x < mSquelchStateHistory.size(); x++)
                {
                    offset = x + (HISTORY_BUFFER_SIZE - mSquelchStateHistory.size());

                    NoiseSquelchState state = mSquelchStateHistory.get(x);

                    noise[offset] = toDisplayNoise(state.noise());
                    noiseOpenThreshold[offset] = toDisplayNoise(state.noiseOpenThreshold());
                    noiseCloseThreshold[offset] = toDisplayNoise(state.noiseCloseThreshold());

                    hysteresis[offset] = state.hysteresis();
                    hysteresisOpenThreshold[offset] = state.hysteresisOpenThreshold();
                    hysteresisCloseThreshold[offset] = state.hysteresisCloseThreshold();

                    if(x == mSquelchStateHistory.size() - 1)
                    {
                        noiseCurrent = toDisplayNoise(state.noise());
                        hysteresisCurrent = state.hysteresis();
                    }
                }
            }
        }

        final boolean finalSquelchOverride = squelchOverride;
        final boolean finalSquelch = squelch;
        final float noiseCurrentFinal = noiseCurrent;
        final int hysteresisCurrentFinal = hysteresisCurrent;

        //Update the chart and label displays on the JavaFX thread.
        Platform.runLater(() -> {
            try
            {
                for(int x = 0; x < HISTORY_BUFFER_SIZE; x++)
                {
                    mNoiseSeries.getData().get(x).setYValue(noise[x]);
                    mNoiseOpenThresholdSeries.getData().get(x).setYValue(noiseOpenThreshold[x]);
                    mNoiseCloseThresholdSeries.getData().get(x).setYValue(noiseCloseThreshold[x]);
                    mHysteresisSeries.getData().get(x).setYValue(hysteresis[x]);
                    mHysteresisOpenThresholdSeries.getData().get(x).setYValue(hysteresisOpenThreshold[x]);
                    mHysteresisCloseThresholdSeries.getData().get(x).setYValue(hysteresisCloseThreshold[x]);
                }

                getSquelchStateLabel().setText(finalSquelchOverride ? "Override" : finalSquelch ? "Closed" : "Open");
                updateLabelNoise(noiseCurrentFinal);
                updateLabelHysteresis(hysteresisCurrentFinal);
            }
            catch(Exception e)
            {
                LOGGER.error("Error updating audio squelch noise values in squelch view", e);
            }
        });
    }

    /**
     * Sets the noise squelch controller for this view.  Unregisters the previous controller, clears the display and
     * registers the new controller on the JavaFX UI thread if it is non-null.
     *
     * Note: this method is invoked by the Swing UI thread in response to user action.
     *
     * @param controller to set (non-null) or clear (null).
     */
    public void setController(INoiseSquelchController controller)
    {
        try
        {
            cancelTimer();

            //Unregister from previous controller.
            if(mController != null)
            {
                mController.setNoiseSquelchStateListener(null);
            }

            //Nullify the controller so the reset doesn't trigger any save actions.
            mController = null;

            //Since this is invoked on the Swing UI thread, transfer control to the JavaFX UI thread since we're
            //accessing the JavaFX controls.
            Platform.runLater(() -> {
                reset();

                mController = controller;

                if(mController != null)
                {
                    mController.setNoiseSquelchStateListener(NoiseSquelchView.this);
                }

                updateTimer();
            });
        }
        catch(Exception e)
        {
            LOGGER.error("Error updating noise squelch controller", e);
        }
    }

    /**
     * Squelch default values reset button.
     */
    private Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button();
            mResetButton.setDisable(true);
            mResetButton.setMaxWidth(Double.MAX_VALUE);
            mResetButton.setTooltip(new Tooltip("Reset noise and hysteresis to default values"));
            IconNode iconNode = new IconNode(FontAwesome.UNDO);
            iconNode.setIconSize(10);
            iconNode.setFill(Color.BLACK);
            mResetButton.setGraphic(iconNode);
            mResetButton.setOnAction(event -> {
                setNoiseSliderValues(NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD, NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD);
                getSquelchOverrideButton().setSelected(false);
                getOpenHysteresisSlider().setValue(NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD);
                //Note: close threshold slider values are added to the open slider, so we subtract open value from the absolute value
                getCloseHysteresisSlider().setValue(NoiseSquelch.DEFAULT_HYSTERESIS_CLOSE_THRESHOLD - NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD);
            });
        }

        return mResetButton;
    }

    /**
     * Squelch override button
     */
    private ToggleButton getSquelchOverrideButton()
    {
        if(mSquelchOverrideButton == null)
        {
            mSquelchOverrideButton = new ToggleButton("Override");
            mSquelchOverrideButton.setTooltip(new Tooltip("Manually override squelch control for always-on audio output"));
            mSquelchOverrideButton.setDisable(true);
            mSquelchOverrideButton.setOnAction(event -> {
                if(mController != null)
                {
                    mController.setSquelchOverride(getSquelchOverrideButton().isSelected());
                }
            });
        }

        return mSquelchOverrideButton;
    }

    /**
     * Open noise threshold slider.  Values range from 0.0 to 0.15.  Note: slider values are inverted and converted
     * to the actual values used by the noise squelch control.
     * @return slider control
     */
    private Slider getOpenNoiseSlider()
    {
        if(mOpenNoiseSlider == null)
        {
            mOpenNoiseSlider = new Slider(0.0, NOISE_CONTROL_INVERSION_BASE, 0.0);
            mOpenNoiseSlider.setDisable(true);
            mOpenNoiseSlider.setTooltip(new Tooltip("Adjust the noise threshold for squelch opening"));
            mOpenNoiseSlider.setMajorTickUnit(0.05);
            mOpenNoiseSlider.setMinorTickCount(4);
            mOpenNoiseSlider.setShowTickMarks(true);
            mOpenNoiseSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateControllerNoise());
        }

        return mOpenNoiseSlider;
    }

    /**
     * Updates the noise threshold on the controller from open/close slider change events.
     */
    private void updateControllerNoise()
    {
        if(mController != null && mShowing)
        {
            float open = getOpenNoiseThresholdFromSliderValue();
            float close = getCloseNoiseThresholdFromSliderValue();
            open = Math.min(open, NoiseSquelch.MAXIMUM_NOISE_THRESHOLD);
            open = Math.max(open, NoiseSquelch.MINIMUM_NOISE_THRESHOLD);
            close = Math.max(open, close);
            close = Math.min(close, NoiseSquelch.MAXIMUM_NOISE_THRESHOLD);
            close = Math.max(close, NoiseSquelch.MINIMUM_NOISE_THRESHOLD);
            mController.setNoiseThreshold(open, close);

            //The controller updates the channel configuration so schedule a playlist save
            if(mPlaylistManager != null)
            {
                mPlaylistManager.schedulePlaylistSave();
            }
        }
    }

    /**
     * Close noise threshold slider.  Values range from 0.0 to 0.15.  Note: slider values are inverted and converted
     * to the actual values used by the noise squelch control.
     * @return slider control
     */
    private Slider getCloseNoiseSlider()
    {
        if(mCloseNoiseSlider == null)
        {
            mCloseNoiseSlider = new Slider(0.00, NOISE_CONTROL_INVERSION_BASE, 0.0);
            mCloseNoiseSlider.setDisable(true);
            mCloseNoiseSlider.setTooltip(new Tooltip("Adjust the noise threshold for squelch closing"));
            mCloseNoiseSlider.setMajorTickUnit(0.05);
            mCloseNoiseSlider.setMinorTickCount(4);
            mCloseNoiseSlider.setShowTickMarks(true);
            mCloseNoiseSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateControllerNoise());
        }

        return mCloseNoiseSlider;
    }

    /**
     * Open hysteresis slider control
     */
    private Slider getOpenHysteresisSlider()
    {
        if(mOpenHysteresisSlider == null)
        {
            mOpenHysteresisSlider = new Slider(1, 5, 0);
            mOpenHysteresisSlider.setDisable(true);
            mOpenHysteresisSlider.setTooltip(new Tooltip("Adjust the hysteresis threshold for squelch opening"));
            mOpenHysteresisSlider.setBlockIncrement(1);
            mOpenHysteresisSlider.setMajorTickUnit(1.0);
            mOpenHysteresisSlider.setMinorTickCount(0);
            mOpenHysteresisSlider.setShowTickMarks(true);
            mOpenHysteresisSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateControllerHysteresis());
        }

        return mOpenHysteresisSlider;
    }

    /**
     * Applies open and close hysteresis slider values to the noise squelch control when the user adjusts either value.
     */
    private void updateControllerHysteresis()
    {
        if(mController != null && mShowing)
        {
            int open = (int)getOpenHysteresisSlider().getValue();
            int close = (int)getCloseHysteresisSlider().getValue() + open;

            if(open != mOpenHysteresisShadow || close != mCloseHysteresisShadow)
            {
                //Shadow copy to limit fire events to only true changes.
                mOpenHysteresisShadow = open;
                mCloseHysteresisShadow = close;
                mController.setHysteresisThreshold(open, close);

                //The controller updates the channel configuration so schedule a playlist save
                if(mPlaylistManager != null)
                {
                    mPlaylistManager.schedulePlaylistSave();
                }
            }
        }
    }

    /**
     * Close hysteresis value slider control
     */
    private Slider getCloseHysteresisSlider()
    {
        if(mCloseHysteresisSlider == null)
        {
            mCloseHysteresisSlider = new Slider(0, 5, 0);
            mCloseHysteresisSlider.setDisable(true);
            mCloseHysteresisSlider.setTooltip(new Tooltip("Adjust the hysteresis threshold for squelch closing"));
            mCloseHysteresisSlider.setBlockIncrement(1);
            mCloseHysteresisSlider.setMajorTickUnit(1.0);
            mCloseHysteresisSlider.setMinorTickCount(0);
            mCloseHysteresisSlider.setShowTickMarks(true);
            mCloseHysteresisSlider.setDisable(true);
            mCloseHysteresisSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateControllerHysteresis());
        }

        return mCloseHysteresisSlider;
    }

    /**
     * Label to display the squelch state.
     */
    private Label getSquelchStateLabel()
    {
        if(mSquelchStateLabel == null)
        {
            mSquelchStateLabel = new Label(NOT_AVAILABLE);
            mSquelchStateLabel.setDisable(true);
            mSquelchStateLabel.setStyle("-fx-font-weight: bold;");
            mSquelchStateLabel.setDisable(true);
        }

        return mSquelchStateLabel;
    }

    /**
     * Label to display the open, close and current noise values.
     */
    private Label getNoiseValueLabel()
    {
        if(mNoiseValueLabel == null)
        {
            mNoiseValueLabel = new Label(NOT_AVAILABLE);
            mNoiseValueLabel.setDisable(true);
            mNoiseValueLabel.setPadding(new Insets(0, 0, 0, 3));
            getOpenNoiseSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateLabelNoise(0.0f));
            getCloseNoiseSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateLabelNoise(0.0f));
        }

        return mNoiseValueLabel;
    }

    /**
     * Label to display the open, close and current hysteresis values.
     */
    private Label getHysteresisValueLabel()
    {
        if(mHysteresisValueLabel == null)
        {
            mHysteresisValueLabel = new Label(NOT_AVAILABLE);
            mHysteresisValueLabel.setDisable(true);
            mHysteresisValueLabel.setPadding(new Insets(0, 0, 0, 3));
            getOpenHysteresisSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateLabelHysteresis(0));
            getCloseHysteresisSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateLabelHysteresis(0));
        }

        return mHysteresisValueLabel;
    }

    /**
     * Updates the hysteresis value label content.
     * @param current hysteresis value.
     */
    private void updateLabelHysteresis(int current)
    {
        int open = (int)getOpenHysteresisSlider().getValue();
        int close = (int)getCloseHysteresisSlider().getValue() + open;
        getHysteresisValueLabel().setText(open + " - " + close + " Current: " + current + " (x" + NoiseSquelch.VARIANCE_CALCULATION_WINDOW_MILLISECONDS + "ms)");
    }

    /**
     * Updates the noise value label content.
     * @param current noise value converted to display format.
     */
    private void updateLabelNoise(float current)
    {
        float open = getOpenNoiseThresholdFromSliderValue();
        float close = getCloseNoiseThresholdFromSliderValue();
        getNoiseValueLabel().setText(DECIMAL_FORMAT.format(toDisplayNoise(close)) + " - " +
                DECIMAL_FORMAT.format(toDisplayNoise(open)) + " Current: " +
                DECIMAL_FORMAT.format(Math.abs(current)));
    }

    /**
     * Converts the current open threshold slider value into the value used by the noise squelch control.
     * @return current open threshold setting.
     */
    private float getOpenNoiseThresholdFromSliderValue()
    {
        return (float)(NOISE_CONTROL_OPEN_OFFSET + (NOISE_CONTROL_INVERSION_BASE - getOpenNoiseSlider().getValue()));
    }

    /**
     * Updates the open and close noise threshold slider controls from the noise squelch threshold values.
     * @param open noise squelch threshold
     * @param close noise squelch threshold
     */
    private void setNoiseSliderValues(float open, float close)
    {
        float openSlider = NOISE_CONTROL_INVERSION_BASE - (open - NOISE_CONTROL_OPEN_OFFSET);
        float closeSlider = NOISE_CONTROL_INVERSION_BASE - (close - open);

        openSlider = Math.min(openSlider, NOISE_CONTROL_INVERSION_BASE);
        openSlider = Math.max(openSlider, 0f);
        closeSlider = Math.min(closeSlider, NOISE_CONTROL_INVERSION_BASE);
        closeSlider = Math.max(closeSlider, 0f);

        getOpenNoiseSlider().setValue(openSlider);
        getCloseNoiseSlider().setValue(closeSlider);
    }

    /**
     * Converts the current close threshold slider value into the value used by the noise squelch control.
     * @return current close threshold setting.
     */
    private float getCloseNoiseThresholdFromSliderValue()
    {
        return getOpenNoiseThresholdFromSliderValue() + (float)(NOISE_CONTROL_INVERSION_BASE - getCloseNoiseSlider().getValue());
    }

    /**
     * Converts from the noise squelch controller noise value to the display noise value.
     * @param noiseValue from the controller
     * @return value for display
     */
    private static float toDisplayNoise(float noiseValue)
    {
        return NOISE_INVERSION_BASE - (noiseValue * NOISE_DISPLAY_SCALOR);
    }

    /**
     * Line chart displaying combined noise and hysteresis history.  Uses 6x lines to display open, close and current
     * values for noise and hysteresis.
     */
    private LineChart<Number,Number> getActivityChart()
    {
        if(mActivityChart == null)
        {
            NumberAxis xAxis = new NumberAxis(0, HISTORY_BUFFER_SIZE - 1,  0);
            NumberAxis yAxis = new NumberAxis(-0.5, 10.5, 1);
            mActivityChart = new LineChart<>(xAxis, yAxis);
            mActivityChart.setLegendSide(Side.RIGHT);
            mActivityChart.setPadding(new Insets(0, 5, 0, 0));
            mActivityChart.setAnimated(false); //Turn off animation
            mActivityChart.setCreateSymbols(false); //Turn off data point markers
            mActivityChart.setMaxHeight(Double.MAX_VALUE);
            mActivityChart.setMaxWidth(Double.MAX_VALUE);
            mActivityChart.lookup(".chart-plot-background").setStyle("-fx-background-color: black;");

            for(int x = 0; x < HISTORY_BUFFER_SIZE; x++)
            {
                mNoiseSeries.getData().add(new XYChart.Data<>(x, 0));
                mNoiseOpenThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mNoiseCloseThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisOpenThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisCloseThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
            }

            mNoiseSeries.setName("Noise (N)");
            mNoiseOpenThresholdSeries.setName("N-Open");
            mNoiseCloseThresholdSeries.setName("N-Close");

            mHysteresisSeries.setName("Hysteresis");
            mHysteresisOpenThresholdSeries.setName("H-Open");
            mHysteresisCloseThresholdSeries.setName("H-Close");

            mActivityChart.getData().add(mNoiseSeries);
            mActivityChart.getData().add(mNoiseOpenThresholdSeries);
            mActivityChart.getData().add(mNoiseCloseThresholdSeries);
            mActivityChart.getData().add(mHysteresisSeries);
            mActivityChart.getData().add(mHysteresisCloseThresholdSeries);
            mActivityChart.getData().add(mHysteresisOpenThresholdSeries);
        }

        return mActivityChart;
    }
}
