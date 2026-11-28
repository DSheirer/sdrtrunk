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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.map.MapView;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.web.WebServer;
import io.github.dsheirer.web.WebServerStatus;

/**
 * Web server status item, for example: ● Web 127.0.0.1:8080.  While the server runs, a click opens the web client
 * in the browser.  Otherwise the item shows Off or Failed and a click opens the web server settings.
 */
public class WebServerStatusItem extends ServiceStatusItem
{
    private final WebServer mWebServer;
    private final Listener<WebServerStatus> mStatusListener = status -> scheduleUpdate();

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param webServer to show the status of
     */
    public WebServerStatusItem(WebServer webServer)
    {
        super("Web Server", Setting.MAIN_WINDOW_STATUS_ITEM_WEB_SERVER);
        mWebServer = webServer;
        setAction(this::open);
        mWebServer.addStatusListener(mStatusListener);
        update();
    }

    private WebServerStatus getStatus()
    {
        WebServerStatus status = mWebServer.getStatus();
        return status != null ? status : WebServerStatus.stopped();
    }

    private void open()
    {
        WebServerStatus status = getStatus();

        if(status.state() == WebServerStatus.State.RUNNING && status.url() != null)
        {
            MapView.browse(status.url());
        }
        else
        {
            MyEventBus.getGlobalEventBus().post(new ViewSettingsEditorRequest(SettingsPageType.WEB_SERVER));
        }
    }

    @Override
    protected void update()
    {
        WebServerStatus status = getStatus();

        switch(status.state())
        {
            case RUNNING ->
            {
                if(status.url() != null)
                {
                    show(StatusSeverity.OK, "Web " + address(status.url()), "The web server is running at " +
                        status.url() + "\n\nClick to open the web client in the browser.");
                }
                else
                {
                    show(StatusSeverity.OK, "Web on", "The web server is running.\n\nClick to change the web " +
                        "server settings.");
                }
            }
            case FAILED -> show(StatusSeverity.ERROR, "Web failed", "The web server couldn't start: " +
                status.error() + "\n\nClick to change the web server settings.");
            default -> show(StatusSeverity.NEUTRAL, "Web off", "The web server is off.\n\nClick to turn it on " +
                "in the web server settings.");
        }
    }

    /**
     * Address without the scheme and the trailing slash, to keep the status bar short.
     */
    static String address(String url)
    {
        String address = url.replaceFirst("^[a-zA-Z]+://", "");
        return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
    }

    @Override
    public void dispose()
    {
        mWebServer.removeStatusListener(mStatusListener);
    }
}
