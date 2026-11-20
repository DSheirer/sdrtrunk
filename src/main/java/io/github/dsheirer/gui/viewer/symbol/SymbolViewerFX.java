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

import java.util.concurrent.CountDownLatch;
import io.github.dsheirer.gui.FxToolkit;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility for viewing sync detection results
 */
public class SymbolViewerFX implements ISymbolResultsListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolViewerFX.class.getName());
    private ISymbolResultsListener mListener;

    public SymbolViewerFX()
    {
        initUI();
    }

    @Override
    public void receive(double samplesPerSymbol, float[] rawI, float[] rawQ, float sampleGain, float pll,
                        double[] points, float[] symbols, CountDownLatch releasee)
    {
        if(mListener != null)
        {
            mListener.receive(samplesPerSymbol, rawI, rawQ, sampleGain, pll, points, symbols, releasee);
        }
    }

    /**
     * Initializes the viewer UI
     */
    private void initUI()
    {
        CountDownLatch latch = new CountDownLatch(1);

        FxToolkit.ensureStarted();
        Platform.runLater(() -> {
            Platform.setImplicitExit(false);
            SymbolViewPanel viewer = new SymbolViewPanel();
            mListener = viewer;
            Scene scene = new Scene(viewer, 1400, 1000);

//            URL resource = getClass().getResource("/sdrtrunk_style.css");
//
//            if(resource != null)
//            {
//                scene.getStylesheets().add(resource.toExternalForm());
//            }
//            else
//            {
//                LOGGER.warn("Can't find stylesheet resource for sdrtrunk");
//            }

            Stage stage = new Stage();
            stage.setTitle("Symbol Results Viewer");
            stage.setScene(scene);
            //Closing the viewer exits the application, the same as the Swing viewer did
            stage.setOnCloseRequest(event -> System.exit(0));
            stage.show();
            latch.countDown();
        });

        try
        {
            latch.await();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}