/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

 package io.github.dsheirer.audio.broadcast.openmhz;

 /**
  * HTTP headers used for posting to Rdio Scanner API
  */
 public enum FormField
 {
     CALL("call"),
     FREQ("freq"),
     START_TIME("start_time"),
     STOP_TIME("stop_time"),
     CALL_LENGTH("call_length"),
     TALKGROUP_NUM("talkgroup_num"),
     EMERGENCY("emergency"),
     API_KEY("api_key"),
     SOURCE_LIST("source_list"),
     FREQ_LIST("freq_list"),
     PATCH_LIST("patch_list"),
     TALKER_ALIAS("talker_alias");

     private String mHeader;

     FormField(String header)
     {
         mHeader = header;
     }

     public String getHeader()
     {
         return mHeader;
     }
 }