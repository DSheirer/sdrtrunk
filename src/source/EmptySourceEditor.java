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
package source;

import javax.swing.JLabel;

import controller.channel.Channel;
import controller.channel.ConfigurationValidationException;
import source.config.SourceConfiguration;

public class EmptySourceEditor extends SourceEditor
{
    private static final long serialVersionUID = 1L;

	public EmptySourceEditor( SourceManager resourceManager, 
							  SourceConfiguration config )
	{
		super( resourceManager, config );
		
		initGUI();
	}
	
	public void reset() {}
	
	public void save()	{}
	
	private void initGUI()
	{
		JLabel selectLabel = new JLabel( "Please select a source" );
		add( selectLabel );
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
