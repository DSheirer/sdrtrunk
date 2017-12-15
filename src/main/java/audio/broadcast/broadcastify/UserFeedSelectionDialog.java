/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast.broadcastify;

import com.radioreference.api.soap2.UserFeedBroadcast;
import external.radioreference.RadioReferenceService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.rpc.ServiceException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class UserFeedSelectionDialog extends JDialog implements DocumentListener
{
    private final static Logger mLog = LoggerFactory.getLogger( UserFeedSelectionDialog.class );

    private Listener<UserFeedBroadcast> mListener;
    private JTextField mUserNameText;
    private JTextField mPasswordText;
    private JButton mLookupButton;
    private JList mUserFeedBroadcastList;
    private JButton mOKButton;

    public UserFeedSelectionDialog(Listener<UserFeedBroadcast> listener)
    {
        mListener = listener;
        init();
    }

    private void init()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("","[align right,grow,fill][align left,grow,fill][]","[][][][][grow][]"));

        panel.add(new JLabel("Please enter your credentials"), "align left,span");

        panel.add(new JLabel("User Name:"));
        mUserNameText = new JTextField();
        mUserNameText.getDocument().addDocumentListener(this);
        panel.add(mUserNameText, "wrap");

        panel.add(new JLabel("Password:"));
        mPasswordText = new JTextField();
        mPasswordText.getDocument().addDocumentListener(this);
        panel.add(mPasswordText, "wrap");

        mLookupButton = new JButton("Get Feeds");
        mLookupButton.setEnabled(false);
        mLookupButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mLookupButton.setEnabled(false);

                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            RadioReferenceService service =
                                new RadioReferenceService(mUserNameText.getText(), mPasswordText.getText());

                            UserFeedBroadcast[] feeds = service.getUserFeedBroadcasts();

                            mUserFeedBroadcastList.setListData(feeds);
                        }
                        catch(ServiceException se)
                        {
                            JOptionPane.showMessageDialog(UserFeedSelectionDialog.this,
                                "Unable to create radio reference web service client.  See log file.",
                                "Error", JOptionPane.ERROR_MESSAGE);

                            mLog.error("Error creating the radio reference web service client", se);
                        }
                        catch(RemoteException re)
                        {
                            Throwable cause = re.getCause();

                            if(cause == null)
                            {
                                JOptionPane.showMessageDialog(UserFeedSelectionDialog.this,
                                    re.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            else if(cause instanceof UnknownHostException)
                            {
                                JOptionPane.showMessageDialog(UserFeedSelectionDialog.this,
                                    "Broadcastify server is unavailable", "No Server",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                            else
                            {
                                JOptionPane.showMessageDialog(UserFeedSelectionDialog.this,
                                    "An unknown error has occurred.  Please see log file.", "" +
                                        "Unknown Error", JOptionPane.ERROR_MESSAGE);

                                mLog.error("Error retrieving user broadcast feed configurations from radio reference " +
                                    "service: ", re);
                            }
                        }

                        mLookupButton.setEnabled(true);
                    }
                });
            }
        });
        panel.add(mLookupButton,"grow,span");

        mUserFeedBroadcastList = new JList();
        mUserFeedBroadcastList.setVisibleRowCount(3);
        mUserFeedBroadcastList.setLayoutOrientation(JList.VERTICAL);
        mUserFeedBroadcastList.setCellRenderer(new CellRenderer());
        mUserFeedBroadcastList.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                mOKButton.setEnabled(mUserFeedBroadcastList.getSelectedValue() != null);
            }
        });

        JScrollPane listScrollPane = new JScrollPane(mUserFeedBroadcastList);
        panel.add(listScrollPane, "grow,span");

        mOKButton = new JButton("Ok");
        mOKButton.setEnabled(false);
        mOKButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(mListener != null)
                {
                    mListener.receive((UserFeedBroadcast)mUserFeedBroadcastList.getSelectedValue());
                }

                dispose();
            }
        });
        panel.add(mOKButton,"grow");

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        panel.add(cancelButton, "wrap");

        setTitle("Broadcastify Feed Lookup");
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setSize(300,250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
    }

    private void updateButtons()
    {
        String username = mUserNameText.getText();
        String password = mPasswordText.getText();

        mLookupButton.setEnabled(username != null && !username.isEmpty() && password != null && !password.isEmpty());
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        updateButtons();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        updateButtons();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        updateButtons();
    }

    private class CellRenderer extends JLabel implements ListCellRenderer<UserFeedBroadcast>
    {
        public CellRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends UserFeedBroadcast> list,
                                                      UserFeedBroadcast value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(value.getDescr());

            return this;        }
    }
}
