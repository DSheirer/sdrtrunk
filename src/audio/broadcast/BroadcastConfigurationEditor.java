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
package audio.broadcast;

import alias.AliasModel;
import gui.editor.DocumentListenerEditor;
import icon.IconManager;

import javax.swing.*;

public abstract class BroadcastConfigurationEditor extends DocumentListenerEditor<BroadcastConfiguration>
{
    protected IconManager mIconManager;
    protected BroadcastModel mBroadcastModel;
    protected AliasModel mAliasModel;

    public BroadcastConfigurationEditor(BroadcastModel broadcastModel, AliasModel aliasModel, IconManager iconManager)
    {
        mBroadcastModel = broadcastModel;
        mAliasModel = aliasModel;
        mIconManager = iconManager;
    }

    /**
     * Updates the configuration with the new name and prompts the user to update any aliases that have a broadcast
     * channel alias id with the old name to update to the new name.
     *
     * @param broadcastConfiguration to assign a new channel name
     * @param newName to assign to the broadcast configuration.  Note: assumes that validateConfiguration() has been
     * invoked to verify that newName is non-null and non-empty
     */
    protected void updateConfigurationName(BroadcastConfiguration broadcastConfiguration, String newName)
    {
        String previousName = broadcastConfiguration.getName();

        if(previousName == null || previousName.isEmpty())
        {
            broadcastConfiguration.setName(newName);
            return;
        }

        if(!previousName.equals(newName))
        {
            mAliasModel.renameBroadcastChannel(previousName, newName);
            broadcastConfiguration.setName(newName);
        }
    }

    /**
     * Validates a text field control for a non-null, non-empty value
     * @param field to validate
     * @param title to use for error dialog
     * @param message to use for error dialog
     * @return true if field contains a non-null, non-empty value
     */
    protected boolean validateTextField(JTextField field, String title, String message)
    {
        String text = field.getText();

        if(text == null || text.isEmpty())
        {
            JOptionPane.showMessageDialog(BroadcastConfigurationEditor.this, message, title,
                    JOptionPane.ERROR_MESSAGE);

            field.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Validates the text field control that contains an integer value for non-null, non-empty and within the
     * specified min/max valid range.
     * @param field to validate
     * @param title to use for error dialog
     * @param message to use for error dialog
     * @param minValid value
     * @param maxValid value
     * @return true if field contains a non-null, non-empty value within the valid min/max range
     */
    protected boolean validateIntegerTextField(JTextField field, String title, String message, int minValid, int maxValid)
    {
        if(validateTextField(field, title, message))
        {
            String text = field.getText();

            try
            {
                int value = Integer.parseInt(text);

                if(minValid <= value && value <= maxValid)
                {
                    return true;
                }
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }

            JOptionPane.showMessageDialog(BroadcastConfigurationEditor.this, message, title,
                    JOptionPane.ERROR_MESSAGE);

            field.requestFocus();
        }

        return false;
    }
}
