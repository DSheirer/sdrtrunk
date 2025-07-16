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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX panel for graphical view of noise squelch state
 */
public class NoiseSquelchView extends VBox implements Listener<NoiseSquelchState>
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(".00");
    //Constant to scale displayed noise values onto same range as hysteresis
    private final float NOISE_DISPLAY_SCALOR = 20.0f;
    private INoiseSquelchController mController;
    private ToggleButton mSquelchOverrideButton;
    private Slider mOpenNoiseSlider;
    private Slider mCloseNoiseSlider;
    private int mOpenHysteresis = 0;
    private int mCloseHysteresis = 0;
    private Slider mOpenHysteresisSlider;
    private Slider mCloseHysteresisSlider;
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
    private final List<NoiseSquelchState> mSquelchStates = new ArrayList<>();
    private boolean mShowing = false;
    private boolean mControlsUpdated = true;
    private ScheduledFuture<?> mTimerFuture;

    /**
     * Constructs an instance
     */
    public NoiseSquelchView()
    {
        init();
    }

    private void init()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(3);
        gridPane.setPadding(new Insets(3));
        gridPane.setMaxWidth(Double.MAX_VALUE);

        //Header - Row 0
        int row = 0;
        Label squelchHeaderLabel = new Label("Squelch");
        GridPane.setHalignment(squelchHeaderLabel, HPos.CENTER);
        gridPane.add(squelchHeaderLabel, 0, row);

        Separator verticalSeparate1 = new Separator(Orientation.VERTICAL);
        verticalSeparate1.setPadding(new Insets(0, 2, 0, 2));
        gridPane.add(verticalSeparate1, 1, row, 1, 3);

        Label thresholdLabel = new Label("Thresholds:");
        GridPane.setHalignment(thresholdLabel, HPos.RIGHT);
        gridPane.add(thresholdLabel, 2, row);

        Label openHeaderLabel = new Label("Open");
        GridPane.setHalignment(openHeaderLabel, HPos.CENTER);
        gridPane.add(openHeaderLabel, 3, row);

        Label closeHeaderLabel = new Label("Close");
        GridPane.setHalignment(closeHeaderLabel, HPos.CENTER);
        gridPane.add(closeHeaderLabel, 4, row);

        Label currentHeaderLabel = new Label("Range / Current");
        GridPane.setHalignment(currentHeaderLabel, HPos.CENTER);
        gridPane.add(currentHeaderLabel, 5, row);

        //Noise - Row 1
        row++;
        GridPane.setHalignment(getSquelchStateLabel(), HPos.CENTER);
        gridPane.add(getSquelchStateLabel(), 0, row);

        Label noiseRowLabel = new Label("Noise:");
        GridPane.setHalignment(noiseRowLabel, HPos.RIGHT);
        gridPane.add(noiseRowLabel, 2, row);

        GridPane.setHgrow(getOpenNoiseSlider(), Priority.ALWAYS);
        gridPane.add(getOpenNoiseSlider(), 3, row);

        GridPane.setHgrow(getCloseNoiseSlider(), Priority.ALWAYS);
        gridPane.add(getCloseNoiseSlider(), 4, row);

        GridPane.setHalignment(getNoiseValueLabel(), HPos.LEFT);
        gridPane.add(getNoiseValueLabel(), 5,row);

        //Hysteresis - Row 2
        row++;

        GridPane.setHalignment(getSquelchOverrideButton(),  HPos.CENTER);
        gridPane.add(getSquelchOverrideButton(), 0, row);

        Label hysteresisRowLabel = new Label("Hysteresis (x" + NoiseSquelch.VARIANCE_CALCULATION_WINDOW_MILLISECONDS + " ms):");
        GridPane.setHalignment(hysteresisRowLabel, HPos.RIGHT);
        gridPane.add(hysteresisRowLabel, 2, row);

        GridPane.setHgrow(getOpenHysteresisSlider(), Priority.ALWAYS);
        gridPane.add(getOpenHysteresisSlider(), 3, row);

        GridPane.setHgrow(getCloseHysteresisSlider(), Priority.ALWAYS);
        gridPane.add(getCloseHysteresisSlider(), 4, row);

        GridPane.setHalignment(getHysteresisValueLabel(), HPos.LEFT);
        gridPane.add(getHysteresisValueLabel(), 5,row);

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
        System.out.println("Showing [" + showing + "] updating timer ...");
        updateTimer();
    }

    private synchronized void updateTimer()
    {
        System.out.println("Inside updateTimer - showing [" + mShowing + "] controller [" + mController + "] timer [" + mTimerFuture + "]");
        if(mShowing && mController != null && mTimerFuture == null)
        {
            System.out.println("Starting the view timer");
            //Start the timer
            mTimerFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::updateChart, 0, 100, TimeUnit.MILLISECONDS);
        }
        else if((!mShowing || mController == null) && mTimerFuture != null)
        {
            System.out.println("Cancelling the view timer");
            mTimerFuture.cancel(true);
            mTimerFuture = null;
            clearChartAndControls();
        }
    }

    @Override
    public void receive(NoiseSquelchState noiseSquelchState)
    {
        synchronized(mSquelchStates)
        {
            mSquelchStates.add(noiseSquelchState);

            while(mSquelchStates.size() > 40)
            {
                mSquelchStates.removeFirst();
            }
        }

        if(!mControlsUpdated)
        {
            Platform.runLater(() -> {
                System.out.println("Updating the controls to display data - noise threshold: " + noiseSquelchState.noiseOpenThreshold());
                getOpenNoiseSlider().setValue(noiseSquelchState.noiseOpenThreshold());
                getOpenNoiseSlider().setDisable(false);
                getCloseNoiseSlider().setValue(noiseSquelchState.noiseCloseThreshold() - noiseSquelchState.noiseOpenThreshold());
                getCloseNoiseSlider().setDisable(false);

                System.out.println("Updating the controls to display data - hysteresis threshold: " + noiseSquelchState.hysteresisOpenThreshold());
                getOpenHysteresisSlider().setValue(noiseSquelchState.hysteresisOpenThreshold());
                getOpenHysteresisSlider().setDisable(false);
                getCloseHysteresisSlider().setValue(noiseSquelchState.hysteresisCloseThreshold() - noiseSquelchState.hysteresisOpenThreshold());
                getCloseHysteresisSlider().setDisable(false);

                getActivityChart().setDisable(false);

                getSquelchOverrideButton().selectedProperty().setValue(noiseSquelchState.squelchOverride());
                getSquelchOverrideButton().setDisable(false);

                getSquelchStateLabel().setDisable(false);

                mControlsUpdated = true;
                System.out.println("Updating controls finished");
            });
        }
    }

    /**
     * Clears any displayed chart data and disables the noise and hysteresis sliders and buttons.
     */
    private void clearChartAndControls()
    {
        Platform.runLater(() -> {
            System.out.println("Clearing the chart");
            getOpenHysteresisSlider().setDisable(true);
            getOpenHysteresisSlider().setValue(0);
            getOpenNoiseSlider().setDisable(true);
            getOpenNoiseSlider().setValue(0);
            getActivityChart().setDisable(true);
            mControlsUpdated = false;
        });
    }

    private void updateChart()
    {
        final int[] hysteresis = new int[40];
        final int[] hysteresisOpenThreshold = new int[40];
        final int[] hysteresisCloseThreshold = new int[40];
        final float[] noise = new float[40];
        final float[] noiseOpenThreshold = new float[40];
        final float[] noiseCloseThreshold = new float[40];
        float noiseCurrent = 0f;
        int hysteresisCurrent = 0;

        boolean squelch = true;
        boolean squelchOverride = false;

        synchronized(mSquelchStates)
        {
            if(!mSquelchStates.isEmpty())
            {
                NoiseSquelchState latest = mSquelchStates.getLast();
                squelch = latest.squelch();
                squelchOverride = latest.squelchOverride();
            }

            for(int x = 0; x < 40; x++)
            {
                if(mSquelchStates.size() > x)
                {
                    NoiseSquelchState state = mSquelchStates.get(x);
                    noise[x] = state.noise() * NOISE_DISPLAY_SCALOR;
                    noiseOpenThreshold[x] = state.noiseOpenThreshold() * NOISE_DISPLAY_SCALOR;
                    noiseCloseThreshold[x] = state.noiseCloseThreshold() * NOISE_DISPLAY_SCALOR;
                    hysteresis[x] = state.hysteresis();
                    hysteresisOpenThreshold[x] = state.hysteresisOpenThreshold();
                    hysteresisCloseThreshold[x] = state.hysteresisCloseThreshold();

                    if(x == 39)
                    {
                        noiseCurrent = state.noise();
                        hysteresisCurrent = state.hysteresis();
                    }
                }
            }
        }

        final boolean finalSquelchOverride = squelchOverride;
        final boolean finalSquelch = squelch;
        final float noiseCurrentFinal = noiseCurrent;
        final int hysteresisCurrentFinal = hysteresisCurrent;

        Platform.runLater(() -> {
            try
            {
                for(int x = 0; x < 40; x++)
                {
                    mNoiseSeries.getData().get(x).setYValue(noise[x]);
                    mNoiseOpenThresholdSeries.getData().get(x).setYValue(noiseOpenThreshold[x]);
                    mNoiseCloseThresholdSeries.getData().get(x).setYValue(noiseCloseThreshold[x]);
                    mHysteresisSeries.getData().get(x).setYValue(hysteresis[x]);
                    mHysteresisOpenThresholdSeries.getData().get(x).setYValue(hysteresisOpenThreshold[x]);
                    mHysteresisCloseThresholdSeries.getData().get(x).setYValue(hysteresisCloseThreshold[x]);
                }

                getSquelchStateLabel().setText(finalSquelchOverride ? "Override" : finalSquelch ? "Closed" : "Open");
                updateNoiseValue(noiseCurrentFinal);
                updateHysteresisValue(hysteresisCurrentFinal);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    /**
     * Sets the controller for this view.  If the controller is non-null, registers this view to receive state events
     * from the controller.  If a controller is already registered then it is unregistered before registering on the
     * new controller.
     *
     * @param controller to set (non-null) or clear (null).
     */
    public void setController(INoiseSquelchController controller)
    {
        System.out.println("Setting the controller to " + controller);

        try
        {
            if(mController != null)
            {
                mController.setNoiseSquelchStateListener(null);
            }

            mController = controller;

            if(mController != null)
            {
                mControlsUpdated = false;
                mController.setNoiseSquelchStateListener(this);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Controller [" + mController + "] updating timer ....");

        updateTimer();
    }

    private ToggleButton getSquelchOverrideButton()
    {
        if(mSquelchOverrideButton == null)
        {
            mSquelchOverrideButton = new ToggleButton("Override");
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

    private Slider getOpenNoiseSlider()
    {
        if(mOpenNoiseSlider == null)
        {
            mOpenNoiseSlider = new Slider(0.01, 2.0, 0.1);
            mOpenNoiseSlider.setMajorTickUnit(1.0);
            mOpenNoiseSlider.setMinorTickCount(3);
            mOpenNoiseSlider.setShowTickMarks(true);
            mOpenNoiseSlider.setDisable(true);
            mOpenNoiseSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateNoise());
        }

        return mOpenNoiseSlider;
    }

    /**
     * Updates the noise threshold on the controller from open/close slider change events.
     */
    private void updateNoise()
    {
        if(mController != null)
        {
            //Ensure close is always >= open by adding, and scale to 1/10th of the display value..
            float open = (float)getOpenNoiseSlider().getValue() / NOISE_DISPLAY_SCALOR;
            float close = (open + ((float)getCloseNoiseSlider().getValue()) / NOISE_DISPLAY_SCALOR);
            mController.setNoiseThreshold(open, close);
        }
    }

    private Slider getCloseNoiseSlider()
    {
        if(mCloseNoiseSlider == null)
        {
            mCloseNoiseSlider = new Slider(0.0, 1.0, 0.0);
            mCloseNoiseSlider.setMajorTickUnit(1.0);
            mCloseNoiseSlider.setMinorTickCount(3);
            mCloseNoiseSlider.setShowTickMarks(true);
            mCloseNoiseSlider.setDisable(true);
            mCloseNoiseSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateNoise());
        }

        return mCloseNoiseSlider;
    }

    private Slider getOpenHysteresisSlider()
    {
        if(mOpenHysteresisSlider == null)
        {
            mOpenHysteresisSlider = new Slider(1, 5, 4);
            mOpenHysteresisSlider.setBlockIncrement(1);
            mOpenHysteresisSlider.setMajorTickUnit(1.0);
            mOpenHysteresisSlider.setMinorTickCount(0);
            mOpenHysteresisSlider.setShowTickMarks(true);
            mOpenHysteresisSlider.setDisable(true);
            mOpenHysteresisSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateHysteresis());
        }

        return mOpenHysteresisSlider;
    }

    private void updateHysteresis()
    {
        if(mController != null)
        {
            int open = (int)getOpenHysteresisSlider().getValue();
            int close = (int)getCloseHysteresisSlider().getValue() + open;

            if(open != mOpenHysteresis || close != mCloseHysteresis)
            {
                //Shadow copy to limit fire events to only true changes.
                mOpenHysteresis = open;
                mCloseHysteresis = close;
                mController.setHysteresisThreshold(open, close);
            }
        }
    }

    private Slider getCloseHysteresisSlider()
    {
        if(mCloseHysteresisSlider == null)
        {
            mCloseHysteresisSlider = new Slider(0, 5, 2);
            mCloseHysteresisSlider.setBlockIncrement(1);
            mCloseHysteresisSlider.setMajorTickUnit(1.0);
            mCloseHysteresisSlider.setMinorTickCount(0);
            mCloseHysteresisSlider.setShowTickMarks(true);
            mCloseHysteresisSlider.setDisable(true);
            mCloseHysteresisSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateHysteresis());
        }

        return mCloseHysteresisSlider;
    }

    private Label getSquelchStateLabel()
    {
        if(mSquelchStateLabel == null)
        {
            mSquelchStateLabel = new Label("Closed");
            mSquelchStateLabel.setDisable(true);
        }

        return mSquelchStateLabel;
    }

    private Label getNoiseValueLabel()
    {
        if(mNoiseValueLabel == null)
        {
            mNoiseValueLabel = new Label("0");
            getOpenNoiseSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateNoiseValue(0.0f));
            getCloseNoiseSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateNoiseValue(0.0f));
        }

        return mNoiseValueLabel;
    }

    private Label getHysteresisValueLabel()
    {
        if(mHysteresisValueLabel == null)
        {
            mHysteresisValueLabel = new Label("0 ms");
            getOpenHysteresisSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateHysteresisValue(0));
            getCloseHysteresisSlider().valueProperty().addListener((observable, oldValue, newValue) -> updateHysteresisValue(0));
        }

        return mHysteresisValueLabel;
    }

    private void updateHysteresisValue(int current)
    {
        int open = (int)getOpenHysteresisSlider().getValue();
        int close = (int)getCloseHysteresisSlider().getValue() + open;
        getHysteresisValueLabel().setText(open + " - " + close + " / " + current);
    }

    private void updateNoiseValue(float current)
    {
        float open = (float)getOpenNoiseSlider().getValue();
        float close = (float)getCloseNoiseSlider().getValue() + open;
        getNoiseValueLabel().setText(DECIMAL_FORMAT.format(open * NOISE_DISPLAY_SCALOR) + " - " +
                DECIMAL_FORMAT.format(close * NOISE_DISPLAY_SCALOR) + " / " +
                DECIMAL_FORMAT.format(current * NOISE_DISPLAY_SCALOR));
    }

    private LineChart<Number,Number> getActivityChart()
    {
        if(mActivityChart == null)
        {
            NumberAxis xAxis = new NumberAxis(0, 39,  10);
            NumberAxis yAxis = new NumberAxis(-1, 10, 1);

            mActivityChart = new LineChart<>(xAxis, yAxis);
            mActivityChart.setMaxHeight(Double.MAX_VALUE);
            mActivityChart.setMaxWidth(Double.MAX_VALUE);
            mActivityChart.setCreateSymbols(false); //Turn off data point markers
            mActivityChart.lookup(".chart-plot-background").setStyle("-fx-background-color: black;");

            for(int x = 0; x < 40; x++)
            {
                mNoiseSeries.getData().add(new XYChart.Data<>(x, 0));
                mNoiseOpenThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mNoiseCloseThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisOpenThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
                mHysteresisCloseThresholdSeries.getData().add(new XYChart.Data<>(x, 0));
            }

            mNoiseSeries.setName("Noise");
            mNoiseOpenThresholdSeries.setName("Open");
            mNoiseCloseThresholdSeries.setName("Close");

            mHysteresisSeries.setName("Hysteresis");
            mHysteresisOpenThresholdSeries.setName("Open");
            mHysteresisCloseThresholdSeries.setName("Close");

            mActivityChart.getData().add(mNoiseSeries);
            mActivityChart.getData().add(mNoiseOpenThresholdSeries);
            mActivityChart.getData().add(mNoiseCloseThresholdSeries);
            mActivityChart.getData().add(mHysteresisSeries);
            mActivityChart.getData().add(mHysteresisOpenThresholdSeries);
            mActivityChart.getData().add(mHysteresisCloseThresholdSeries);
        }

        return mActivityChart;
    }
}
