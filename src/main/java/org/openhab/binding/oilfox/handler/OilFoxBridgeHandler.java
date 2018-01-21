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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.oilfox.OilFoxBindingConstants;
import org.openhab.binding.oilfox.internal.OilFoxBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link OilFoxBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Roland Moser - Initial contribution
 */
@NonNullByDefault
public class OilFoxBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(OilFoxBridgeHandler.class);

    private OilFoxBridgeConfiguration config;

    private ScheduledFuture<?> refreshJob;

    private List<OilFoxStatusListener> oilFoxStatusListeners = new CopyOnWriteArrayList<>();

    public OilFoxBridgeHandler(Bridge bridge) {
        super(bridge);
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

                for (OilFoxStatusListener oilFoxStatusListener : oilFoxStatusListeners) {
                    oilFoxStatusListener.onOilFoxRefresh(this);
                }
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
        config = getConfigAs(OilFoxBridgeConfiguration.class);
        synchronized (this) {
            // cancel old job
            if (refreshJob != null) {
                refreshJob.cancel(false);
            }

            login();

            refreshJob = scheduler.scheduleWithFixedDelay(() -> {
                ReadStatus();
            }, 0, config.refresh.longValue(), TimeUnit.MINUTES);
        }
    }

    // communication with OilFox Cloud

    // TBD: store token
    private String token;

    protected JsonElement Query(String address) throws MalformedURLException, IOException {
        return Query(address, JsonNull.INSTANCE);
    }

    @SuppressWarnings("null")
    protected JsonElement Query(String path, JsonElement requestObject) throws MalformedURLException, IOException {
        URL url = new URL("https://" + config.address + path);
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

            JsonElement responseObject = Query("/v1/user/login", requestObject);

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
        JsonElement responseObject = Query("/v1/user/summary");
        logger.info(responseObject.toString());

        if (responseObject.isJsonObject()) {
            JsonObject object = responseObject.getAsJsonObject();
            JsonArray oilfoxes = object.get("oilfoxes").getAsJsonArray();
            for (JsonElement oilfox : oilfoxes) {
                String id = oilfox.getAsJsonObject().get("id").getAsString();
                String name = oilfox.getAsJsonObject().get("name").getAsString();
                String hwid = oilfox.getAsJsonObject().get("hwid").getAsString();
                for (OilFoxStatusListener oilFoxStatusListener : oilFoxStatusListeners) {
                    try {
                        oilFoxStatusListener.onOilFoxAdded(this.getThing().getUID(), name, id, hwid);
                    } catch (Exception e) {
                        logger.error("An exception occurred while calling the BridgeHeartbeatListener", e);
                    }
                }
            }

            JsonArray tanks = object.get("tanks").getAsJsonArray();
            for (Thing thing : getThing().getThings()) {
                String oilfoxid = thing.getProperties().get(OilFoxBindingConstants.PROPERTY_OILFOXID);
                if (oilfoxid == null) {
                    logger.error("OilFoxId is not set in {}", thing.getUID());
                    return;
                }

                for (JsonElement tank : tanks) {
                    String id = tank.getAsJsonObject().get("id").getAsString();
                    if (oilfoxid.equals(id)) {
                        ((OilFoxHandler) thing.getHandler()).refreshTank(tank);
                        break;
                    }
                }
            }
        }
    }

    public boolean registerOilFoxStatusListener(@Nullable OilFoxStatusListener oilFoxStatusListener) {
        if (oilFoxStatusListener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null LightStatusListener.");
        }
        return oilFoxStatusListeners.add(oilFoxStatusListener);
    }

    public boolean unregisterOilFoxStatusListener(OilFoxStatusListener oilFoxStatusListener) {
        return oilFoxStatusListeners.remove(oilFoxStatusListener);
    }
}