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

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import log.Log;
import source.config.SourceConfigMixer;
import source.config.SourceConfiguration;
import source.tuner.MixerTunerDataLine;
import source.tuner.MixerTunerType;
import controller.channel.ProcessingChain;

public class MixerManager
{
	private static MixerManager sInstance = null;
	
	private HashMap<String,TargetDataLine> mMixerLines = 
								new HashMap<String,TargetDataLine>();
	
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
    
    public MixerSource getSource( ProcessingChain channel )
    {
		MixerSource retVal = null;
		
		SourceConfiguration config = 
				channel.getChannel().getSourceConfiguration();

    	if( config instanceof SourceConfigMixer )
    	{
			SourceConfigMixer mixerConfig = (SourceConfigMixer)config;

			//Get the name of the requested mixer
			String mixerName = mixerConfig.getMixer();

			if( mixerName != null )
			{
				TargetDataLine tdl = mMixerLines.get( mixerName );

				if( tdl != null )
				{
					switch( mixerConfig.getChannel() )
					{
						case MONO:
				    		retVal = new MixerSource( mMixerLines.get( mixerName ),
				    				 MONO,
									 mixerName,
									 new ShortAdapter() );
							break;
						case LEFT:
						case RIGHT:
				    		retVal = new MixerSource( mMixerLines.get( mixerName ),
				    				 STEREO,
									 mixerName,
									 new ChannelShortAdapter( mixerConfig.getChannel() ) );
							break;
					}
				}
			}
    	}

    	return retVal;
    }
    
    public Set<String> getMixers()
    {
    	return mMixerLines.keySet();
    }
    
    public Collection<MixerTunerDataLine> getMixerTunerDataLines()
    {
    	return mMixerTuners.values();
    }
    
	private void loadMixers()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "MixerManager - loading system mixer devices\n" );
		
        for( Mixer.Info  mixerInfo: AudioSystem.getMixerInfo() )
        {
        	//Sort between the mixers and the tuner mixers, and load each
        	MixerTunerType mixerTunerType = MixerTunerType.getMixerTunerType( mixerInfo );
        	
        	if( mixerTunerType == MixerTunerType.UNKNOWN )
        	{
                Mixer mixer = AudioSystem.getMixer( mixerInfo );

                if( mixer != null )
                {
                	TargetDataLine tdl = getTargetDataLine( mixerInfo, STEREO );

                	if( tdl != null )
                	{
                        mMixerLines.put( mixerInfo.getName(), tdl );

                        sb.append( "\t[LOADED]     Mixer:" + mixerInfo.getName() + "\n"  );
                	}
                	else
                	{
                        sb.append( "\t[NOT LOADED] Mixer:" + mixerInfo.getName() + " - audio format not supported\n" );
                	}
                }
                else
                {
                    sb.append( "\t[NOT LOADED] Mixer:" + mixerInfo.getName() + " - couldn't get mixer\n" );
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
        
		Log.info( sb.toString() );
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

}
