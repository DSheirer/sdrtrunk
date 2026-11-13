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

package io.github.dsheirer.gui.symbol;

import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Listener;
import java.net.URL;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Chart display for received QPSK symbols.  Colors follow a spectral display theme: the theme background, the
 * spectrum trace color for the symbols, and the theme line color for the grid, axes and legend.
 */
public class SymbolView extends ChannelView implements Listener<Float>
{
    private static final int SYMBOL_DISPLAY_COUNT = 4800;
    private static final int SYMBOL_QUEUE_SIZE = SYMBOL_DISPLAY_COUNT / 40;
    private final float[] mSymbolQueue = new float[SYMBOL_QUEUE_SIZE];
    private int mSymbolQueuePointer = 0;
    private int mChartSymbolPointer = 0;
    private final NumberAxis mSymbolTiming = new NumberAxis();
    private final NumberAxis mSymbolPhase = new NumberAxis(-Math.PI, Math.PI, Math.PI / 2);
    private final XYChart.Series<Number, Number> mSymbolSeries = new XYChart.Series<>();
    private final ScatterChart<Number, Number> mSymbolChart = new ScatterChart<>(mSymbolTiming, mSymbolPhase);
    private FeedbackDecoder mFeedbackDecoder;

    /**
     * Constructs an instance
     */
    public SymbolView()
    {
        mSymbolSeries.setName("xx Demodulated Symbols");

        for(int x = 0; x < SYMBOL_DISPLAY_COUNT; x++)
        {
            mSymbolSeries.getData().add(new XYChart.Data<>(x, 0));
        }

        getStyleClass().add("symbol-view");
        URL stylesheet = SymbolView.class.getResource("/spectral_display.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        mSymbolChart.getData().add(mSymbolSeries);
        mSymbolChart.setMaxHeight(Double.MAX_VALUE);
        mSymbolChart.setMaxWidth(Double.MAX_VALUE);
        mSymbolChart.setAnimated(false);
        VBox.setVgrow(mSymbolChart, Priority.ALWAYS);
        getChildren().add(mSymbolChart);
    }

    /**
     * Applies the theme colors now and whenever the theme changes.  JavaFX thread only.
     * @param theme to follow, for example the theme shared by the spectral displays
     */
    public void followTheme(ObservableValue<SpectralDisplayTheme> theme)
    {
        theme.addListener((ob, ov, nv) -> setTheme(nv));
        setTheme(theme.getValue());
    }

    /**
     * Applies the theme colors to the chart.  JavaFX thread only.
     */
    public void setTheme(SpectralDisplayTheme theme)
    {
        if(theme == null)
        {
            theme = SpectralDisplayTheme.getDefault();
        }

        Color line = theme.getLine();
        Color text = Color.color(line.getRed(), line.getGreen(), line.getBlue());
        Color trace = theme.getTrace();
        Color symbol = Color.color(trace.getRed(), trace.getGreen(), trace.getBlue(),
                Math.max(0.6, trace.getOpacity()));

        //Looked-up colors used by the symbol-view rules in spectral_display.css
        setStyle("symbol-background: " + toWeb(theme.getBackground()) + ";" +
                "symbol-color: " + toWeb(symbol) + ";" +
                "symbol-grid: " + toWeb(theme.getLine(0.35)) + ";" +
                "symbol-text: " + toWeb(text) + ";");
    }

    private static String toWeb(Color color)
    {
        return String.format("#%02x%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255),
                Math.round(color.getOpacity() * 255));
    }

    /**
     * Updates the chart axis label with the currently displayed protocol
     * @param protocol that is being processed/displayed.
     */
    public void setProtocol(String protocol)
    {
        Platform.runLater(() -> mSymbolSeries.setName(protocol + " Demodulated Symbols"));
    }

    /**
     * Registers the decoder with this view and this view then registers as a symbol listener on the decoder.
     * @param feedbackDecoder for this view
     */
    public void setSymbolProvider(FeedbackDecoder feedbackDecoder)
    {
        removeSymbolProvider();

        mFeedbackDecoder = feedbackDecoder;

        if(mFeedbackDecoder != null)
        {
            mFeedbackDecoder.setSymbolListener(this);
        }
    }

    /**
     * Removes current symbol provider and unregisters this view from that provider.
     */
    public void removeSymbolProvider()
    {
        if(mFeedbackDecoder != null)
        {
            mFeedbackDecoder.removeSymbolListener();
        }

        mFeedbackDecoder = null;
    }

    @Override
    public void receive(Float symbol)
    {
        if(isShowing())
        {
            mSymbolQueue[mSymbolQueuePointer++] = symbol;

            //Flush the queued symbols to the chart when the queue is full
            if(mSymbolQueuePointer == SYMBOL_QUEUE_SIZE)
            {
                float[] symbolDataCopy = Arrays.copyOf(mSymbolQueue, mSymbolQueue.length);

                //Execute the chart data update on the JavaFX UI thread
                Platform.runLater(() ->
                {
                    for(float v : symbolDataCopy)
                    {
                        mSymbolSeries.getData().get(mChartSymbolPointer++).setYValue(v);
                        mChartSymbolPointer %= SYMBOL_DISPLAY_COUNT;
                    }

                });

                mSymbolQueuePointer = 0;
            }
        }
    }
}
