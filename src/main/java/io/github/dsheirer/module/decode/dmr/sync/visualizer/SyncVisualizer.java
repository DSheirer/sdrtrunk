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

package io.github.dsheirer.module.decode.dmr.sync.visualizer;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX viewer application for visualizing sample buffers and detected sync patterns
 */
public class SyncVisualizer extends VBox implements ISyncResultsListener
{
    private static final float SYMBOL = (float)(Math.PI / 4.0);
    private final NumberAxis mConstellationI = new NumberAxis();
    private final NumberAxis mConstellationQ = new NumberAxis();
    private final NumberAxis mSampleTiming = new NumberAxis();
    private final NumberAxis mSamplePhase = new NumberAxis();
    private final ScatterChart<Number,Number> mConstellationChart = new ScatterChart<>(mConstellationI, mConstellationQ);
    private final LineChart<Number,Number> mSampleChart = new LineChart<>(mSampleTiming, mSamplePhase);
    private final Button mReleaseButton = new Button("Next Sync");
    private final Label mPatternLabel = new Label();
    private CountDownLatch mRelease;

    /**
     * Constructs an instance
     */
    public SyncVisualizer()
    {
        setPadding(new Insets(5));
        mConstellationI.setLabel("Inphase");
        mConstellationQ.setLabel("Quadrature");
        mSampleTiming.setLabel("Sample Timing");
        mSamplePhase.setLabel("Sample Phase (+/- PI)");

        mReleaseButton.setOnAction(e ->
        {
            if(mRelease != null)
            {
                mRelease.countDown();
                mReleaseButton.setDisable(true);
            }
        });

        mConstellationChart.setPrefSize(400, 400);
        HBox hbox = new HBox();
        hbox.getChildren().add(mConstellationChart);
//        VBox.setVgrow(hbox, Priority.ALWAYS);
        VBox.setVgrow(mSampleChart, Priority.ALWAYS);

        HBox buttons = new HBox();
        buttons.setAlignment(Pos.BASELINE_CENTER);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(mReleaseButton, mPatternLabel);
        VBox.setVgrow(buttons, Priority.NEVER);

        getChildren().addAll(hbox, mSampleChart, buttons);
    }

    @Override
    public void receive(float[] symbols, float[] samples, float[] sync, float[] syncIntervals, String label, CountDownLatch latch)
    {
        mRelease = latch;
        mReleaseButton.setDisable(latch == null);

        Platform.runLater(() -> {
            XYChart.Series<Number,Number> constellationIdeal = new XYChart.Series<>();
            constellationIdeal.setName("Constellation Ideal");
            float PI_4 = (float)(Math.PI / 4);
            constellationIdeal.getData().add(new XYChart.Data<>(PI_4, PI_4));
            constellationIdeal.getData().add(new XYChart.Data<>(PI_4, -PI_4));
            constellationIdeal.getData().add(new XYChart.Data<>(-PI_4, PI_4));
            constellationIdeal.getData().add(new XYChart.Data<>(-PI_4, -PI_4));
            constellationIdeal.getData().add(new XYChart.Data<>(1, 0));
            constellationIdeal.getData().add(new XYChart.Data<>(-1, 0));
            constellationIdeal.getData().add(new XYChart.Data<>(0, 1));
            constellationIdeal.getData().add(new XYChart.Data<>(0, -1));

            mConstellationChart.getData().clear();
            mConstellationChart.getData().add(constellationIdeal);

            XYChart.Series<Number,Number> constellation = new XYChart.Series<>();
            constellation.setName("Constellation");

            for(int x = 0; x < symbols.length; x++)
            {
                constellation.getData().add(new XYChart.Data<>(Math.cos(symbols[x]), Math.sin(symbols[x])));
            }

            mConstellationChart.getData().add(constellation);


            XYChart.Series<Number,Number> sampleSeries = new XYChart.Series<>();
            sampleSeries.setName("Demodulated Samples");

            for(int x = 0; x < samples.length; x++)
            {
                sampleSeries.getData().add(new XYChart.Data<>(x, samples[x]));
            }

            mSampleChart.getData().clear();
            mSampleChart.getData().add(sampleSeries);

            XYChart.Series<Number,Number> patternSeries = new XYChart.Series<>();
            patternSeries.setName("Ideal Sync Pattern");

            for(int x = 0; x < sync.length; x++)
            {
                patternSeries.getData().add(new XYChart.Data<>(syncIntervals[x], sync[x]));
            }

            mSampleChart.getData().add(patternSeries);

            mPatternLabel.setText(label);
        });
    }
}
