package module.decode.p25.audio;

import audio.AudioFormats;
import audio.AudioPacket;
import audio.IAudioPacketProvider;
import audio.squelch.ISquelchStateListener;
import audio.squelch.SquelchState;
import channel.metadata.Metadata;
import dsp.gain.NonClippingGain;
import jmbe.iface.AudioConversionLibrary;
import jmbe.iface.AudioConverter;
import message.IMessageListener;
import message.Message;
import module.Module;
import module.decode.p25.message.hdu.HDUMessage;
import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.message.ldu.LDU2Message;
import module.decode.p25.message.ldu.LDUMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.util.concurrent.ScheduledExecutorService;

public class P25AudioModule extends Module implements Listener<Message>, IAudioPacketProvider, IMessageListener,
    ISquelchStateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25AudioModule.class);

    private static final String IMBE_CODEC = "IMBE";
    private static boolean mLibraryLoadStatusLogged = false;

    private boolean mCanConvertAudio = false;
    private boolean mEncryptedCall = false;
    private boolean mEncryptedCallStateEstablished = false;

    private AudioConverter mAudioConverter;
    private Listener<AudioPacket> mAudioPacketListener;
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private Metadata mMetadata;
    private LDU1Message mCachedLDU1Message = null;

    public P25AudioModule(Metadata metadata)
    {
        mMetadata = metadata;
        loadConverter();
    }

    @Override
    public Listener<Message> getMessageListener()
    {
        return this;
    }

    @Override
    public Listener<SquelchState> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void dispose()
    {
        mAudioConverter = null;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {

    }

    @Override
    public void stop()
    {

    }

    /**
     * Processes call header (HDU) and voice frame (LDU1/LDU2) messages to decode audio and to determine the
     * encrypted audio status of a call event. Only the HDU and LDU2 messages convey encrypted call status. If an
     * LDU1 message is received without a preceding HDU message, then the LDU1 message is cached until the first
     * LDU2 message is received and the encryption state can be determined. Both the LDU1 and the LDU2 message are
     * then processed for audio if the call is unencrypted.
     */
    public void receive(Message message)
    {
        if(mCanConvertAudio && mAudioPacketListener != null)
        {
            if(mEncryptedCallStateEstablished)
            {
                if(message instanceof LDUMessage)
                {
                    processAudio((LDUMessage)message);
                }
            }
            else
            {
                if(message instanceof HDUMessage)
                {
                    mEncryptedCallStateEstablished = true;
                    mEncryptedCall = ((HDUMessage)message).isEncryptedAudio();
                }
                else if(message instanceof LDU1Message)
                {
                    //When we receive an LDU1 message with first receiving the HDU message, cache the LDU1 Message
                    //until we can determine the encrypted call state from the next LDU2 message
                    mCachedLDU1Message = (LDU1Message)message;
                }
                else if(message instanceof LDU2Message)
                {
                    mEncryptedCallStateEstablished = true;
                    LDU2Message ldu2 = (LDU2Message)message;
                    mEncryptedCall = ldu2.isEncryptedAudio();

                    if(mCachedLDU1Message != null)
                    {
                        processAudio(mCachedLDU1Message);
                        mCachedLDU1Message = null;
                    }

                    processAudio(ldu2);
                }
            }
        }
    }

    /**
     * Processes an audio packet by decoding the IMBE audio frames and rebroadcasting them as PCM audio packets.
     */
    private void processAudio(LDUMessage ldu)
    {
        if(!mEncryptedCall)
        {
            for(byte[] frame : ldu.getIMBEFrames())
            {
                float[] audio = mAudioConverter.decode(frame);

                audio = mGain.apply(audio);

                mAudioPacketListener.receive(new AudioPacket(audio, mMetadata.copyOf()));
            }
        }
        else
        {
            //Encrypted audio processing not implemented
        }
    }

    /**
     * Loads audio frame processing chain.  Constructs an imbe targetdataline
     * to receive the raw imbe frames.  Adds an IMBE to 8k PCM format conversion
     * stream wrapper.  Finally, adds an upsampling (8k to 48k) stream wrapper.
     */
    private void loadConverter()
    {
        AudioConversionLibrary library = null;

        try
        {
            @SuppressWarnings("rawtypes")
            Class temp = Class.forName("jmbe.JMBEAudioLibrary");

            library = (AudioConversionLibrary)temp.newInstance();

            if((library.getMajorVersion() == 0 && library.getMinorVersion() >= 3 &&
                library.getBuildVersion() >= 3) || library.getMajorVersion() >= 1)
            {
                mAudioConverter = library.getAudioConverter(IMBE_CODEC, AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO);

                if(mAudioConverter != null)
                {
                    mCanConvertAudio = true;

                    if(!mLibraryLoadStatusLogged)
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append("JMBE audio conversion library [");
                        sb.append(library.getVersion());
                        sb.append("] successfully loaded - P25 audio will be available");

                        mLog.info(sb.toString());

                        mLibraryLoadStatusLogged = true;
                    }
                }
                else
                {
                    if(!mLibraryLoadStatusLogged)
                    {
                        mLog.info("JMBE audio conversion library NOT FOUND");
                        mLibraryLoadStatusLogged = true;
                    }
                }
            }
            else
            {
                mLog.warn("JMBE library version 0.3.3 or higher is required - found: " + library.getVersion());
            }
        }
        catch(ClassNotFoundException e1)
        {
            if(!mLibraryLoadStatusLogged)
            {
                mLog.error("Couldn't find/load JMBE audio conversion library");
                mLibraryLoadStatusLogged = true;
            }
        }
        catch(InstantiationException e1)
        {
            if(!mLibraryLoadStatusLogged)
            {
                mLog.error("Couldn't instantiate JMBE audio conversion library class");
                mLibraryLoadStatusLogged = true;
            }
        }
        catch(IllegalAccessException e1)
        {
            if(!mLibraryLoadStatusLogged)
            {
                mLog.error("Couldn't load JMBE audio conversion library due to "
                    + "security restrictions");
                mLibraryLoadStatusLogged = true;
            }
        }
    }

    @Override
    public void setAudioPacketListener(Listener<AudioPacket> listener)
    {
        mAudioPacketListener = listener;
    }

    @Override
    public void removeAudioPacketListener()
    {
        mAudioPacketListener = null;
    }

    /**
     * Wrapper for squelch state to process end of call actions.  At call end the encrypted call state established
     * flag is reset so that the encrypted audio state for the next call can be properly detected and we send an
     * END audio packet so that downstream processors like the audio recorder can properly close out a call sequence.
     */
    public class SquelchStateListener implements Listener<SquelchState>
    {
        @Override
        public void receive(SquelchState state)
        {
            if(state == SquelchState.SQUELCH)
            {
                if(mAudioPacketListener != null)
                {
                    mAudioPacketListener.receive(new AudioPacket(AudioPacket.Type.END, mMetadata.copyOf()));
                }

                mEncryptedCallStateEstablished = false;
                mEncryptedCall = false;
                mCachedLDU1Message = null;
            }
        }
    }
}
