package org.jma.encoder.audio;

/**
 * Retrieved from: http://livertmpjavapublisher.blogspot.com/2011/06/orgtritonuslowlevellameapiiaudioencoder.html
 * on 5 Sep 2016
 */
public interface IAudioEncoder
{
    public int encodeBuffer(byte[] input, int offset, int encodedCount, byte[] output);

    public int encodeFinish(byte[] input);

    public int getInputBufferSize();

    public int getOutputBufferSize();

    public void close();
}
