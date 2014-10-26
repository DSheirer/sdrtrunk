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
package source.mixer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.adapter.ChannelShortAdapter;
import sample.adapter.ShortAdapter;
import source.config.SourceConfigMixer;
import source.config.SourceConfiguration;
import source.tuner.MixerTunerDataLine;
import source.tuner.MixerTunerType;
import controller.channel.ProcessingChain;

public class MixerManager
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( MixerManager.class );

	private static MixerManager sInstance = null;
	
	private ArrayList<DiscoveredMixer> mDiscoveredMixers = 
				new ArrayList<DiscoveredMixer>();
	
	private HashMap<String,MixerTunerDataLine> mMixerTuners = 
						new HashMap<String,MixerTunerDataLine>();

    public static AudioFormat MONO = new AudioFormat( 48000,  //SampleRate
    												  16,     //Sample Size
    												  1,      //Channels
    												  true,   //Signed
    												  false ); //Little Endian

    public static AudioFormat STEREO = new AudioFormat( 48000,  //SampleRate
													  16,     //Sample Size
													  2,      //Channels
													  true,   //Signed
													  false ); //Little Endian

    private MixerManager()
	{
		loadMixers();
	}

    public static MixerManager getInstance()
    {
    	if( sInstance == null )
    	{
    		sInstance = new MixerManager();
    	}
    	
    	return sInstance;
    }
    
    public SimplexMixerSource getSource( ProcessingChain processingChain )
    {
		SimplexMixerSource retVal = null;
		
		SourceConfiguration config = 
				processingChain.getChannel().getSourceConfiguration();

    	if( config instanceof SourceConfigMixer )
    	{
			SourceConfigMixer mixerConfig = (SourceConfigMixer)config;

			String mixerName = mixerConfig.getMixer();

			if( mixerName != null )
			{
				DiscoveredMixer mixer = getDiscoveredMixer( mixerName );

				if( mixer != null )
				{
					MixerChannel channel = mixerConfig.getChannel();
					
					if( mixer.supportsChannel( channel ) )
					{
						
						if( channel == MixerChannel.MONO )
						{
							DataLine.Info info = 
								new DataLine.Info(TargetDataLine.class, MONO );

							TargetDataLine dataLine;
							
                            try
                            {
	                            dataLine = (TargetDataLine)mixer
	                            		.getMixer().getLine( info );

	                            if( dataLine != null )
	                            {
									return new SimplexMixerSource( dataLine,
						    				 MONO,
											 mixerName,
											 new ShortAdapter() );
	                            }
                            }
                            catch ( LineUnavailableException e )
                            {
	                            mLog.error( "couldn't get mixer data line " +
	                            		"for [" + mixerName + "] for channel [" + 
	                            		channel.name() + "]", e );
                            }
							
						}
						else
						{
							DataLine.Info info = 
								new DataLine.Info(TargetDataLine.class, STEREO );

							TargetDataLine dataLine;

							try
                            {
	                            dataLine = (TargetDataLine)mixer
	                            		.getMixer().getLine( info );

	                            if( dataLine != null )
								{
									return new SimplexMixerSource( dataLine, STEREO,
										mixerName, new ChannelShortAdapter( 
												mixerConfig.getChannel() ) );
								}
                            }
                            catch ( LineUnavailableException e )
                            {
	                            mLog.error( "couldn't get mixer data line " +
	                            		"for [" + mixerName + "] for channel [" + 
	                            		channel.name() + "]", e );
                            }
						}
					}
				}
			}
    	}

    	return null;
    }
    
    public DiscoveredMixer[] getMixers()
    {
    	return mDiscoveredMixers.toArray( 
    			new DiscoveredMixer[ mDiscoveredMixers.size() ] );
    }
    
    public DiscoveredMixer getDiscoveredMixer( String name )
    {
    	for( DiscoveredMixer mixer: mDiscoveredMixers )
    	{
    		if( mixer.getMixerName().contentEquals( name ) )
    		{
    			return mixer;
    		}
    	}
    	
    	return null;
    }
    
    public Collection<MixerTunerDataLine> getMixerTunerDataLines()
    {
    	return mMixerTuners.values();
    }
    
	private void loadMixers()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "loading system mixer devices\n" );
		
        for( Mixer.Info  mixerInfo: AudioSystem.getMixerInfo() )
        {
        	//Sort between the mixers and the tuner mixers, and load each
        	MixerTunerType mixerTunerType = MixerTunerType.getMixerTunerType( mixerInfo );
        	
        	if( mixerTunerType == MixerTunerType.UNKNOWN )
        	{
                Mixer mixer = AudioSystem.getMixer( mixerInfo );

                if( mixer != null )
                {
                	EnumSet<MixerChannel> channels = getSupportedChannels( mixer );
                	
                	if( channels != null )
                	{
                		mDiscoveredMixers.add( 
                				new DiscoveredMixer( mixer, channels ) );
                		
                        sb.append( "\t[LOADED]     Mixer:" + mixerInfo.getName() + 
                        		" CHANNELS: " + channels + "\n"  );
                	}
                	else
                	{
                        sb.append( "\t[NOT LOADED] Mixer:" + 
		                			mixerInfo.getName() + 
		                			" - audio format not supported\n" );
                	}
                }
                else
                {
                    sb.append( "\t[NOT LOADED] Mixer:" + mixerInfo.getName() + 
                    		" - couldn't get mixer\n" );
                }
        	}
        	else //Process as a Mixer-based tuner data line
        	{
            	TargetDataLine tdl = 
            			getTargetDataLine( mixerInfo, 
            							   mixerTunerType.getAudioFormat() );

            	if( tdl != null )
            	{
            		switch( mixerTunerType )
            		{
            			case FUNCUBE_DONGLE_PRO:
            			case FUNCUBE_DONGLE_PRO_PLUS:
                        	mMixerTuners.put( mixerInfo.getName(), 
            					  new MixerTunerDataLine( tdl, mixerTunerType ) );
                            sb.append( "\t[LOADED]     FunCube Dongle Mixer Tuner:" + mixerInfo.getName() + "[" + mixerTunerType.getDisplayString() + "]\n" );
                        	break;
            			case UNKNOWN:
        				default:
                            sb.append( "\t[NOT LOADED] Tuner:" + mixerInfo.getName() + " - unrecognized tuner type\n" );
        					break;
            		}
            	}
                else
                {
                    sb.append( "\t[NOT LOADED] Tuner:" + mixerInfo.getName() + " - couldn't get target data line\n" );
                }
        	}
        }
        
        mLog.info( sb.toString() );
	}
	
	public HashMap<String,MixerTunerDataLine> getMixerTuners()
	{
		return mMixerTuners;
	}
	
	private TargetDataLine getTargetDataLine( Mixer.Info mixerInfo, AudioFormat format )
	{
		TargetDataLine retVal = null;
		
        Mixer mixer = AudioSystem.getMixer( mixerInfo );

        if( mixer != null )
        {
            try
            {
                DataLine.Info datalineInfo=
                		new DataLine.Info(TargetDataLine.class, format );

                retVal = (TargetDataLine) mixer.getLine( datalineInfo );
            }
            catch( Exception e )
            {
            	//Do nothing ... we couldn't get the TDL
            }
        }
        
        return retVal;
	}
	
	private EnumSet<MixerChannel> getSupportedChannels( Mixer mixer )
	{
        DataLine.Info stereoInfo=
        		new DataLine.Info(TargetDataLine.class, STEREO );

        boolean stereoSupported = mixer.isLineSupported( stereoInfo );
        
        DataLine.Info monoInfo=
        		new DataLine.Info(TargetDataLine.class, MONO );

        boolean monoSupported = mixer.isLineSupported( monoInfo );
        
        if( stereoSupported && monoSupported )
        {
        	return EnumSet.of( MixerChannel.LEFT,
        					   MixerChannel.RIGHT,
        					   MixerChannel.MONO );
        }
        else if( stereoSupported )
        {
        	return EnumSet.of( MixerChannel.LEFT,
					   MixerChannel.RIGHT,
					   MixerChannel.MONO );
        }
        else if( monoSupported )
        {
        	return EnumSet.of( MixerChannel.MONO );
        }
        
        return null;
	}
	
	public static String getMixerDevices()
	{
		StringBuilder sb = new StringBuilder();
		
		for( Mixer.Info mixerInfo: AudioSystem.getMixerInfo() )
		{
			sb.append( "\n--------------------------------------------------" );
			sb.append( "\nMIXER name:" + mixerInfo.getName() + 
					   "\n      desc:" + mixerInfo.getDescription() +
					   "\n      vendor:" + mixerInfo.getVendor() +
					   "\n      version:" + mixerInfo.getVersion() +
					   "\n" );
			
			Mixer mixer = AudioSystem.getMixer( mixerInfo );

			Line.Info[] sourceLines = mixer.getSourceLineInfo();
			
			for( Line.Info lineInfo: sourceLines )
			{
				sb.append( "      SOURCE LINE desc:" + lineInfo.toString() + 
						   "\n               class:" + lineInfo.getClass() +
						   "\n               lineclass:" + lineInfo.getLineClass() +
						   "\n" );
			}

			Line.Info[] targetLines = mixer.getTargetLineInfo();
			
			for( Line.Info lineInfo: targetLines )
			{
				sb.append( "      TARGET LINE desc:" + lineInfo.toString() + 
						   "\n                class:" + lineInfo.getClass() +
						   "\n                lineclass:" + lineInfo.getLineClass() +
						   "\n" );
			}
		}
		
		return sb.toString();
	}
	
	public class DiscoveredMixer
	{
		private Mixer mMixer;
		private EnumSet<MixerChannel> mChannels;
		
		public DiscoveredMixer( Mixer mixer, EnumSet<MixerChannel> channels )
		{
			mMixer = mixer;
			mChannels = channels;
		}
		
		public Mixer getMixer()
		{
			return mMixer;
		}
		
		public String getMixerName()
		{
			return mMixer.getMixerInfo().getName();
		}
		
		public EnumSet<MixerChannel> getChannels()
		{
			return mChannels;
		}
		
		public boolean supportsChannel( MixerChannel channel )
		{
			return mChannels.contains( channel );
		}
		
		public String toString()
		{
			return getMixerName();
		}
	}
}
