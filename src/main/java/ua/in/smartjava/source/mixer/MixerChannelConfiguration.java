package ua.in.smartjava.source.mixer;

import javax.sound.sampled.Mixer;

public class MixerChannelConfiguration
{
	private Mixer mMixer;
	private MixerChannel mMixerChannel;
	
	public MixerChannelConfiguration( Mixer mixer, MixerChannel channel )
	{
		mMixer = mixer;
		mMixerChannel = channel;
	}
	
	public Mixer getMixer()
	{
		return mMixer;
	}
	
	public MixerChannel getMixerChannel()
	{
		return mMixerChannel;
	}
	
	public boolean matches( String mixer, String channels )
	{
		return mixer != null && 
			   channels != null &&
			   mMixer.getMixerInfo().getName().contentEquals( mixer ) &&
			   mMixerChannel.name().contentEquals( channels );
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMixer.getMixerInfo().getName() );
		sb.append( " - " );
		sb.append( mMixerChannel.name() );
		
		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( mMixer == null ) ? 0 : mMixer.hashCode() );
		result = prime * result
				+ ( ( mMixerChannel == null ) ? 0 : mMixerChannel.hashCode() );
		return result;
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		MixerChannelConfiguration other = (MixerChannelConfiguration) obj;
		if ( mMixer == null )
		{
			if ( other.mMixer != null )
				return false;
		} else if ( !mMixer.equals( other.mMixer ) )
			return false;
		if ( mMixerChannel != other.mMixerChannel )
			return false;
		return true;
	}
}
