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

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class BroadcastifySelectionDialog extends JFrame
{

    public BroadcastifySelectionDialog()
    {
        setTitle("Broadcastify Channels");
        setSize(new Dimension(400, 400));
        add(new SelectionPanel());
    }

    public class SelectionPanel extends JPanel
    {
        private JTextField mUserName;
        private JTextField mPassword;
        private JButton mConnectButton;

        public SelectionPanel()
        {
            init();
        }

        private void init()
        {
            setLayout( new MigLayout( "fill,wrap 2", "[align right][grow,fill]", "[][][][][][grow,fill]" ) );

            add(new JLabel("Enter your credentials and click 'Connect'"), "span, align center");

            add(new JLabel("User Name:"));

            mUserName = new JTextField();
            add(mUserName);

            add(new JLabel("Password:"));

            mPassword = new JTextField();
            add(mPassword);

            mConnectButton = new JButton("Connect");
            add(mConnectButton,"span, align center");

            add(new JSeparator(JSeparator.HORIZONTAL), "span,growx");

        }
    }

    public static void main(String[] args)
    {
        BroadcastifySelectionDialog dialog = new BroadcastifySelectionDialog();

        try
        {
            EventQueue.invokeAndWait(new Runnable()
            {
                @Override
                public void run()
                {

                    dialog.setVisible(true);
                    dialog.setLocationRelativeTo(null);
                }
            });
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
