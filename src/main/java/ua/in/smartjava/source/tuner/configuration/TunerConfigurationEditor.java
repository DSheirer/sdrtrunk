package ua.in.smartjava.source.tuner.configuration;

import ua.in.smartjava.gui.editor.Editor;

public abstract class TunerConfigurationEditor extends Editor<TunerConfiguration>
{
	private static final long serialVersionUID = 1L;
	
	private TunerConfigurationModel mTunerConfigurationModel;
	
	public TunerConfigurationEditor( TunerConfigurationModel model )
	{
		mTunerConfigurationModel = model;
	}
	
	public TunerConfigurationModel getTunerConfigurationModel()
	{
		return mTunerConfigurationModel;
	}
}
