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
package io.github.dsheirer.audio.broadcast.broadcastify;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.AuthorizationInformation;
import io.github.dsheirer.rrapi.type.UserFeedBroadcast;
import io.github.dsheirer.rrapi.type.UserInfo;
import io.github.dsheirer.sample.Listener;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class UserFeedSelectionDialog extends JDialog implements DocumentListener
{
    private final static Logger mLog = LoggerFactory.getLogger( UserFeedSelectionDialog.class );

    public static final String SDRTRUNK_APP_KEY = "88969092";

    private Listener<UserFeedBroadcast> mListener;
    private UserPreferences mUserPreferences;
    private JTextField mUserNameText;
    private JTextField mPasswordText;
    private JCheckBox mSaveCredentialsCheckBox;
    private JButton mClearStoredCredentialsButton;
    private JButton mLookupButton;
    private JList mUserFeedBroadcastList;
    private JButton mOKButton;

    public UserFeedSelectionDialog(UserPreferences userPreferences, Listener<UserFeedBroadcast> listener)
    {
        mUserPreferences = userPreferences;
        mListener = listener;

        //Register to receive radio reference preferences updates
        MyEventBus.getEventBus().register(this);

        init();
    }

    private void init()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("","[align right,grow,fill][align left,grow,fill][]","[][][][][][grow][]"));

        panel.add(new JLabel("Please enter your credentials"), "align left,span");

        panel.add(new JLabel("User Name:"));
        mUserNameText = new JTextField();
        String storedUserName = mUserPreferences.getRadioReferencePreference().getUserName();
        if(storedUserName != null)
        {
            mUserNameText.setText(storedUserName);
        }
        mUserNameText.getDocument().addDocumentListener(this);
        panel.add(mUserNameText, "wrap");

        panel.add(new JLabel("Password:"));
        mPasswordText = new JTextField();
        String storedPassword = mUserPreferences.getRadioReferencePreference().getPassword();
        if(storedPassword != null)
        {
            mPasswordText.setText(storedPassword);
        }
        mPasswordText.getDocument().addDocumentListener(this);
        panel.add(mPasswordText, "wrap");

        mSaveCredentialsCheckBox = new JCheckBox("Store Credentials");
        mSaveCredentialsCheckBox.setSelected(mUserPreferences.getRadioReferencePreference().isStoreCredentials());
        mSaveCredentialsCheckBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //If user unchecks box and there are stored credentials, prompt to clear them
                if(!mSaveCredentialsCheckBox.isSelected() && mUserPreferences.getRadioReferencePreference().hasStoredCredentials())
                {
                    int choice = JOptionPane.showOptionDialog(UserFeedSelectionDialog.this,
                        "Would you like to remove previously stored credentials from this computer?",
                        "Clear Stored Credentials?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, null, 0);

                    if(choice == JOptionPane.YES_OPTION)
                    {
                        mUserPreferences.getRadioReferencePreference().removeStoreCredentials();
                    }
                }
            }
        });
        panel.add(mSaveCredentialsCheckBox);

        mClearStoredCredentialsButton = new JButton("Remove");
        mClearStoredCredentialsButton.setToolTipText("Clear stored credentials");
        mClearStoredCredentialsButton.setEnabled(mUserPreferences.getRadioReferencePreference().hasStoredCredentials());
        mClearStoredCredentialsButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mUserPreferences.getRadioReferencePreference().removeStoreCredentials();
            }
        });
        panel.add(mClearStoredCredentialsButton, "wrap");

        mLookupButton = new JButton("Get Feeds");
        mLookupButton.setEnabled(storedPassword != null && storedPassword != null);
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
                            AuthorizationInformation authorizationInformation = new AuthorizationInformation(SDRTRUNK_APP_KEY,
                                mUserNameText.getText(), mPasswordText.getText());

                            mLog.debug("User:" + authorizationInformation.getUserName() + " Pass:" + authorizationInformation.getPassword() + " Key:" + authorizationInformation.getApplicationKey());

                            RadioReferenceService service = new RadioReferenceService(authorizationInformation);
                            UserInfo userInfo = service.getUserInfo();
                            mLog.info("RR User:" + userInfo.getUserName() + " Expiration Date:" + userInfo.getExpirationDate());
                            List<UserFeedBroadcast> feeds = service.getUserFeeds();

                            mUserFeedBroadcastList.setListData(feeds.toArray(new UserFeedBroadcast[0]));
                        }
                        catch(RadioReferenceException rre)
                        {
                            JOptionPane.showMessageDialog(UserFeedSelectionDialog.this,
                                "Unable to create radio reference web service client.  See log file.",
                                "Error", JOptionPane.ERROR_MESSAGE);

                            mLog.error("Error creating the radio reference web service client", rre);
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
                    storeCredentials();
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
        setSize(400,450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
    }

    /**
     * Stores login credentials in the user preferences
     */
    private void storeCredentials()
    {
        boolean store = mSaveCredentialsCheckBox.isSelected();
        mUserPreferences.getRadioReferencePreference().setStoreCredentials(store);

        if(store)
        {
            String userName = mUserNameText.getText();

            if(userName != null)
            {
                mUserPreferences.getRadioReferencePreference().setUserName(userName);
            }

            String password = mPasswordText.getText();

            if(password != null)
            {
                mUserPreferences.getRadioReferencePreference().setPassword(password);
            }
        }
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

            setText(value.getDescription());

            return this;
        }
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.RADIO_REFERENCE)
        {
            //Update the store-credentials checkbox only, so we don't change the username or password currently displayed
            mSaveCredentialsCheckBox.setSelected(mUserPreferences.getRadioReferencePreference().isStoreCredentials());
            mClearStoredCredentialsButton.setEnabled(mUserPreferences.getRadioReferencePreference().hasStoredCredentials());
        }
    }
}
