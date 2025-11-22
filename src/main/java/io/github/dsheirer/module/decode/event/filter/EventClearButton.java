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

package io.github.dsheirer.module.decode.event.filter;

import com.jidesoft.swing.JideSplitButton;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public class EventClearButton extends JideSplitButton
    {
        private static final long serialVersionUID = 1L;
        private EventClearHandler mEventClearHandler;

        public EventClearButton(int maxHistoryCount)
        {
            super("Clear");

            JPanel historyPanel = new JPanel();

            historyPanel.add(new JLabel("History Size:"));
            final JSlider historySlider = initializeHistorySlider();
            JLabel valueLabel = new JLabel(String.valueOf(maxHistoryCount));

            historySlider.setValue(maxHistoryCount);
            historySlider.addChangeListener(arg0 -> {
                if (mEventClearHandler != null)
                {
                    mEventClearHandler.onHistoryLimitChanged(historySlider.getValue());
                }

                valueLabel.setText(String.valueOf(historySlider.getValue()));
            });

            historyPanel.add(historySlider);
            historyPanel.add(valueLabel);
            add(historyPanel);

            /* This handles the click action on the main button. Clear messages */
            addActionListener(e -> {
                if (mEventClearHandler != null)
                {
                    mEventClearHandler.onClearHistoryClicked();
                }
            });
        }

        public void setEventClearHandler(EventClearHandler eventClearHandler) {
            this.mEventClearHandler = eventClearHandler;
        }

        private JSlider initializeHistorySlider()
        {
            final JSlider slider = new JSlider();
            slider.setOpaque(true);
            // Dark mode colors will be applied by UIManager if enabled
            slider.setMinimum(0);
            slider.setMaximum(2000);
            slider.setMajorTickSpacing(500);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);

            slider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent arg0)
                {
                    if(SwingUtilities.isLeftMouseButton(arg0) && arg0.getClickCount() == 2)
                    {
                        slider.setValue(500); //default
                    }
                }

                public void mouseEntered(MouseEvent arg0)
                {
                }

                public void mouseExited(MouseEvent arg0)
                {
                }

                public void mousePressed(MouseEvent arg0)
                {
                }

                public void mouseReleased(MouseEvent arg0)
                {
                }
            });
            return slider;
        }
    }