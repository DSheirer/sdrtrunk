package io.github.dsheirer.audio.broadcast.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Small binary-safe multipart/form-data builder used by remote call and Whisper requests. */
public class MultipartBody
{
    private final String mBoundary = "sdrtrunk-" + UUID.randomUUID();
    private final ByteArrayOutputStream mOutput = new ByteArrayOutputStream();

    public String boundary() { return mBoundary; }

    public MultipartBody text(String name, String value)
    {
        if(value != null)
        {
            write("--" + mBoundary + "\r\nContent-Disposition: form-data; name=\"" + name +
                "\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + value + "\r\n");
        }
        return this;
    }

    public MultipartBody file(String name, String filename, String contentType, byte[] data)
    {
        write("--" + mBoundary + "\r\nContent-Disposition: form-data; name=\"" + name +
            "\"; filename=\"" + filename.replace("\"", "") + "\"\r\nContent-Type: " + contentType + "\r\n\r\n");
        try
        {
            mOutput.write(data);
        }
        catch(IOException e)
        {
            throw new IllegalStateException("Unable to create multipart request", e);
        }
        write("\r\n");
        return this;
    }

    public HttpRequest.BodyPublisher publisher()
    {
        write("--" + mBoundary + "--\r\n");
        return HttpRequest.BodyPublishers.ofByteArray(mOutput.toByteArray());
    }

    private void write(String value)
    {
        try
        {
            mOutput.write(value.getBytes(StandardCharsets.UTF_8));
        }
        catch(IOException e)
        {
            throw new IllegalStateException("Unable to create multipart request", e);
        }
    }
}
