package io.github.dsheirer.module.decode.event.filter;

import com.jidesoft.swing.JideSplitButton;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class EventClearButton extends JideSplitButton
    {
        private static final long serialVersionUID = 1L;
        private EventClearHandler mEventClearHandler;

        public EventClearButton(int maxHistoryCount)
        {
            super("Clear");

            JPanel historyPanel = new JPanel();

            historyPanel.add(new JLabel("History Entries:"));

            final JSlider historySlider = initializeHistorySlider();

            historySlider.setValue(maxHistoryCount);
            historySlider.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent arg0)
                {
                    if (mEventClearHandler != null)
                    {
                        mEventClearHandler.onHistoryLimitChanged(historySlider.getValue());
                    }
                }
            });

            historyPanel.add(historySlider);

            add(historyPanel);

            /* This handles the click action on the main button. Clear messages */
            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (mEventClearHandler != null)
                    {
                        mEventClearHandler.onClearHistoryClicked();
                    }
                }
            });
        }

        public void setEventClearHandler(EventClearHandler eventClearHandler) {
            this.mEventClearHandler = eventClearHandler;
        }

        private JSlider initializeHistorySlider()
        {
            final JSlider slider = new JSlider();
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