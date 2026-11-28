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

/**
 * Overlap-save (fast convolution) channelizer: a tuner channelizer that serves any number of channels, at any
 * frequency offset, from one forward FFT of the tuner's sample stream.  It is selected with
 * {@link io.github.dsheirer.settings.source.ChannelizerType#FAST_CONVOLUTION} and runs alongside the polyphase and
 * heterodyne channelizers.
 *
 * <h2>Structure</h2>
 *
 * <pre>
 *   buffer processor thread                            each channel's dispatcher thread
 *   -----------------------                            --------------------------------
 *   tuner buffer -> I/Q
 *        |
 *   [ P old | L new ]  block of N samples
 *        |
 *   forward FFT, size N  (once, for all channels)
 *        |
 *        +-- gather Nout bins around k0 (bin shift) ---> x mask H[m] -> Nout-point IFFT -> drop P/D -> block phase,
 *        |                                                                                           rotator -> decoder
 *        +-- gather ... (one per channel)
 * </pre>
 *
 * <h2>Overlap-save fast convolution</h2>
 *
 * Each block is N complex samples: the last P = N - L samples of the previous block followed by L new samples, the
 * hop.  The overlap factor V sets the hop to L = N (V - 1) / V, so the overlap is P = N / V.  Multiplying the block's
 * spectrum by a channel filter's frequency response performs a circular convolution.  When the filter's impulse
 * response has at most P + 1 taps, the first P outputs of each block are the only ones that wrap around the block, so
 * discarding them leaves L outputs of the linear convolution, and consecutive blocks join without a seam.  Borgerding
 * [1] shows that the same structure also mixes and downsamples, so one forward FFT serves many channels, each of which
 * only costs a few hundred bins of work.  Renfors, Yli-Kaakinen and harris [2] analyze it as a multirate filter bank
 * and set out the trade-offs between the overlap factor, the block size and the mask design.
 *
 * <h2>Block size and sample rates</h2>
 *
 * The bins are 40 Hz wide, so N = Fs / 40, and V = 5.  At 10 MS/s that is N = 250,000 = 2^4 x 5^6, a hop of
 * L = 200,000 samples (20 ms) and an overlap of P = 50,000 samples (5 ms).  The FFT sizes are restricted to products
 * of 2, 3 and 5 so that the output rates are round (625 bins = 25 kHz) and the FFTs in dsp/fft can transform them.
 * 2.048, 2.4, 2.5, 3, 6, 8, 10 and 20 MS/s qualify.  Sample rates that don't, for example 2.8 MS/s
 * (70,000 = 2^4 x 5^4 x 7), are served by the polyphase channelizer instead, with a log message
 * ({@link io.github.dsheirer.source.tuner.manager.OverlapSaveChannelSourceManager}).  The fallback is by sample rate
 * only: at a supported rate, a channel that the channelizer can't provide is refused, as with the polyphase
 * channelizer.  The forward FFT is the one that calibration selected
 * ({@link io.github.dsheirer.dsp.fft.ComplexFFTFactory}): the four-step VectorComplexFFT or the scalar
 * MixedRadixComplexFFT.
 *
 * <h2>Per channel</h2>
 *
 * <ul>
 *     <li><b>Frequency translation by bin shift.</b>  The channel's center bin is k0 = round(offset / 40 Hz).  Taking
 *     the output band's bins around k0 as the new bin 0 moves the channel to baseband with no mixer [1].</li>
 *     <li><b>Mask.</b>  The bins are multiplied by the Nout-point DFT of a FIR low-pass filter designed at the output
 *     rate: for a decoder-ready channel, the decoder's own channel filter convolved with a 90 dB Kaiser windowed sinc
 *     (below); otherwise a wide 90 dB Kaiser windowed sinc.  The filter's length is limited to P / D + 1 taps (D
 *     below): 126 taps at 25 kHz and 251 at 50 kHz when Fs is 10 MS/s.  The stop band reaches the output Nyquist
 *     frequency because bins outside the output band are dropped [2].</li>
 *     <li><b>Decimation.</b>  Only Nout bins are kept and inverse transformed with an Nout-point FFT, which decimates
 *     by D = N / Nout in the frequency domain [1].  At 10 MS/s a 25 kHz channel keeps Nout = 625 bins, so D = 400.  D
 *     must divide both L and P.</li>
 *     <li><b>Overlap discard.</b>  The first P / D outputs of each inverse transform (125 at 25 kHz) are discarded and
 *     the remaining L / D (500) are the block's output.</li>
 * </ul>
 *
 * <h2>Block phase correction</h2>
 *
 * The bin shift mixes each block by exp(-j 2 pi k0 n / N), where n counts from the start of the block, not from the
 * start of the stream.  Block b starts L b samples into the stream, so its output carries an extra phase of
 * exp(j 2 pi k0 L b / N) = exp(j 2 pi k0 b (V - 1) / V).  This is 1 for every block only when k0 is a multiple of V.
 * Otherwise the output phase steps at every block boundary, by a constant (k0 mod V) / V of a turn, which would
 * scramble any phase-modulated signal.  Each block is multiplied by the conjugate of that phase [2].  The phase is
 * accumulated from block to block with the step of the block's own k0, so a change of center bin also leaves the
 * output phase continuous.
 *
 * <h2>Residual rotator, frequency correction and retuning</h2>
 *
 * The bin center is within 20 Hz of the channel frequency.  A rotator at the output rate removes the residual
 * offset and applies the decoder's frequency correction, as the polyphase channel's mixer does.  It follows the
 * channel filter, so its phase is referenced back by the filter's group delay, which keeps the phase continuous when
 * the correction changes.  A tuner center frequency change is applied to every channel at the next block boundary on
 * the buffer processor thread, by recomputing each channel's bins.
 *
 * <h2>Decoder-ready output</h2>
 *
 * When a decoder's channel specification carries its channel filter
 * ({@link io.github.dsheirer.source.tuner.channel.ChannelSpecification#getChannelFilter()}), the channel is delivered
 * at the filter's rate, normally 25 kHz, at which the decoder doesn't decimate, and the mask applies the decoder's own
 * filter, designed by the decoder's own design function at the output rate, convolved with a Kaiser low-pass whose
 * transition starts just above the decoder's pass band.  So the decoder sees the pass band response that its
 * demodulator, symbol recovery and squelch are tuned to, and the Kaiser filter adds stop band attenuation (the
 * decoders' own filters stop at about 40 to 46 dB).  The source reports
 * {@link io.github.dsheirer.source.tuner.channel.TunerChannelSource#isChannelFiltered()}, the processing chain passes
 * that on with the startup sample rate event, and the decoder then skips its own baseband filter.  A channel is
 * decoder-ready only when the channelizer can deliver the filter's rate exactly and the filter fits the overlap's
 * length limit at that rate (OverlapSaveChannelSource.isDecoderReady()).  For example, at 10 MS/s the available rates
 * near 25 kHz are those whose decimation divides the 50,000-sample overlap, so 25 and 50 kHz are available and 12,
 * 12.5 and 30 kHz aren't.  Other channels, and decoders without a channel filter, get at least 50 kHz with a wide
 * mask, as from a polyphase channel, and filter as usual.
 *
 * <h2>Threads</h2>
 *
 * The buffer processor thread converts each tuner buffer, runs the forward FFT for each block and copies each
 * channel's Nout bins into a pooled array for that channel's dispatcher.  Each channel's dispatcher thread applies
 * the mask, the inverse FFT, the overlap discard, the block phase and the rotator, and calls the decoder.  There is no
 * shared spectrum to hold or reference count beyond the copy, and a slow channel only drops its own blocks.
 *
 * <h2>Latency, block length and memory</h2>
 *
 * Each channel receives one buffer per block: 500 samples at 25 kHz, every 20 ms.  Latency is the 20 ms hop plus the
 * channel filter's group delay (at most 2.5 ms at 25 kHz, where the mask is at most 126 taps) plus the dispatcher
 * interval.  At 10 MS/s the channelizer holds the block, the saved overlap, and the four-step FFT's work matrix,
 * strips and twiddles: of the order of 10 MB.  Each channel holds its mask, its inverse FFT and up to a second of
 * queued bins (about 260 kB at 25 kHz).
 *
 * <h2>References</h2>
 *
 * <ol>
 *     <li>M. Borgerding, "Turning Overlap-Save into a Multiband Mixing, Downsampling Filter Bank", IEEE Signal
 *     Processing Magazine, vol. 23, no. 2, pp. 158-161, March 2006.
 *     <a href="https://doi.org/10.1109/MSP.2006.1598092">https://doi.org/10.1109/MSP.2006.1598092</a></li>
 *     <li>M. Renfors, J. Yli-Kaakinen, F. J. harris, "Analysis and Design of Efficient and Flexible Fast-Convolution
 *     Based Multirate Filter Banks", IEEE Transactions on Signal Processing, vol. 62, no. 15, pp. 3768-3783, 2014.
 *     <a href="https://doi.org/10.1109/TSP.2014.2330331">https://doi.org/10.1109/TSP.2014.2330331</a></li>
 * </ol>
 *
 * Credit to Phil Karn, KA9Q, whose ka9q-radio (<a href="https://github.com/ka9q/ka9q-radio">
 * https://github.com/ka9q/ka9q-radio</a>) trialed and proved out this fast-convolution approach for multichannel SDR
 * reception, and the technique was further tuned in the scanrad.io receiver backend (by Matthew Kaufman, KA6SQG).
 */
package io.github.dsheirer.dsp.filter.channelizer.ols;
