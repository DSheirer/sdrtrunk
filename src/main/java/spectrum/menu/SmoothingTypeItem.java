package spectrum.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import spectrum.SpectralDisplayAdjuster;
import dsp.filter.smoothing.SmoothingFilter.SmoothingType;

public class SmoothingTypeItem extends JCheckBoxMenuItem
{
	private static final long serialVersionUID = 1L;
	
	private SpectralDisplayAdjuster mAdjuster;
	private SmoothingType mSmoothingType;
	
	public SmoothingTypeItem( SpectralDisplayAdjuster adjuster, SmoothingType type )
	{
		super( type.name() );
		
		mAdjuster = adjuster;
		mSmoothingType = type;
	
		setSelected( mAdjuster.getSmoothingType() == type );

		addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				mAdjuster.setSmoothingType( mSmoothingType );
			}
		} );
	}
}
