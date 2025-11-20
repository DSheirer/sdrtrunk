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

package io.github.dsheirer.gui.viewer.sync;

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
public class FMSyncVisualizer extends VBox implements ISyncResultsListener
{
    private static final int SYMBOL_CHART_SERIES_COUNT = 2000;
    private static final float SYMBOL = (float) (Math.PI / 4.0);
    private final NumberAxis mConstellationI = new NumberAxis();
    private final NumberAxis mConstellationQ = new NumberAxis();
    private final NumberAxis mSampleTiming = new NumberAxis();
    private final NumberAxis mSamplePhase = new NumberAxis(-3.5, 3.5, .1);
    private final NumberAxis mSymbolTiming = new NumberAxis();
    private final NumberAxis mSymbolPhase = new NumberAxis(-Math.PI, Math.PI, Math.PI / 2);
    private final XYChart.Series<Number, Number> mSymbolSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> mSymbolSeriesMarker = new XYChart.Series<>();
    private final ScatterChart<Number, Number> mConstellationChart = new ScatterChart<>(mConstellationI, mConstellationQ);
    private final LineChart<Number, Number> mSampleChart = new LineChart<>(mSampleTiming, mSamplePhase);
    private final ScatterChart<Number, Number> mSymbolChart = new ScatterChart<>(mSymbolTiming, mSymbolPhase);

    private final NumberAxis mEqualizerHistoryAxis = new NumberAxis();
    private final NumberAxis mEqualizerValueAxis = new NumberAxis(-8.5, 8.5, .25);
    private final LineChart<Number, Number> mEqualizerChart = new LineChart<>(mEqualizerHistoryAxis, mEqualizerValueAxis);
    private int mEqualizerPointer = 0;
    private int mEqualizerHistorySize = 40;
    private float[] mEqualizerBalanceHistory = new float[mEqualizerHistorySize];
    private float[] mEqualizerGainHistory = new float[mEqualizerHistorySize];

    private final Button mReleaseButton = new Button("Next Sync");
    private final Label mPatternLabel = new Label();
    private CountDownLatch mRelease;
    private int mSymbolPointer = 0;

