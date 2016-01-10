/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package record;

import record.config.RecordConfiguration;
import controller.channel.Channel;
import controller.channel.ChannelConfigurationEditor;
import controller.channel.ConfigurationValidationException;

public class RecordEditor extends ChannelConfigurationEditor
{
    private static final long serialVersionUID = 1L;
    
    protected RecordConfiguration mConfig;
    
	public RecordEditor( RecordConfiguration config )
	{
		mConfig = config;
	}
	
	public RecordConfiguration getConfig()
	{
		return mConfig;
	}

	@Override
    public void save()
    {
    }

	@Override
	public void setConfiguration( Channel channel )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		// TODO Auto-generated method stub
		
	}
}
