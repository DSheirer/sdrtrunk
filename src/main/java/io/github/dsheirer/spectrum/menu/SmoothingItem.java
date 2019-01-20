/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter;
import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;

public class SmoothingItem extends JSlider implements ChangeListener
{
    private static final long serialVersionUID = 1L;

    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;

    public SmoothingItem(SpectralDisplayAdjuster adjuster, int defaultValue)
    {
        super(JSlider.HORIZONTAL,
            SmoothingFilter.SMOOTHING_MINIMUM,
            SmoothingFilter.SMOOTHING_MAXIMUM,
            adjuster.getSmoothing());

        mDefaultValue = defaultValue;
        mAdjuster = adjuster;

        setSnapToTicks(true);
        setMajorTickSpacing(6);
        setMinorTickSpacing(2);
        setPaintTicks(true);
        setPaintLabels(true);

        Hashtable<Integer,JLabel> labels = new Hashtable<>();
        labels.put(3, new JLabel("3"));
        labels.put(9, new JLabel("9"));
        labels.put(15, new JLabel("15"));
        labels.put(21, new JLabel("21"));
        labels.put(27, new JLabel("27"));

        setLabelTable(labels);

        addChangeListener(this);

        addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    SmoothingItem.this.setValue(mDefaultValue);
                }
            }

            public void mouseReleased(MouseEvent arg0)
            {
            }

            public void mousePressed(MouseEvent arg0)
            {
            }

            public void mouseExited(MouseEvent arg0)
            {
            }

            public void mouseEntered(MouseEvent arg0)
            {
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent event)
    {
        int value = ((JSlider)event.getSource()).getValue();

        if(value % 2 == 1)
        {
            mAdjuster.setSmoothing(value);
        }
    }
}
