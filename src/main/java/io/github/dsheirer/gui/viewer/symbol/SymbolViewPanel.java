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

package io.github.dsheirer.gui.viewer.symbol;

import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX viewer application for visualizing sample buffers and detected sync patterns
 */
public class SymbolViewPanel extends VBox implements ISymbolResultsListener
{
    private static final double TWO_PI = Math.PI * 2.0;
    private final NumberAxis mRawTiming = new NumberAxis();
    private final NumberAxis mRawValue = new NumberAxis(-1.1, 1.1, .1);
    private final NumberAxis mDemodulatedTiming = new NumberAxis();
    private final NumberAxis mDemodulatedValue = new NumberAxis(-1.1, 1.1, .1);
    private final NumberAxis mSymbolTiming = new NumberAxis();
    private final NumberAxis mSymbolValue = new NumberAxis(-Math.PI, Math.PI, Math.PI / 4.0);
    private final LineChart<Number, Number> mRawChart = new LineChart<>(mRawTiming, mRawValue);
    private final LineChart<Number, Number> mDemodulatedChart = new LineChart<>(mDemodulatedTiming, mDemodulatedValue);
    private final LineChart<Number, Number> mSymbolChart = new LineChart<>(mSymbolTiming, mSymbolValue);
    private final Button mReleaseButton = new Button("Next Symbol");
    private final Label mDisplayText = new Label();
    private CountDownLatch mRelease;

