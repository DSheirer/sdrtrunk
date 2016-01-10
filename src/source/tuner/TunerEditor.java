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
package source.tuner;

import controller.channel.Channel;
import controller.channel.ConfigurationValidationException;
import gui.control.JFrequencyControl;
import source.SourceEditor;
import source.SourceManager;
import source.config.SourceConfigTuner;
import source.config.SourceConfiguration;

public class TunerEditor extends SourceEditor
{
    private static final long serialVersionUID = 1L;
    private JFrequencyControl mFrequencyControl;
    
	public TunerEditor( SourceManager sourceManager, 
						SourceConfiguration config )
	{
		super( sourceManager, config );
		
		initGUI();
	}

	public void reset()
	{
		mFrequencyControl.setFrequency( 
				((SourceConfigTuner)mConfig).getFrequency(), false );
	}
	
	public void save()
	{
		((SourceConfigTuner)mConfig).setFrequency( mFrequencyControl.getFrequency() );
	}
	
	private void initGUI()
	{
		mFrequencyControl = new JFrequencyControl();
		
		mFrequencyControl.setFrequency( 
				((SourceConfigTuner)mConfig).getFrequency(), false );
		
		add( mFrequencyControl );
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