    /**
     * Constructs an instance
     */
    public FMSyncVisualizer()
    {
        setPadding(new Insets(5));
        mConstellationI.setLabel("Constellation - Inphase");
        mConstellationQ.setLabel("Quadrature");
        mSampleTiming.setLabel("Sample Timing");
        mSamplePhase.setLabel("Sample Phase (+/- PI)");

        mReleaseButton.setOnAction(e -> {
            if(mRelease != null)
            {
                mRelease.countDown();
                mReleaseButton.setDisable(true);
            }
        });

        mConstellationChart.setPrefSize(400, 400);
        mConstellationChart.setAnimated(false);
        mSampleChart.setAnimated(false);
        mSymbolChart.setAnimated(false);
        mSymbolChart.setId("symbol-chart");
        mEqualizerChart.setAnimated(false);

        mEqualizerValueAxis.setLabel("Value");
        mEqualizerHistoryAxis.setLabel("Equalizer");

        HBox hbox = new HBox();
        hbox.getChildren().add(mConstellationChart);
        HBox.setHgrow(mEqualizerChart, Priority.ALWAYS);
        hbox.getChildren().add(mEqualizerChart);
        //        VBox.setVgrow(hbox, Priority.ALWAYS);

        VBox.setVgrow(mSampleChart, Priority.ALWAYS);
        VBox.setVgrow(mSymbolChart, Priority.ALWAYS);

        HBox buttons = new HBox();
        buttons.setAlignment(Pos.BASELINE_LEFT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(mReleaseButton, mPatternLabel);
        VBox.setVgrow(buttons, Priority.NEVER);

        //Load the symbol chart with symbols
        mSymbolSeries.setName("Symbols");
        for(int x = 0; x < SYMBOL_CHART_SERIES_COUNT; x++)
        {
            mSymbolSeries.getData().add(new XYChart.Data<>(x, 0));
        }
        mSymbolChart.getData().add(mSymbolSeries);
        float pi_4 = (float)Math.PI / 4f;
        mSymbolSeriesMarker.getData().add(new XYChart.Data<>(0, 3f * pi_4));
        mSymbolSeriesMarker.getData().add(new XYChart.Data<>(0, pi_4));
        mSymbolSeriesMarker.getData().add(new XYChart.Data<>(0, 0));
        mSymbolSeriesMarker.getData().add(new XYChart.Data<>(0, -pi_4));
        mSymbolSeriesMarker.getData().add(new XYChart.Data<>(0, -3 * pi_4));
        mSymbolChart.getData().add(mSymbolSeriesMarker);
        getChildren().addAll(buttons, hbox, mSampleChart, mSymbolChart);
    }

    @Override
    public void symbol(float symbol)
    {
        for(XYChart.Data<Number, Number> data : mSymbolSeriesMarker.getData())
        {
            data.setXValue(mSymbolPointer);
        }
        mSymbolSeries.getData().get(mSymbolPointer++).setYValue(symbol);
        mSymbolPointer %= SYMBOL_CHART_SERIES_COUNT;
    }

    @Override
    public void receive(float[] symbols, float[] samples, float[] sync, float[] syncIntervals, float equalizerBalance, float equalizerGain, String label, CountDownLatch latch)
    {
        mRelease = latch;
        mReleaseButton.setDisable(latch == null);

        mEqualizerBalanceHistory[mEqualizerPointer] = equalizerBalance;
        mEqualizerGainHistory[mEqualizerPointer++] = (equalizerGain - 1.0f);
        mEqualizerPointer %= mEqualizerHistorySize;
        //Clear the next/upcoming values to give visual indication of where we're at.
        mEqualizerBalanceHistory[mEqualizerPointer] = 0f;
        mEqualizerGainHistory[mEqualizerPointer] = 0f;

        Platform.runLater(() -> {
            XYChart.Series<Number, Number> constellationIdeal = new XYChart.Series<>();
            constellationIdeal.setName("Ideal");
            float PI_4 = (float) (Math.PI / 4);
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

            XYChart.Series<Number, Number> constellation = new XYChart.Series<>();
            constellation.setName("Decoded");

            for(int x = 0; x < symbols.length; x++)
            {
                constellation.getData().add(new XYChart.Data<>(Math.cos(symbols[x]), Math.sin(symbols[x])));
            }

            mConstellationChart.getData().add(constellation);

            XYChart.Series<Number, Number> sampleSeries = new XYChart.Series<>();
            sampleSeries.setName("Demodulated Samples");

            for(int x = 0; x < samples.length; x++)
            {
                sampleSeries.getData().add(new XYChart.Data<>(x, samples[x]));
            }

            mSampleChart.getData().clear();
            mSampleChart.getData().add(sampleSeries);

            XYChart.Series<Number, Number> patternSeries = new XYChart.Series<>();
            patternSeries.setName("Ideal Sync Pattern");

            for(int x = 0; x < sync.length; x++)
            {
                patternSeries.getData().add(new XYChart.Data<>(syncIntervals[x], sync[x]));
            }

            mSampleChart.getData().add(patternSeries);

            mPatternLabel.setText(label);
            mEqualizerChart.getData().clear();

            XYChart.Series<Number, Number> balanceSeries = new XYChart.Series<>();
            balanceSeries.setName("Balance");
            XYChart.Series<Number, Number> gainSeries = new XYChart.Series<>();
            gainSeries.setName("Gain (Value minus 1.0)");

            for(int x = 0; x < mEqualizerHistorySize; x++)
            {
                balanceSeries.getData().add(new XYChart.Data<>(x, mEqualizerBalanceHistory[x]));
                gainSeries.getData().add(new XYChart.Data<>(x, mEqualizerGainHistory[x]));
            }

            mEqualizerChart.getData().add(balanceSeries);
            mEqualizerChart.getData().add(gainSeries);
        });
    }
}
