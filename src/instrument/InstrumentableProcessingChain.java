package instrument;

import instrument.tap.Tap;
import instrument.tap.TapGroup;

import java.util.ArrayList;
import java.util.List;

import module.Module;
import module.ProcessingChain;
import source.Source;
import controller.channel.Channel.ChannelType;

public class InstrumentableProcessingChain extends ProcessingChain implements Instrumentable
{
	public InstrumentableProcessingChain()
	{
		super( ChannelType.STANDARD );
	}

	public void setSource( Source source ) throws IllegalStateException
	{
		mSource = source;
	}

	@Override
	public List<TapGroup> getTapGroups()
	{
		List<TapGroup> groups = new ArrayList<>();
		
		for( Module module: getModules() )
		{
			if( module instanceof Instrumentable )
			{
				groups.addAll( ((Instrumentable)module).getTapGroups() );
			}
		}
		
		return groups;
	}

	@Override
	public void registerTap( Tap tap )
	{
		for( Module module: getModules() )
		{
			if( module instanceof Instrumentable )
			{
				((Instrumentable)module).registerTap( tap );
			}
		}
	}

	@Override
	public void unregisterTap( Tap tap )
	{
		for( Module module: getModules() )
		{
			if( module instanceof Instrumentable )
			{
				((Instrumentable)module).unregisterTap( tap );
			}
		}
	}
}
