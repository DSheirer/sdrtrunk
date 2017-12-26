package io.github.dsheirer.source.tuner.configuration;

import io.github.dsheirer.gui.editor.Editor;

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
