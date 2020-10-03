/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.dsp.filter.design;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterViewer extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(FilterViewer.class);

    /**
     * Developer tool to visualize filter designs
     */
    public FilterViewer()
    {
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Scene scene = new Scene(new FilterView(getFilter()));

        primaryStage.setTitle("Filter Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    /**
     * Provides the filter to visualize.  Modify this method to visualize your filter design.
     */
    private float[] getFilter()
    {
//        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
//            .sampleRate(8000)
//            .gridDensity(16)
//            .oddLength(true)
//            .passBandCutoff(300)
//            .passBandAmplitude(1.0)
//            .passBandRipple(0.01)
//            .stopBandStart(500)
//            .stopBandAmplitude(0.0)
//            .stopBandRipple(0.03) //Approximately 60 dB attenuation
//            .build();
//
//        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
//            .sampleRate(8000)
//            .stopBandCutoff(200)
//            .stopBandAmplitude(0.0)
//            .stopBandRipple(0.025)
//            .passBandStart(300)
//            .passBandAmplitude(1.0)
//            .passBandRipple(0.01)
//            .build();

//        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
//            .sampleRate(50000.0)
//            .passBandCutoff(6500)
//            .passBandAmplitude(1.0)
//            .passBandRipple(0.005)
//            .stopBandAmplitude(0.0)
//            .stopBandStart(7200)
//            .stopBandRipple(0.01)
//            .build();

        float[] taps = null;

        boolean rrc = false;

        if(rrc)
        {
            double sampleRate = 50000.0;
            int symbolRate = 4800;
            double samplesPerSymbol = sampleRate / symbolRate / 2;
            float alpha = 0.25f;
            int symbolCount = 20;

            taps = FilterFactory.getRootRaisedCosine(samplesPerSymbol, symbolCount, alpha);
        }
        else
        {
            FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                    .sampleRate(50000)
                    .passBandCutoff(4500)
                    .passBandAmplitude(1.0)
                    .passBandRipple(0.1)
                    .stopBandAmplitude(0.0)
                    .stopBandStart(7000)
                    .stopBandRipple(0.01)
                    .build();

            try
            {
                taps = FilterFactory.getTaps(specification);//
            }
            catch(Exception fde) //FilterDesignException
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate");
            }
        }


        if(taps == null)
        {
            throw new IllegalStateException("Couldn't design filter");
        }

        return taps;
    }
}