    /**
     * Constructs an instance
     */
    public SymbolViewPanel()
    {
        setPadding(new Insets(5));
        mRawTiming.setLabel("Raw Sample Timing");
        mRawValue.setLabel("Raw Sample Value (+/- 1.0)");
        mDemodulatedTiming.setLabel("Demodulated Sample Timing");
        mDemodulatedValue.setLabel("Demodulated Sample Value (+/- 1.0)");
        mSymbolTiming.setLabel("Symbol Timing");
        mSymbolValue.setLabel("Symbol Value (+/- PI)");

        mReleaseButton.setOnAction(e -> {
            if(mRelease != null)
            {
                mRelease.countDown();
                mReleaseButton.setDisable(true);
            }
        });

        mRawChart.setAnimated(false);
        mDemodulatedChart.setAnimated(false);
        mSymbolChart.setAnimated(false);

        VBox.setVgrow(mRawChart, Priority.ALWAYS);
        VBox.setVgrow(mDemodulatedChart, Priority.ALWAYS);
        VBox.setVgrow(mSymbolChart, Priority.ALWAYS);

        HBox buttons = new HBox();
        buttons.setAlignment(Pos.BASELINE_LEFT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(mReleaseButton, mDisplayText);
        VBox.setVgrow(buttons, Priority.NEVER);

        getChildren().addAll(buttons, mRawChart, mDemodulatedChart, mSymbolChart);
    }

    @Override
    public void receive(double samplesPerSymbol, float[] rawI, float[] rawQ, float sampleGain, float pll,
                        double[] points, float[] symbolSequence, CountDownLatch release)
    {
        mRelease = release;
        mReleaseButton.setDisable(release == null);

        Platform.runLater(() -> {
            XYChart.Series<Number, Number> rawSeriesI = new XYChart.Series<>();
            rawSeriesI.setName("Raw Samples");
            XYChart.Series<Number, Number> rawSeriesQ = new XYChart.Series<>();
            XYChart.Series<Number, Number> rawTiming = new XYChart.Series<>();
            XYChart.Series<Number, Number> rawDecisionI = new XYChart.Series<>();
            XYChart.Series<Number, Number> rawDecisionQ = new XYChart.Series<>();
            XYChart.Series<Number, Number> demodulatedSeriesI = new XYChart.Series<>();
            XYChart.Series<Number, Number> demodulatedSeriesQ = new XYChart.Series<>();
            XYChart.Series<Number, Number> demodulatedTiming = new XYChart.Series<>();
            XYChart.Series<Number, Number> demodulatedDecisionI = new XYChart.Series<>();
            XYChart.Series<Number, Number> demodulatedDecisionQ = new XYChart.Series<>();

            XYChart.Series<Number, Number> symbolsRaw = new XYChart.Series<>();
            symbolsRaw.setName("Symbols Raw");
            XYChart.Series<Number, Number> symbolsPLL = new XYChart.Series<>();
            symbolsPLL.setName("Symbols PLL");
            XYChart.Series<Number, Number> symbolTiming = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolP3 = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolP1 = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolM1 = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolM3 = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolsWithPLL = new XYChart.Series<>();
            XYChart.Series<Number, Number> symbolsWithoutPLL = new XYChart.Series<>();

            float pllI = (float)Math.cos(pll);
            float pllQ = (float)Math.sin(pll);
            float pllTemp = 0;

            double spsMax = Math.ceil(samplesPerSymbol);
            double fractional = spsMax - samplesPerSymbol;
            for(int x = 0; x < rawI.length; x++)
            {
                rawSeriesI.getData().add(new XYChart.Data<>(x, rawI[x] * sampleGain));
                rawSeriesQ.getData().add(new XYChart.Data<>(x, rawQ[x] * sampleGain));

                if(x >= spsMax)
                {
                    int offset = x - (int)spsMax;
                    float previousI = LinearInterpolator.calculate(rawI[offset] * sampleGain, rawI[offset + 1] * sampleGain, fractional);
                    float previousQ = LinearInterpolator.calculate(rawQ[offset] * sampleGain, rawQ[offset + 1] * sampleGain, fractional);
                    float currentI = rawI[x] * sampleGain;
                    float currentQ = rawQ[x] * sampleGain;
                    float demodulatedI = (previousI * currentI) - (-previousQ * currentQ);
                    float demodulatedQ = (previousI * currentQ) + (-previousQ * currentI);

                    //Rotate by the PLL
                    pllTemp = (demodulatedI * pllI) - (demodulatedQ * pllQ);
                    demodulatedQ = (demodulatedQ * pllI) + (demodulatedI * pllQ);
                    demodulatedI = pllTemp;

                    demodulatedSeriesI.getData().add(new XYChart.Data<>(x, demodulatedI));
                    demodulatedSeriesQ.getData().add(new XYChart.Data<>(x, demodulatedQ));
                    symbolsRaw.getData().add(new XYChart.Data<>(x, Math.atan2(demodulatedQ, demodulatedI)));
//                    symbolsPLL.getData().add(new XYChart.Data<>(x, normalize(Math.atan2(demodulatedQ, demodulatedI) + pll)));
                }
            }

            rawTiming.getData().add(new XYChart.Data<>(0, -1));
            demodulatedTiming.getData().add(new XYChart.Data<>(0, -1));
            symbolTiming.getData().add(new XYChart.Data<>(0, -Math.PI));

            for(int x = 0; x < 3; x++)
            {
                rawTiming.getData().add(new XYChart.Data<>(points[x], -1));
                rawTiming.getData().add(new XYChart.Data<>(points[x], 1));
                rawTiming.getData().add(new XYChart.Data<>(points[x], -1));
                demodulatedTiming.getData().add(new XYChart.Data<>(points[x], -1));
                demodulatedTiming.getData().add(new XYChart.Data<>(points[x], 1));
                demodulatedTiming.getData().add(new XYChart.Data<>(points[x], -1));
                symbolTiming.getData().add(new XYChart.Data<>(points[x], -Math.PI));
                symbolTiming.getData().add(new XYChart.Data<>(points[x], Math.PI));
                symbolTiming.getData().add(new XYChart.Data<>(points[x], -Math.PI));
            }

            rawTiming.getData().add(new XYChart.Data<>(rawI.length - 1, -1));
            demodulatedTiming.getData().add(new XYChart.Data<>(rawI.length - 1, -1));
            symbolTiming.getData().add(new XYChart.Data<>(rawI.length - 1, -Math.PI));

            rawDecisionI.getData().add(new XYChart.Data<>(points[0], points[3]));
            rawDecisionI.getData().add(new XYChart.Data<>(points[1], points[4]));
            rawDecisionI.getData().add(new XYChart.Data<>(points[2], points[5]));

            rawDecisionQ.getData().add(new XYChart.Data<>(points[0], points[6]));
            rawDecisionQ.getData().add(new XYChart.Data<>(points[1], points[7]));
            rawDecisionQ.getData().add(new XYChart.Data<>(points[2], points[8]));

            demodulatedDecisionI.getData().add(new XYChart.Data<>(points[0], points[9]));
            demodulatedDecisionI.getData().add(new XYChart.Data<>(points[1], points[10]));
            demodulatedDecisionI.getData().add(new XYChart.Data<>(points[2], points[11]));

            demodulatedDecisionQ.getData().add(new XYChart.Data<>(points[0], points[12]));
            demodulatedDecisionQ.getData().add(new XYChart.Data<>(points[1], points[13]));
            demodulatedDecisionQ.getData().add(new XYChart.Data<>(points[2], points[14]));

            symbolP3.getData().add(new XYChart.Data<>(0, 3 * Math.PI / 4));
            symbolP1.getData().add(new XYChart.Data<>(0, Math.PI / 4));
            symbolM1.getData().add(new XYChart.Data<>(0, -Math.PI / 4));
            symbolM3.getData().add(new XYChart.Data<>(0, -3 * Math.PI / 4));
            symbolP3.getData().add(new XYChart.Data<>(rawI.length - 1, 3 * Math.PI / 4));
            symbolP1.getData().add(new XYChart.Data<>(rawI.length - 1, Math.PI / 4));
            symbolM1.getData().add(new XYChart.Data<>(rawI.length - 1, -Math.PI / 4));
            symbolM3.getData().add(new XYChart.Data<>(rawI.length - 1, -3 * Math.PI / 4));

            symbolsWithPLL.getData().add((new XYChart.Data<>(points[0], symbolSequence[0])));
//            symbolsWithPLL.getData().add((new XYChart.Data<>(points[1], symbolSequence[1])));
            symbolsWithPLL.getData().add((new XYChart.Data<>(points[2], symbolSequence[2])));
//            symbolsWithoutPLL.getData().add((new XYChart.Data<>(points[0], normalize(symbolSequence[0] - pll))));
//            symbolsWithoutPLL.getData().add((new XYChart.Data<>(points[1], normalize(symbolSequence[1] - pll))));
//            symbolsWithoutPLL.getData().add((new XYChart.Data<>(points[2], normalize(symbolSequence[2] - pll))));

            mRawChart.getData().clear();
            mRawChart.getData().add(rawSeriesI);
            mRawChart.getData().add(rawSeriesQ);
            mRawChart.getData().add(rawTiming);
            mRawChart.getData().add(rawDecisionI);
            mRawChart.getData().add(rawDecisionQ);
            mDemodulatedChart.getData().clear();
            mDemodulatedChart.getData().add(demodulatedSeriesI);
            mDemodulatedChart.getData().add(demodulatedSeriesQ);
            mDemodulatedChart.getData().add(demodulatedTiming);
            mDemodulatedChart.getData().add(demodulatedDecisionI);
            mDemodulatedChart.getData().add(demodulatedDecisionQ);
            mSymbolChart.getData().clear();
            mSymbolChart.getData().add(symbolsRaw);
//            mSymbolChart.getData().add(symbolsPLL);
            mSymbolChart.getData().add(symbolTiming);
            mSymbolChart.getData().add(symbolP3);
            mSymbolChart.getData().add(symbolP1);
            mSymbolChart.getData().add(symbolM1);
            mSymbolChart.getData().add(symbolM3);
            mSymbolChart.getData().add(symbolsWithPLL);
//            mSymbolChart.getData().add(symbolsWithoutPLL);
            mDisplayText.setText("PLL: " + pll + " Sample Gain: " + sampleGain);
        });
    }

    /**
     * Normalizes the value in radians to the range of +/-(2 * PI)
     * @param value to normalize
     * @return normalized value.
     */
    private static double normalize(double value)
    {
        if(value > Math.PI)
        {
            return value - TWO_PI;
        }
        else if(value < -Math.PI)
        {
            return value + TWO_PI;
        }

        return value;
    }
}
