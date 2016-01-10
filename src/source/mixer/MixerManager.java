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
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.adapter.ChannelShortAdapter;
import sample.adapter.ShortAdapter;
import source.config.SourceConfigMixer;
import source.config.SourceConfiguration;
import source.tuner.MixerTunerDataLine;
import source.tuner.MixerTunerType;
import audio.AudioFormats;

public class MixerManager
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( MixerManager.class );

	private List<InputMixerConfiguration> mInputMixers = new ArrayList<>();
	private List<MixerChannelConfiguration> mOutputMixers = new ArrayList<>();
	private HashMap<String,MixerTunerDataLine> mMixerTuners = new HashMap<>();

	public MixerManager()
	{
		loadMixers();
	}

    public RealMixerSource getSource( SourceConfiguration config )
    {
		RealMixerSource retVal = null;
		
    	if( config instanceof SourceConfigMixer )
    	{
			SourceConfigMixer mixerConfig = (SourceConfigMixer)config;

			String mixerName = mixerConfig.getMixer();

			if( mixerName != null )
			{
				InputMixerConfiguration mixer = getInputMixer( mixerName );

				if( mixer != null )
				{
					MixerChannel channel = mixerConfig.getChannel();
					
					if( mixer.supportsChannel( channel ) )
					{
						
						if( channel == MixerChannel.MONO )
						{
							DataLine.Info info = 
								new DataLine.Info(TargetDataLine.class, 
									AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO );

							TargetDataLine dataLine;
							
                            try
                            {
	                            dataLine = (TargetDataLine)mixer
	                            		.getMixer().getLine( info );

	                            if( dataLine != null )
	                            {
									return new RealMixerSource( dataLine,
						    				 AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO,
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
								new DataLine.Info(TargetDataLine.class, 
									AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO );

							TargetDataLine dataLine;

							try
                            {
	                            dataLine = (TargetDataLine)mixer
	                            		.getMixer().getLine( info );

	                            if( dataLine != null )
								{
									return new RealMixerSource( dataLine, 
										AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO,
										new ChannelShortAdapter( 
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
    
    public InputMixerConfiguration[] getInputMixers()
    {
    	return mInputMixers.toArray( new InputMixerConfiguration[ mInputMixers.size() ] );
    }
    
    public InputMixerConfiguration getInputMixer( String name )
    {
    	for( InputMixerConfiguration mixer: mInputMixers )
    	{
    		if( mixer.getMixerName().contentEquals( name ) )
    		{
    			return mixer;
    		}
    	}
    	
    	return null;
    }

    public MixerChannelConfiguration[] getOutputMixers()
    {
    	return mOutputMixers.toArray( new MixerChannelConfiguration[ mOutputMixers.size() ] );
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
                	EnumSet<MixerChannel> inputChannels = getSupportedTargetChannels( mixer );
                	
                	if( inputChannels != null )
                	{
                		mInputMixers.add( 
                				new InputMixerConfiguration( mixer, inputChannels ) );
                		
                        sb.append( "\t[LOADED]     Input:  " + mixerInfo.getName() + 
                        		" CHANNELS: " + inputChannels + "\n"  );
                	}
                	
                	EnumSet<MixerChannel> outputChannels = getSupportedSourceChannels( mixer );
                	
                	if( outputChannels != null )
                	{
                    	for( MixerChannel channel: outputChannels )
                    	{
                    		mOutputMixers.add( new MixerChannelConfiguration( mixer, channel ) );
                    	}

                    	sb.append( "\t[LOADED]     Output: " + mixerInfo.getName() + 
                        		" CHANNELS: " + outputChannels + "\n"  );
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
	
	private EnumSet<MixerChannel> getSupportedTargetChannels( Mixer mixer )
	{
        DataLine.Info stereoInfo=
        		new DataLine.Info(TargetDataLine.class, 
        				AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO );

        boolean stereoSupported = mixer.isLineSupported( stereoInfo );
        
        DataLine.Info monoInfo=
        		new DataLine.Info(TargetDataLine.class, 
        				AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO );

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

	/**
	 * Returns enumset of SourceDataLine (audio output) channels 
	 * (MONO and/or STEREO) supported by the mixer, or null if the mixer doesn't 
	 * have any source data lines.
	 */
	private EnumSet<MixerChannel> getSupportedSourceChannels( Mixer mixer )
	{
        DataLine.Info stereoInfo=
        		new DataLine.Info(SourceDataLine.class, 
        				AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO );

        boolean stereoSupported = mixer.isLineSupported( stereoInfo );
        
        DataLine.Info monoInfo=
        		new DataLine.Info(SourceDataLine.class, 
        				AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO );

        boolean monoSupported = mixer.isLineSupported( monoInfo );
        
        if( stereoSupported && monoSupported )
        {
        	return EnumSet.of( MixerChannel.MONO,
        					   MixerChannel.STEREO );
        }
        else if( stereoSupported )
        {
        	return EnumSet.of( MixerChannel.STEREO );
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
			
			Line.Info portInfo = new Line.Info( Port.class );
			
			if( mixer.isLineSupported( portInfo ) )
			{
				sb.append( "**PORT LINE IS SUPPORTED BY THIS MIXER***\n" );
			}
		}
		
		return sb.toString();
	}
	
	public class InputMixerConfiguration
	{
		private Mixer mMixer;
		private EnumSet<MixerChannel> mChannels;
		
		public InputMixerConfiguration( Mixer mixer, EnumSet<MixerChannel> channels )
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
			return mMixer.getMixerInfo().getName();
		}
	}
}
