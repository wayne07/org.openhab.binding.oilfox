/**
 * Copyright (c) 2018,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.oilfox.handler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.oilfox.internal.OilFoxConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link OilFoxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Roland Moser - Initial contribution
 */
@NonNullByDefault
public class OilFoxHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OilFoxHandler.class);

    @Nullable
    private OilFoxConfiguration config;

    private ScheduledFuture<?> refreshJob;

    public OilFoxHandler(Thing thing) {
        super(thing);
    }

    private void ReadStatus() {
        synchronized (this) {
            if (getThing().getStatus() == ThingStatus.OFFLINE) {
                login();
            }

            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }

            try {
                summary();
                updateStatus(ThingStatus.ONLINE);
            } catch (MalformedURLException e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            } catch (IOException e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO: currently not implemented to change any configuration
    }

    @Override
    public void initialize() {
        config = getConfigAs(OilFoxConfiguration.class);
        synchronized (this) {
            // cancel old job
            if (refreshJob != null) {
                refreshJob.cancel(false);
            }

            login();

            refreshJob = scheduler.scheduleWithFixedDelay(() -> {
                ReadStatus();
            }, 0, config.refresh.longValue(), TimeUnit.HOURS);
        }
    }

    // communication with OilFox Cloud

    // TBD: store token
    private String token;

    protected JsonElement Query(String address) throws MalformedURLException, IOException {
        return Query(address, JsonNull.INSTANCE);
    }

    @SuppressWarnings("null")
    protected JsonElement Query(String address, JsonElement requestObject) throws MalformedURLException, IOException {
        URL url = new URL(address);
        logger.info("Query({})", url.toString());
        HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
        request.setReadTimeout(10000);
        request.setConnectTimeout(15000);
        request.setRequestProperty("Content-Type", "application/json");
        request.setDoInput(true);
        if (requestObject == JsonNull.INSTANCE) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                throw new IOException("Not logged in");
            }

            request.setRequestProperty("X-Auth-Token", token);
        } else {
            request.setRequestMethod("POST");
            request.setDoOutput(true);

            OutputStream os = request.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(requestObject.toString());
            writer.flush();
            writer.close();
            os.close();
        }

        request.connect();

        switch (request.getResponseCode()) {
            case 401:
                throw new IOException("Unauthorized");
            case 200:
                // authorized
            default:
                Reader reader = new InputStreamReader(request.getInputStream(), "UTF-8");
                JsonParser parser = new JsonParser();
                JsonElement element = parser.parse(reader);
                reader.close();
                return element;
        }
    }

    private void login() {
        try {
            JsonObject requestObject = new JsonObject();
            requestObject.addProperty("email", config.email);
            requestObject.addProperty("password", config.password);

            JsonElement responseObject = Query("https://" + config.address + "/v1/user/login", requestObject);

            if (responseObject.isJsonObject()) {
                JsonObject object = responseObject.getAsJsonObject();
                token = object.get("token").getAsString();
                logger.info("Token " + token);
            }

            updateStatus(ThingStatus.ONLINE);
        } catch (IOException e) {
            logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public void summary() throws MalformedURLException, IOException {
        JsonElement responseObject = Query("https://" + config.address + "/v1/user/summary");
        logger.info(responseObject.toString());

        /*
         * if (element.isJsonObject()) {
         * JsonObject object = responseObject.getAsJsonObject();
         * String metering = object.get("metering").getAsString();
         * this.updateProperty(CHANNEL_METERING, metering);
         * logger.info("Metering " + metering);
         * }
         */
    }
}
