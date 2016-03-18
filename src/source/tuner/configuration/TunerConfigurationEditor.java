package source.tuner.configuration;

import gui.editor.Editor;
import source.tuner.TunerType;

public abstract class TunerConfigurationEditor extends Editor<TunerConfiguration>
{
	private static final long serialVersionUID = 1L;
	
	private TunerConfigurationModel mTunerConfigurationModel;
	
	public TunerConfigurationEditor( TunerConfigurationModel model )
	{
		mTunerConfigurationModel = model;
	}
	
	public abstract boolean canEdit( TunerType tunerType );
	
	public TunerConfigurationModel getTunerConfigurationModel()
	{
		return mTunerConfigurationModel;
	}
}
