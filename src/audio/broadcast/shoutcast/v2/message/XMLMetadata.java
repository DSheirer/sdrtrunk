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
package audio.broadcast.shoutcast.v2.message;

public class XMLMetadata extends UltravoxMessage
{
    /**
     * Client request to server to send cacheable XML metadata
     *
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     */
    XMLMetadata()
    {
        super(UltravoxMessageType.XML_METADATA);
    }


    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    XMLMetadata(byte[] data)
    {
        super(data);
    }

//    /**
//     * Adds custom metadata (TXXX) tagged value to the metadata set.  The metadata value is identified by the
//     * id parameter.
//     *
//     * @param id tag to identify the type of metadata
//     * @param value for the id tag
//     */
//    public void addCustomMetadata(String id, String value)
//    {
//        StringBuilder sb = new StringBuilder();
//        sb.append("<").append(UltravoxMetadata.CUSTOM_TEXT_FIELD.getXMLTag());
//        sb.append(" id=\"").append(id).append("\">");
//        sb.append(value);
//        sb.append("</").append(UltravoxMetadata.CUSTOM_TEXT_FIELD.getXMLTag()).append(">");
//
//        mMetadata.add(sb.toString());
//    }
//
//    /**
//     * Adds the binary object data to the metadata set.
//     * @param id tag to identify the binary object
//     * @param mime type
//     * @param filename associated with the data
//     * @param base64Data base-64 encoded binary data to embed in the metadata tag
//     */
//    public void addBinaryObject(String id, String mime, String filename, String base64Data)
//    {
//        StringBuilder sb = new StringBuilder();
//        sb.append("<").append(UltravoxMetadata.GENERAL_BINARY_OBJECT.getXMLTag());
//        sb.append(" mime=\"").append(mime).append("\"");
//        sb.append(" filename=\"").append(filename).append("\"");
//        sb.append(" id=\"").append(id).append("\"");
//        sb.append(">");
//        sb.append(base64Data);
//        sb.append("</").append(UltravoxMetadata.GENERAL_BINARY_OBJECT.getXMLTag()).append(">");
//
//        mMetadata.add(sb.toString());
//    }
//
//    /**
//     * Adds a recording time (TDRC) tagged metadata timestamp
//     *
//     * @param timestamp for the recording time
//     */
//    public void addRecordingTime(long timestamp)
//    {
//        Calendar calendar = new GregorianCalendar();
//        calendar.setTimeInMillis(timestamp);
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("<TDRC>\n");
//        sb.append("<YEAR>").append(calendar.get(Calendar.YEAR)).append("</YEAR>\n");
//        sb.append("<MONTH>").append(calendar.get(Calendar.MONTH)).append("</MONTH>\n");
//        sb.append("<DAY>").append(calendar.get(Calendar.DAY_OF_MONTH)).append("</DAY>\n");
//        sb.append("<HOUR>").append(calendar.get(Calendar.HOUR_OF_DAY)).append("</HOUR>\n");
//        sb.append("<MINUTE>").append(calendar.get(Calendar.MINUTE)).append("</MINUTE>\n");
//        sb.append("<ZONE>").append(calendar.get(Calendar.ZONE_OFFSET)).append("</ZONE>\n");
//        sb.append("</TDRC>\n");
//
//        mMetadata.add(sb.toString());
//    }
}
