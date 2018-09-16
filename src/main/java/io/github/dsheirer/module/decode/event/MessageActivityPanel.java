/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.event;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MessageActivityPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);

    private static MessageActivityModel EMPTY_MODEL = new MessageActivityModel();

    private JTable mTable = new JTable(EMPTY_MODEL);

    private MessageManagementPanel mManagementPanel = new MessageManagementPanel();

    private ChannelProcessingManager mChannelProcessingManager;

    public MessageActivityPanel(ChannelProcessingManager channelProcessingManager)
    {
        mChannelProcessingManager = channelProcessingManager;

        setLayout(new MigLayout("insets 0 0 0 0", "[][grow,fill]", "[]0[grow,fill]"));

        add(mManagementPanel, "span,growx");

        add(new JScrollPane(mTable), "span,grow");
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mTable.setModel(processingChain != null ? processingChain.getMessageActivityModel() : EMPTY_MODEL);

                if(processingChain != null)
                {
                    mManagementPanel.enableButtons();
                }
                else
                {
                    mManagementPanel.disableButtons();
                }
            }
        });
    }

    public class MessageManagementPanel extends JPanel
    {
        private static final long serialVersionUID = 1L;

        private MessageHistoryButton mHistoryButton = new MessageHistoryButton();
        private MessageFilterButton mFilterButton = new MessageFilterButton();

        public MessageManagementPanel()
        {
            setLayout(new MigLayout("insets 2 2 5 5", "[]5[left,grow]", ""));

            disableButtons();

            add(mFilterButton);
            add(mHistoryButton);
        }

        public void enableButtons()
        {
            mHistoryButton.setEnabled(true);
            mFilterButton.setEnabled(true);
        }

        public void disableButtons()
        {
            mHistoryButton.setEnabled(false);
            mFilterButton.setEnabled(false);
        }
    }

    public class MessageHistoryButton extends JideSplitButton
    {
        private static final long serialVersionUID = 1L;

        public MessageHistoryButton()
        {
            super("Clear");

            JPanel historyPanel = new JPanel();

            historyPanel.add(new JLabel("Message History:"));

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

            slider.setValue(((MessageActivityModel)mTable.getModel()).getMaxMessageCount());
            slider.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent arg0)
                {
                    ((MessageActivityModel)mTable.getModel()).setMaxMessageCount(slider.getValue());
                }
            });

            historyPanel.add(slider);

            add(historyPanel);

        	/* Clear messages */
            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ((MessageActivityModel)mTable.getModel()).clear();
                }
            });
        }
    }

    public class MessageFilterButton extends JideButton
    {
        private static final long serialVersionUID = 1L;

        public MessageFilterButton()
        {
            super("Filter");

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    final JFrame editor = new JFrame();

                    editor.setTitle("Message Filter Editor");
                    editor.setSize(600, 400);
                    editor.setLocationRelativeTo(MessageFilterButton.this);
                    editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    editor.setLayout(new MigLayout("", "[grow,fill]",
                        "[grow,fill][][]"));

                    @SuppressWarnings("unchecked")
                    FilterSet<Message> filter = (FilterSet<Message>) ((MessageActivityModel)mTable.getModel()).getMessageFilter();

                    FilterEditorPanel<Message> panel = new FilterEditorPanel<Message>(filter);

                    JScrollPane scroller = new JScrollPane(panel);
                    scroller.setViewportView(panel);

                    editor.add(scroller, "wrap");

                    editor.add(new JLabel("Right-click to select/deselect all nodes"), "wrap");

                    JButton close = new JButton("Close");
                    close.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            editor.dispose();
                        }
                    });

                    editor.add(close);

                    EventQueue.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            editor.setVisible(true);
                        }
                    });
                }
            });
        }
    }
}
