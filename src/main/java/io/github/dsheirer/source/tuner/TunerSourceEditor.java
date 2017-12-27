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
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.control.JFrequencyControl;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfiguration;
import net.miginfocom.swing.MigLayout;

public class TunerSourceEditor extends Editor<Channel>
{
	private static final long serialVersionUID = 1L;
	private JFrequencyControl mFrequencyControl;

	public TunerSourceEditor()
	{
		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[left]", "" ) );
		mFrequencyControl = new JFrequencyControl();
		mFrequencyControl.setEnabled( false );
		mFrequencyControl.addListener( new ISourceEventProcessor()
		{
			@Override
			public void process(SourceEvent event )
			{
				setModified( true );
			}
		} );
		add( mFrequencyControl );
	}

	public void save()
	{
		if( hasItem() && isModified() )
		{
			SourceConfigTuner config = new SourceConfigTuner();
			config.setFrequency( mFrequencyControl.getFrequency() );
			getItem().setSourceConfiguration( config );
		}

		setModified( false );
	}

	@Override
	public void setItem( Channel item )
	{
		super.setItem( item );

		if( hasItem() )
		{
			SourceConfiguration config = getItem().getSourceConfiguration();

			mFrequencyControl.setEnabled( true );

			if( config instanceof SourceConfigTuner )
			{
				mFrequencyControl.setFrequency( ((SourceConfigTuner)config).getFrequency(), false );
				setModified( false );
			}
			else
			{
				mFrequencyControl.setFrequency( 101000000, false );
				setModified( true );
			}
		}
		else
		{
			mFrequencyControl.setEnabled( false );
			setModified( false );
		}
	}
}