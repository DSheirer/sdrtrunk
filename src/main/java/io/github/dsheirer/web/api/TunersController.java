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

package io.github.dsheirer.web.api;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.auth.AccessManager;
import io.javalin.openapi.OpenApiRequestBody;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.web.api.RuntimeViews.TunerChannelView;
import io.github.dsheirer.web.api.RuntimeViews.TunerView;
import io.github.dsheirer.web.auth.Permission;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import java.util.List;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Discovered tuners: /api/v1/tuners.  Reading needs tuner.read; enable, disable and frequency need tuner.control.
 */
public class TunersController
{
    private final TunerManager mTunerManager;
    private final AccountService mAccountService;

    /**
     * Constructs an instance
     * @param tunerManager with the discovered tuners, or null when it isn't available
     * @param accountService for the audit log
     */
    public TunersController(TunerManager tunerManager, AccountService accountService)
    {
        mTunerManager = tunerManager;
        mAccountService = accountService;
    }

    public void register(RoutesConfig routes)
    {
        routes.get(API_V1 + "/tuners", this::list, Permission.TUNER_READ);
        routes.get(API_V1 + "/tuners/{key}", this::get, Permission.TUNER_READ);
        routes.get(API_V1 + "/tuners/{key}/channels", this::channels, Permission.TUNER_READ);
        routes.post(API_V1 + "/tuners/{key}/enable", this::enable, Permission.TUNER_CONTROL);
        routes.post(API_V1 + "/tuners/{key}/disable", this::disable, Permission.TUNER_CONTROL);
        routes.put(API_V1 + "/tuners/{key}/frequency", this::setFrequency, Permission.TUNER_CONTROL);
    }

    @OpenApi(path = API_V1 + "/tuners/{key}/enable", methods = HttpMethod.POST, tags = "Tuners",
        summary = "Enable a tuner for use by sdrtrunk (remembered across restarts)",
        pathParams = @OpenApiParam(name = "key", required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerView.class)))
    private void enable(Context ctx)
    {
        setEnabled(ctx, true);
    }

    @OpenApi(path = API_V1 + "/tuners/{key}/disable", methods = HttpMethod.POST, tags = "Tuners",
        summary = "Disable a tuner, stopping the channels that use it (remembered across restarts)",
        pathParams = @OpenApiParam(name = "key", required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerView.class)))
    private void disable(Context ctx)
    {
        setEnabled(ctx, false);
    }

    private void setEnabled(Context ctx, boolean enabled)
    {
        DiscoveredTuner tuner = find(ctx.pathParam("key"));

        if(tuner.getTunerStatus() == io.github.dsheirer.source.tuner.manager.TunerStatus.REMOVED)
        {
            throw ApiException.conflict("The tuner was removed");
        }

        //Starting or stopping a tuner can take a moment; the request waits for it
        tuner.setEnabled(enabled);
        mAccountService.audit(AccessManager.principal(ctx).username(), enabled ? "tuner.enable" : "tuner.disable",
            tuner.getId(), true, ctx.ip(), null);
        ctx.json(TunerView.of(tuner));
    }

    @OpenApi(path = API_V1 + "/tuners/{key}/frequency", methods = HttpMethod.PUT, tags = "Tuners",
        summary = "Tune the tuner's center frequency",
        description = "Refused while channels are using the tuner: they lock its frequency and sample rate.",
        pathParams = @OpenApiParam(name = "key", required = true),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = FrequencyRequest.class), required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerView.class)),
            @OpenApiResponse(status = "409", description = "Locked by channels, not running, or out of range")
        })
    private void setFrequency(Context ctx)
    {
        FrequencyRequest request = ApiSupport.body(ctx, FrequencyRequest.class);
        DiscoveredTuner tuner = find(ctx.pathParam("key"));

        if(request.frequency() == null || request.frequency() <= 0)
        {
            throw ApiException.invalid("frequency", "is required, in Hz");
        }

        if(!tuner.hasTuner() || !tuner.isAvailable())
        {
            throw ApiException.conflict("The tuner isn't running");
        }

        TunerController controller = tuner.getTuner().getTunerController();

        if(controller.isLockedSampleRate())
        {
            throw ApiException.conflict("Channels are using the tuner, so its frequency is locked");
        }

        if(request.frequency() < controller.getMinimumFrequency() ||
            request.frequency() > controller.getMaximumFrequency())
        {
            throw ApiException.invalid("frequency", "must be from " + controller.getMinimumFrequency() + " to " +
                controller.getMaximumFrequency() + " Hz");
        }

        try
        {
            controller.setFrequency(request.frequency());
        }
        catch(SourceException se)
        {
            throw ApiException.conflict("The tuner couldn't tune: " + se.getMessage());
        }

        if(tuner.hasTunerConfiguration())
        {
            tuner.getTunerConfiguration().setFrequency(controller.getFrequency());
            mTunerManager.saveConfigurations();
        }

        mAccountService.audit(AccessManager.principal(ctx).username(), "tuner.frequency", tuner.getId(), true,
            ctx.ip(), String.valueOf(controller.getFrequency()));
        ctx.json(TunerView.of(tuner));
    }

    /**
     * New center frequency
     * @param frequency in Hz
     */
    public record FrequencyRequest(Long frequency) {}

    @OpenApi(path = API_V1 + "/tuners", methods = HttpMethod.GET, tags = "Tuners",
        summary = "Discovered tuners: USB, network and recording tuners, with their status and tuning",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerView[].class)))
    private void list(Context ctx)
    {
        ctx.json(new Page<>(getTuners().stream().map(TunerView::of).toList(), null));
    }

    @OpenApi(path = API_V1 + "/tuners/{key}", methods = HttpMethod.GET, tags = "Tuners", summary = "One tuner",
        pathParams = @OpenApiParam(name = "key", required = true, description = "key from the tuner list"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerView.class)),
            @OpenApiResponse(status = "404")
        })
    private void get(Context ctx)
    {
        ctx.json(TunerView.of(find(ctx.pathParam("key"))));
    }

    @OpenApi(path = API_V1 + "/tuners/{key}/channels", methods = HttpMethod.GET, tags = "Tuners",
        summary = "Channels currently sourced from the tuner, with their offset from the center frequency",
        pathParams = @OpenApiParam(name = "key", required = true, description = "key from the tuner list"),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = TunerChannelView[].class)))
    private void channels(Context ctx)
    {
        ctx.json(new Page<>(RuntimeViews.tunerChannels(find(ctx.pathParam("key"))), null));
    }

    private List<DiscoveredTuner> getTuners()
    {
        if(mTunerManager == null)
        {
            throw ApiException.unavailable("Tuners aren't available");
        }

        return mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners();
    }

    private DiscoveredTuner find(String key)
    {
        return getTuners().stream().filter(tuner -> RuntimeViews.tunerKey(tuner).equals(key)).findFirst()
            .orElseThrow(() -> ApiException.notFound("No tuner with key " + key));
    }
}
