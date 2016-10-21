/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast.icecast;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MyProducer extends BasicAsyncRequestProducer
{
    private final static Logger mLog = LoggerFactory.getLogger( MyProducer.class );

    public MyProducer(HttpHost target, HttpEntityEnclosingRequest request, HttpAsyncContentProducer producer)
    {
        super(target, request, producer);
    }

    @Override
    public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException
    {
//        mLog.debug("############### Producing Content");
        super.produceContent(encoder, ioctrl);
    }

    @Override
    public HttpRequest generateRequest()
    {
        mLog.debug("############### Generating a request");

        HttpRequest request = super.generateRequest();

        mLog.debug("############### Our request class is:" + request.getClass() );

        return request;
    }

    @Override
    public HttpHost getTarget()
    {
        mLog.debug("############### Getting our target");
        return super.getTarget();
    }

    @Override
    public void requestCompleted(HttpContext context)
    {
        mLog.debug("############### requesting completed");
        super.requestCompleted(context);
    }

    @Override
    public void failed(Exception ex)
    {
        mLog.debug("############### Fail!");
        super.failed(ex);
    }

    @Override
    public boolean isRepeatable()
    {
        mLog.debug("############### Indicating if we're repeatable");
        return super.isRepeatable();
    }

    @Override
    public void resetRequest() throws IOException
    {
        mLog.debug("############### resetting request");
        super.resetRequest();
    }

    @Override
    public void close() throws IOException
    {
        mLog.debug("############### Closing");
        super.close();
    }
}
