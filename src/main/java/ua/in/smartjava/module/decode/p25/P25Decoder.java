package ua.in.smartjava.module.decode.p25;

import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.instrument.Instrumentable;
import ua.in.smartjava.module.decode.Decoder;
import ua.in.smartjava.module.decode.DecoderType;
import ua.in.smartjava.source.tuner.frequency.IFrequencyChangeListener;
import ua.in.smartjava.source.tuner.frequency.IFrequencyChangeProvider;

public abstract class P25Decoder extends Decoder implements IFrequencyChangeListener, IFrequencyChangeProvider, Instrumentable
{
	private P25MessageProcessor mMessageProcessor;
	
	public P25Decoder( AliasList aliasList )
	{
        mMessageProcessor = new P25MessageProcessor( aliasList );
        mMessageProcessor.setMessageListener( this );
	}
	
	public void dispose()
	{
		super.dispose();
		
		mMessageProcessor.dispose();
		mMessageProcessor = null;
	}

	@Override
    public DecoderType getDecoderType()
    {
	    return DecoderType.P25_PHASE1;
    }

	protected P25MessageProcessor getMessageProcessor()
	{
		return mMessageProcessor;
	}
	
	public abstract Modulation getModulation();
	
	public enum Modulation
	{ 
		CQPSK( "Simulcast (LSM)", "LSM" ),
		C4FM( "Normal (C4FM)", "C4FM" );
		
		private String mLabel;
		private String mShortLabel;
		
		private Modulation( String label, String shortLabel )
		{
			mLabel = label;
			mShortLabel = shortLabel;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String getShortLabel()
		{
			return mShortLabel;
		}
		
		public String toString()
		{
			return getLabel();
		}
	}
}
