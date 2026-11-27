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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.pager.flex.FlexDecoder;
import io.github.dsheirer.module.decode.pager.pocsag.PocsagDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Auxiliary paging decoder for the demodulated audio of an NBFM channel.  Decodes POCSAG at 512, 1200 and 2400 baud
 * and FLEX at 1600/2, 3200/2, 3200/4 and 6400/4 simultaneously, so no configuration is required.  POCSAG at 512 and
 * 1200 baud is also decoded when it is sent as 1200/2200 Hz AFSK audio tones over a voice channel.
 *
 * The NBFM decoder only provides audio while its noise squelch is open, and it controls the channel state.  The
 * protocol decoders are reset when the NBFM decoder reports that the channel is idle (squelch closed) since the audio
 * stream is not continuous across the squelch closure.
 */
public class PagerDecoder extends Decoder implements IRealBufferListener, Listener<float[]>, IDecoderStateEventListener
{
    public static final double SAMPLE_RATE = 8000.0;
    private static final int[] POCSAG_BIT_RATES = {512, 1200, 2400};
    private static final int[] POCSAG_AFSK_BIT_RATES = {512, 1200};
    private static final double AFSK_LOWER_TONE = 1200.0;
    private static final double AFSK_UPPER_TONE = 2200.0;
    private static final double AFSK_TONE_WINDOW_SECONDS = 0.001;

    private final List<FskSymbolDemodulator> mDemodulators = new ArrayList<>();
    private final List<FskSymbolDemodulator> mAfskDemodulators = new ArrayList<>();
    private final AfskToneDiscriminator mAfskToneDiscriminator = new AfskToneDiscriminator(SAMPLE_RATE,
        AFSK_LOWER_TONE, AFSK_UPPER_TONE, AFSK_TONE_WINDOW_SECONDS);
    private final List<PocsagDecoder> mPocsagDecoders = new ArrayList<>();
    private final FlexDecoder mFlexDecoder;
    private final Listener<DecoderStateEvent> mDecoderStateEventListener = this::receiveDecoderStateEvent;
    private boolean mSignalPresent;

    /**
     * Constructs an instance
     */
    public PagerDecoder()
    {
        Listener<IMessage> messageListener = message -> getMessageListener().receive(message);

        for(int bitRate: POCSAG_BIT_RATES)
        {
            PocsagDecoder decoder = new PocsagDecoder(bitRate, false, messageListener);
            mPocsagDecoders.add(decoder);
            mDemodulators.add(new FskSymbolDemodulator(SAMPLE_RATE, bitRate, getAverageLength(bitRate), decoder));
        }

        //POCSAG sent as 1200/2200 Hz AFSK audio tones over a voice channel
        for(int bitRate: POCSAG_AFSK_BIT_RATES)
        {
            PocsagDecoder decoder = new PocsagDecoder(bitRate, true, messageListener);
            mPocsagDecoders.add(decoder);
            mAfskDemodulators.add(new FskSymbolDemodulator(SAMPLE_RATE, bitRate, getAverageLength(bitRate), decoder));
        }

        //The FLEX demodulator changes symbol rate within each frame, so it doesn't use a moving average filter that
        //would need to change length with the symbol rate.
        FlexSymbolRouter router = new FlexSymbolRouter();
        FskSymbolDemodulator flexDemodulator = new FskSymbolDemodulator(SAMPLE_RATE, FlexDecoder.getSyncSymbolRate(),
            1, router);
        mFlexDecoder = new FlexDecoder(flexDemodulator, messageListener);
        router.setDecoder(mFlexDecoder);
        mDemodulators.add(flexDemodulator);
    }

    /**
     * Moving average filter length of about half of a symbol period, to reduce noise at the lower symbol rates.
     */
    private static int getAverageLength(int symbolRate)
    {
        return Math.max(1, (int)(SAMPLE_RATE / symbolRate / 2.0));
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.PAGER;
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return this;
    }

    @Override
    public void receive(float[] samples)
    {
        mSignalPresent = true;

        for(FskSymbolDemodulator demodulator: mDemodulators)
        {
            demodulator.process(samples);
        }

        float[] afsk = mAfskToneDiscriminator.process(samples);

        for(FskSymbolDemodulator demodulator: mAfskDemodulators)
        {
            demodulator.process(afsk);
        }
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mDecoderStateEventListener;
    }

    /**
     * Monitors decoder state events for the IDLE notifications that the NBFM decoder broadcasts while the noise
     * squelch is closed, and resets the protocol decoders once at the end of each transmission.
     */
    private void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getState() == State.IDLE && event.getEvent() == DecoderStateEvent.Event.CONTINUATION && mSignalPresent)
        {
            mSignalPresent = false;
            reset();
        }
        else if(event.getEvent() == DecoderStateEvent.Event.REQUEST_RESET)
        {
            mSignalPresent = false;
            reset();
        }
    }

    /**
     * Resets the protocol decoders and demodulators.  Pages in progress are sent before the reset.
     */
    @Override
    public void reset()
    {
        for(PocsagDecoder decoder: mPocsagDecoders)
        {
            decoder.reset();
        }

        mFlexDecoder.reset();

        for(FskSymbolDemodulator demodulator: mDemodulators)
        {
            demodulator.reset();
        }

        mAfskToneDiscriminator.reset();

        for(FskSymbolDemodulator demodulator: mAfskDemodulators)
        {
            demodulator.reset();
        }
    }

    @Override
    public void stop()
    {
        reset();
        super.stop();
    }

    /**
     * Routes FLEX demodulator symbols to the FLEX decoder, which is constructed after the demodulator.
     */
    private static class FlexSymbolRouter implements FskSymbolDemodulator.ISymbolListener
    {
        private FlexDecoder mDecoder;

        public void setDecoder(FlexDecoder decoder)
        {
            mDecoder = decoder;
        }

        @Override
        public void receive(float symbol)
        {
            if(mDecoder != null)
            {
                mDecoder.receive(symbol);
            }
        }
    }
}
