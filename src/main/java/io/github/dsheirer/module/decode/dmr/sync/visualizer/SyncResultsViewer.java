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

import io.github.dsheirer.util.SwingUtils;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.JFrame;

/**
 * Utility for viewing sync detection results
 */
public class SyncResultsViewer implements ISyncResultsListener
{
    private ISyncResultsListener mListener;

    public SyncResultsViewer()
    {
        initUI();
    }

    /**
     * View sync detection results.
     * @param symbols that were detected
     * @param samples for the symbols
     * @param sync that was detected
     * @param syncIntervals symbol timing interval pointers into the I/Q sample arrays
     * @param label to display to the user
     * @param release latch to release via the UI when done viewing these results
     */
    @Override
    public void receive(float[] symbols, float[] sync, float[] samples, float[] syncIntervals, String label, CountDownLatch release)
    {
        if(mListener != null)
        {
            mListener.receive(symbols, samples, sync, syncIntervals, label, release);
        }
    }

    /**
     * Initializes the viewer UI
     */
    private void initUI()
    {
        CountDownLatch latch = new CountDownLatch(1);

        final JFXPanel fxPanel = new JFXPanel();
        Platform.runLater(() -> {
            Platform.setImplicitExit(false);
            SyncVisualizer syncVisualizer = new SyncVisualizer();
            mListener = syncVisualizer;
            Scene scene = new Scene(syncVisualizer, 1400, 1000);
            fxPanel.setScene(scene);
            fxPanel.setVisible(true);

            JFrame frame = new JFrame();
            frame.setTitle("Sync Results Viewer");
            frame.setContentPane(fxPanel);
            frame.setSize(1400, 1000);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            SwingUtils.run(() -> {
                frame.setVisible(true);
                latch.countDown();
            });
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