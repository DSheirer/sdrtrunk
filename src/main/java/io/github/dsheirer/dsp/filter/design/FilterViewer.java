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

package io.github.dsheirer.dsp.filter.design;

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
//        FIRFilterSpecification specification = FIRFilterSpecification
//                .lowPassBuilder()
//                .sampleRate(12500)
//                .passBandCutoff(2600)
//                .passBandAmplitude(1.0).passBandRipple(0.01)
//                .stopBandAmplitude(0.0).stopBandStart(3125)
//                .stopBandRipple(0.01).build();
//
//        float[] taps = null;
//
//        try
//        {
//            taps = FilterFactory.getTaps(specification);
//        }
//        catch(Exception fde) //FilterDesignException
//        {
//            System.out.println("Error");
//        }

        float decimatedSampleRate = 12500f;
        int symbolLength = 26;
        float rrcAlpha = 0.2f;

//        float[] taps = FilterFactory.getRootRaisedCosine(decimatedSampleRate / 4800, symbolLength, rrcAlpha);
//        float[] taps = FilterFactory.getSinc(1100d / 12500d, 31, WindowType.BLACKMAN_HARRIS_7);
//        float[] taps = FilterFactory.getInverseSync(1100d/12500d, 31, WindowType.BLACKMAN_HARRIS_7);

        float[] taps = { +0.031462429f, +0.031747267f, +0.030401148f, +0.027362877f,
                +0.022653298f, +0.016379869f, +0.008737200f, +0.000003302f,
                -0.009468531f, -0.019262057f, -0.028914291f, -0.037935027f,
                -0.045828927f, -0.052119261f, -0.056372283f, -0.058221106f,
                -0.057387924f, -0.053703443f, -0.047122444f, -0.037734535f,
                -0.025769308f, -0.011595336f, +0.004287292f, +0.021260954f,
                +0.038610717f, +0.055550276f, +0.071252765f, +0.084885375f,
                +0.095646450f, +0.102803611f, +0.105731303f, +0.103946126f,
                +0.097138329f, +0.085197939f, +0.068234131f, +0.046586711f,
                +0.020828821f, -0.008239664f, -0.039608255f, -0.072081234f,
                -0.104311776f, -0.134843790f, -0.162160200f, -0.184736015f,
                -0.201094346f, -0.209863285f, -0.209831516f, -0.200000470f,
                -0.179630919f, -0.148282051f, -0.105841323f, -0.052543664f,
                +0.011020985f, +0.083912428f, +0.164857408f, +0.252278939f,
                +0.344336996f, +0.438979335f, +0.534000832f, +0.627109358f,
                +0.715995947f, +0.798406824f, +0.872214756f, +0.935487176f,
                +0.986548646f, +1.024035395f, +1.046939951f, +1.054644241f,
                +1.046939951f, +1.024035395f, +0.986548646f, +0.935487176f,
                +0.872214756f, +0.798406824f, +0.715995947f, +0.627109358f,
                +0.534000832f, +0.438979335f, +0.344336996f, +0.252278939f,
                +0.164857408f, +0.083912428f, +0.011020985f, -0.052543664f,
                -0.105841323f, -0.148282051f, -0.179630919f, -0.200000470f,
                -0.209831516f, -0.209863285f, -0.201094346f, -0.184736015f,
                -0.162160200f, -0.134843790f, -0.104311776f, -0.072081234f,
                -0.039608255f, -0.008239664f, +0.020828821f, +0.046586711f,
                +0.068234131f, +0.085197939f, +0.097138329f, +0.103946126f,
                +0.105731303f, +0.102803611f, +0.095646450f, +0.084885375f,
                +0.071252765f, +0.055550276f, +0.038610717f, +0.021260954f,
                +0.004287292f, -0.011595336f, -0.025769308f, -0.037734535f,
                -0.047122444f, -0.053703443f, -0.057387924f, -0.058221106f,
                -0.056372283f, -0.052119261f, -0.045828927f, -0.037935027f,
                -0.028914291f, -0.019262057f, -0.009468531f, +0.000003302f,
                +0.008737200f, +0.016379869f, +0.022653298f, +0.027362877f,
                +0.030401148f, +0.031747267f, +0.031462429f
        };

        if(taps == null)
        {
            throw new IllegalStateException("Couldn't design filter");
        }

        return taps;
    }
}
