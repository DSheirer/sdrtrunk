/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.source.tuner.Tuner;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import net.miginfocom.swing.MigLayout;

import javax.swing.JFrame;

/**
 * Swing frame for showing a separate spectrum display for a tuner.
 */
public class SpectrumFrame extends JFrame implements WindowListener
{
    private static final long serialVersionUID = 1L;

    @Resource
    private SpectralDisplayPanel mSpectralDisplayPanel;

    /**
     * Constructs an instance
     */
    public SpectrumFrame()
    {
    }

    /**
     * Post instantiation/startup steps.
     */
    @PostConstruct
    public void postConstruct()
    {
        setBounds(100, 100, 1280, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));
        add(mSpectralDisplayPanel, "grow");
        /* Register a shutdown listener */
        this.addWindowListener(this);
    }

    /**
     * Shows the specified tuner in this spectrum frame.
     * @param tuner to show.
     */
    public void setTuner(Tuner tuner)
    {
        EventQueue.invokeLater(() -> {
            setTitle("SDRTRunk [" + tuner.getPreferredName() + "]");
            mSpectralDisplayPanel.showTuner(tuner);
            setVisible(true);
        });
    }

    @Override
    public void windowClosed(WindowEvent arg0)
    {
        mSpectralDisplayPanel.dispose();
    }

    @Override
    public void windowActivated(WindowEvent arg0)
    {
    }

    @Override
    public void windowClosing(WindowEvent arg0)
    {
    }

    @Override
    public void windowDeactivated(WindowEvent arg0)
    {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0)
    {
    }

    @Override
    public void windowIconified(WindowEvent arg0)
    {
    }

    @Override
    public void windowOpened(WindowEvent arg0)
    {
    }
}
