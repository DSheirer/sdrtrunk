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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.gui.instrument.chart.AFSK1200SampleBufferChart;
import io.github.dsheirer.gui.instrument.chart.AFSK1200ZeroCrossingErrorDetectorChart;
import io.github.dsheirer.gui.instrument.chart.DecodedSymbolChart;
import io.github.dsheirer.gui.instrument.chart.IInstrumentedAFSK1200Decoder;
import io.github.dsheirer.gui.instrument.chart.RealSampleLineChart;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAFSK1200Pane extends RealDecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractAFSK1200Pane.class);

    private HBox mSampleChartBox;
    private RealSampleLineChart mRealSampleLineChart;
    private HBox mSymbolDecoderBox;
    private AFSK1200SampleBufferChart mSampleBufferChart;
    private AFSK1200ZeroCrossingErrorDetectorChart mZeroCrossingErrorDetectorChart;
    private DecodedSymbolChart mDecodedSymbolChart;

    private IInstrumentedAFSK1200Decoder mDecoder;

    public AbstractAFSK1200Pane(DecoderType decoderType)
    {
        super(decoderType);
        init();
    }

    private void init()
    {
        //This is force-cast without type checking ....
        addListener((Listener<float[]>) getDecoder());

        addListener(getSampleLineChart());

        HBox.setHgrow(getSampleChartBox(), Priority.ALWAYS);
        HBox.setHgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        VBox.setVgrow(getSampleChartBox(), Priority.ALWAYS);
        VBox.setVgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        getSampleChartBox().setMaxWidth(Double.MAX_VALUE);
        getSymbolDecoderBox().setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(getSampleChartBox(), getSymbolDecoderBox());

        getDecoder().getAFSK1200Decoder().addListener(getDecodedSymbolChart());

        getDecoder().setMessageListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage message)
            {
                mLog.debug((message.isValid() ? "PASS " : "FAIL ") + message.toString());
            }
        });
    }

    protected abstract IInstrumentedAFSK1200Decoder getDecoder();

    private HBox getSampleChartBox()
    {
        if(mSampleChartBox == null)
        {
            mSampleChartBox = new HBox();
            mSampleChartBox.setMaxHeight(Double.MAX_VALUE);

            getSampleLineChart().setMaxWidth(Double.MAX_VALUE);
            getDecodedSymbolChart().setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(getSampleLineChart(), Priority.ALWAYS);
            HBox.setHgrow(getDecodedSymbolChart(), Priority.ALWAYS);

            mSampleChartBox.getChildren().addAll(getSampleLineChart(), getDecodedSymbolChart());
        }

        return mSampleChartBox;
    }

    private RealSampleLineChart getSampleLineChart()
    {
        if(mRealSampleLineChart == null)
        {
            mRealSampleLineChart = new RealSampleLineChart(200, (10.0));
        }

        return mRealSampleLineChart;
    }

    private HBox getSymbolDecoderBox()
    {
        if(mSymbolDecoderBox == null)
        {
            mSymbolDecoderBox = new HBox();
            mSymbolDecoderBox.setMaxWidth(Double.MAX_VALUE);

            getSampleBufferChart().setMaxWidth(Double.MAX_VALUE);
            getZeroCrossingErrorDetectorChart().setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(getSampleBufferChart(), Priority.ALWAYS);
            HBox.setHgrow(getZeroCrossingErrorDetectorChart(), Priority.ALWAYS);

            mSymbolDecoderBox.getChildren().addAll(getSampleBufferChart(), getZeroCrossingErrorDetectorChart());
        }

        return mSymbolDecoderBox;
    }

    private AFSK1200SampleBufferChart getSampleBufferChart()
    {
        if(mSampleBufferChart == null)
        {
            mSampleBufferChart = new AFSK1200SampleBufferChart(getDecoder(), 106);
        }

        return mSampleBufferChart;
    }

    private AFSK1200ZeroCrossingErrorDetectorChart getZeroCrossingErrorDetectorChart()
    {
        if(mZeroCrossingErrorDetectorChart == null)
        {
            mZeroCrossingErrorDetectorChart = new AFSK1200ZeroCrossingErrorDetectorChart(getDecoder(),
                    getDecoder().getAFSK1200Decoder().getErrorDetector().getBuffer().length);
        }

        return mZeroCrossingErrorDetectorChart;
    }

    private DecodedSymbolChart getDecodedSymbolChart()
    {
        if(mDecodedSymbolChart == null)
        {
            mDecodedSymbolChart = new DecodedSymbolChart(50);
        }

        return mDecodedSymbolChart;
    }
}
