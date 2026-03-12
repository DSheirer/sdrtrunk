/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.gain.NonClippingGain;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Disconnect;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionRelease;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallWithOptionalLocation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CipherType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jmbe.iface.IAudioCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN AMBE audio module with SCRAMBLE cipher brute-force support.
 *
 * For unencrypted calls, audio is decoded and played in real-time.
 * For SCRAMBLE-encrypted calls:
 *   - If the key for this key-ID is already known (from a previous brute force), decryption
 *     happens in real-time and audio is played live.
 *   - If the key is unknown, frames are buffered. After the call ends a background brute force
 *     tries all 32767 possible 15-bit keys. The key with the highest decoded audio energy is
 *     stored in a per-session cache so subsequent calls with the same key-ID play immediately.
 */
public class NXDNAudioModule extends AmbeAudioModule
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NXDNAudioModule.class);

    /** Maximum number of encrypted frames to buffer for post-call brute force. */
    private static final int MAX_BUFFERED_FRAMES = 200;

    /** Minimum RMS energy (per sample) for a decoded frame to be considered non-silence. */
    private static final float MIN_ENERGY_THRESHOLD = 0.001f;

    /** Session-level cache: keyId → scrambler key.  Shared across all audio module instances. */
    private static final Map<Integer, Integer> sScrambleKeyCache = new HashMap<>();

    private static final ExecutorService sBruteForceExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "NXDN-BruteForce");
                t.setDaemon(true);
                return t;
            });

    private final SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private final NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private final List<Audio> mCachedAudioMessages = new ArrayList<>();

    /** Encrypted frames buffered for post-call brute force. */
    private final List<byte[]> mEncryptedFrameBuffer = new ArrayList<>();

    private boolean mEncryptedCall = false;
    private boolean mEncryptedCallStateEstablished = false;
    private AudioCodec mAudioCodec;
    private CipherType mCipherType = CipherType.UNENCRYPTED;
    private int mKeyId = 0;

    /** Live scrambler register state – valid only when mLiveDecryptionActive is true. */
    private int[] mLiveRegister = new int[]{0};
    private boolean mLiveDecryptionActive = false;

    /**
     * Constructs an instance
     * @param userPreferences component
     * @param aliasList for the current channel
     */
    public NXDNAudioModule(UserPreferences userPreferences, AliasList aliasList)
    {
        super(userPreferences, aliasList, 0);
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
    }

    @Override
    public void start()
    {
    }

    /**
     * Processes audio and layer 3 messages to decode audio and to determine the encrypted status of a call event.
     */
    public void receive(IMessage message)
    {
        if(hasAudioCodec())
        {
            if(message instanceof Audio audio)
            {
                if(mEncryptedCallStateEstablished)
                {
                    processAudio(audio);
                }
                else
                {
                    //Cache audio until we can determine the encryption state
                    mCachedAudioMessages.add(audio);
                }
            }
            else if(message.isValid())
            {
                if(message instanceof VoiceCall voiceCall)
                {
                    var encKey = voiceCall.getEncryptionKeyIdentifier();
                    setupCallState(encKey.isEncrypted(),
                            encKey.getValue() != null ? encKey.getValue().getAlgorithm() : 0,
                            encKey.getValue() != null ? encKey.getValue().getKey() : 0,
                            voiceCall.getCallOption().getCodec());
                }
                else if(message instanceof VoiceCallWithOptionalLocation voiceCall)
                {
                    var encKey = voiceCall.getEncryptionKeyIdentifier();
                    setupCallState(encKey.isEncrypted(),
                            encKey.getValue() != null ? encKey.getValue().getAlgorithm() : 0,
                            encKey.getValue() != null ? encKey.getValue().getKey() : 0,
                            voiceCall.getCallOption().getCodec());
                }
                else if(message instanceof Disconnect || message instanceof TransmissionRelease)
                {
                    handleCallEnd();
                }
            }
        }
    }

    /**
     * Sets up the call state once the VoiceCall message is received.
     */
    private void setupCallState(boolean encrypted, int algorithm, int keyId, AudioCodec codec)
    {
        mEncryptedCall = encrypted;
        mEncryptedCallStateEstablished = true;
        mAudioCodec = codec;
        mCipherType = CipherType.fromValue(algorithm);
        mKeyId = keyId;
        mLiveDecryptionActive = false;

        if(mEncryptedCall && mCipherType == CipherType.SCRAMBLE)
        {
            Integer cachedKey = sScrambleKeyCache.get(mKeyId);
            if(cachedKey != null)
            {
                LOGGER.info("NXDN SCRAMBLE: using cached key {} for key-ID {}", cachedKey, mKeyId);
                mLiveRegister[0] = cachedKey & 0x7FFF;
                mLiveDecryptionActive = true;
            }
            else
            {
                LOGGER.info("NXDN SCRAMBLE: key-ID {} unknown – buffering frames for brute force", mKeyId);
            }
        }

        processCachedAudio();
    }

    /**
     * Triggers end-of-call cleanup and, for unsolved SCRAMBLE calls, a background brute force.
     */
    private void handleCallEnd()
    {
        if(mEncryptedCall && mCipherType == CipherType.SCRAMBLE && !mLiveDecryptionActive
                && !mEncryptedFrameBuffer.isEmpty())
        {
            List<byte[]> framesToCrack = new ArrayList<>(mEncryptedFrameBuffer);
            int keyIdForCrack = mKeyId;
            IAudioCodec codec = getAudioCodec();

            sBruteForceExecutor.submit(() -> bruteForce(framesToCrack, keyIdForCrack, codec));
        }

        closeAudioSegment();
        mCachedAudioMessages.clear();
        mEncryptedFrameBuffer.clear();
        mEncryptedCall = false;
        mEncryptedCallStateEstablished = false;
        mLiveDecryptionActive = false;
    }

    /**
     * Processes any cached audio frames that were pending an encryption state determination.
     */
    private void processCachedAudio()
    {
        for(Audio audio : mCachedAudioMessages)
        {
            processAudio(audio);
        }

        mCachedAudioMessages.clear();
    }

    /**
     * Processes an audio packet.  For unencrypted or live-decryptable SCRAMBLE calls the AMBE frames
     * are decoded and sent downstream.  For buffered-but-not-yet-cracked SCRAMBLE calls the raw
     * encrypted frames are stored for post-call brute force.
     */
    private void processAudio(Audio audio)
    {
        if(mAudioCodec == null || !mAudioCodec.equals(AudioCodec.HALF_RATE))
        {
            return; // Full-rate not yet supported
        }

        if(!mEncryptedCall)
        {
            // Unencrypted – decode and play directly
            for(byte[] frame : audio.getAudioFrames())
            {
                float[] generatedAudio = getAudioCodec().getAudio(frame);
                generatedAudio = mGain.apply(generatedAudio);
                addAudio(generatedAudio);
            }
        }
        else if(mCipherType == CipherType.SCRAMBLE && mLiveDecryptionActive)
        {
            // Known key – decrypt and play in real-time
            for(byte[] frame : audio.getAudioFrames())
            {
                byte[] decrypted = NXDNVoiceScrambler.descrambleFrame(frame, mLiveRegister);
                float[] generatedAudio = getAudioCodec().getAudio(decrypted);
                generatedAudio = mGain.apply(generatedAudio);
                addAudio(generatedAudio);
            }
        }
        else if(mCipherType == CipherType.SCRAMBLE)
        {
            // Unknown key – buffer frames for post-call brute force
            for(byte[] frame : audio.getAudioFrames())
            {
                if(mEncryptedFrameBuffer.size() < MAX_BUFFERED_FRAMES)
                {
                    mEncryptedFrameBuffer.add(frame.clone());
                }
            }
        }
    }

    /**
     * Background brute force: tries all 32767 possible 15-bit SCRAMBLE keys against the buffered
     * encrypted frames.  Decodes several frames per key with the JMBE codec and picks the key whose
     * decoded audio has the highest RMS energy (i.e. sounds most like speech).
     *
     * On success the key is stored in the session cache so future calls with the same key-ID play live.
     *
     * @param frames      buffered encrypted AMBE frames from the call
     * @param keyId       key identifier from the signaling layer
     * @param codec       AMBE codec instance used to decode test frames
     */
    private static void bruteForce(List<byte[]> frames, int keyId, IAudioCodec codec)
    {
        if(frames.isEmpty() || codec == null)
        {
            return;
        }

        LOGGER.info("NXDN SCRAMBLE brute force starting for key-ID {} ({} frames buffered)…", keyId, frames.size());

        // Use up to 5 frames near the start for faster testing
        int testCount = Math.min(5, frames.size());
        List<byte[]> testFrames = frames.subList(0, testCount);

        int bestKey = -1;
        float bestEnergy = 0.0f;

        for(int key = 1; key <= 32767; key++)
        {
            List<byte[]> decrypted = NXDNVoiceScrambler.descramble(testFrames, key);

            codec.reset();
            float energy = 0.0f;
            int sampleCount = 0;

            for(byte[] frame : decrypted)
            {
                float[] audio = codec.getAudio(frame);
                if(audio != null)
                {
                    for(float s : audio)
                    {
                        energy += s * s;
                    }
                    sampleCount += audio.length;
                }
            }

            float rms = sampleCount > 0 ? (float)Math.sqrt(energy / sampleCount) : 0.0f;

            if(rms > bestEnergy)
            {
                bestEnergy = rms;
                bestKey = key;
            }

            if(key % 4096 == 0)
            {
                LOGGER.debug("NXDN SCRAMBLE brute force progress: {}/32767 (best so far key={} rms={:.4f})",
                        key, bestKey, bestEnergy);
            }
        }

        if(bestKey > 0 && bestEnergy >= MIN_ENERGY_THRESHOLD)
        {
            LOGGER.info("NXDN SCRAMBLE brute force FOUND key={} (RMS energy={}) for key-ID {}",
                    bestKey, bestEnergy, keyId);
            sScrambleKeyCache.put(keyId, bestKey);
        }
        else
        {
            LOGGER.warn("NXDN SCRAMBLE brute force for key-ID {} did not find a confident key " +
                    "(best key={} RMS={}). Call may have been too short or cipher type mismatch.",
                    keyId, bestKey, bestEnergy);
        }
    }

    /**
     * Wrapper for squelch state to process end of call actions.
     */
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH)
            {
                handleCallEnd();
            }
        }
    }
}
