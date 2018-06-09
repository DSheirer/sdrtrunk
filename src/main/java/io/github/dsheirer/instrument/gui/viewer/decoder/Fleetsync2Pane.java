/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer.decoder;

import io.github.dsheirer.instrument.gui.viewer.chart.DecodedSymbolChart;
import io.github.dsheirer.instrument.gui.viewer.chart.Fleetsync2SampleBufferChart;
import io.github.dsheirer.instrument.gui.viewer.chart.FleetsyncZeroCrossingErrorDetectorChart;
import io.github.dsheirer.instrument.gui.viewer.chart.RealSampleLineChart;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.fleetsync2.Fleetsync2DecoderInstrumented;
import io.github.dsheirer.sample.Listener;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fleetsync2Pane extends RealDecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(Fleetsync2Pane.class);

    private HBox mSampleChartBox;
    private RealSampleLineChart mRealSampleLineChart;
    private HBox mSymbolDecoderBox;
    private Fleetsync2SampleBufferChart mSampleBufferChart;
    private FleetsyncZeroCrossingErrorDetectorChart mZeroCrossingErrorDetectorChart;
    private DecodedSymbolChart mDecodedSymbolChart;

    private Fleetsync2DecoderInstrumented mDecoder;

    public Fleetsync2Pane()
    {
        super(DecoderType.LTR_NET);
        init();
    }

    private void init()
    {
        addListener(getDecoder());
        addListener(getSampleLineChart());

        HBox.setHgrow(getSampleChartBox(), Priority.ALWAYS);
        HBox.setHgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        VBox.setVgrow(getSampleChartBox(), Priority.ALWAYS);
        VBox.setVgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        getSampleChartBox().setMaxWidth(Double.MAX_VALUE);
        getSymbolDecoderBox().setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(getSampleChartBox(), getSymbolDecoderBox());

        getDecoder().getAFSK1200Decoder().addListener(getDecodedSymbolChart());

        getDecoder().setMessageListener(new Listener<Message>()
        {
            @Override
            public void receive(Message message)
            {
                mLog.debug(message.getMessage());
            }
        });
    }

    private Fleetsync2DecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            mDecoder = new Fleetsync2DecoderInstrumented();
            mDecoder.setMessageListener(new Listener<Message>()
            {
                @Override
                public void receive(Message message)
                {
                    mLog.debug(message.getMessage());
                }
            });
        }

        return mDecoder;
    }

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

    private Fleetsync2SampleBufferChart getSampleBufferChart()
    {
        if(mSampleBufferChart == null)
        {
            mSampleBufferChart = new Fleetsync2SampleBufferChart(getDecoder(), 106);
        }

        return mSampleBufferChart;
    }

    private FleetsyncZeroCrossingErrorDetectorChart getZeroCrossingErrorDetectorChart()
    {
        if(mZeroCrossingErrorDetectorChart == null)
        {
            mZeroCrossingErrorDetectorChart = new FleetsyncZeroCrossingErrorDetectorChart(getDecoder(),
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
