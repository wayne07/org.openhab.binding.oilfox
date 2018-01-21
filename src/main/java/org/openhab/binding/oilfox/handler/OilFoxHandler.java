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

import static org.openhab.binding.oilfox.OilFoxBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.oilfox.internal.OilFoxCommunication;
import org.openhab.binding.oilfox.internal.OilFoxConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private OilFoxCommunication communication = new OilFoxCommunication();

    private ScheduledFuture<?> refreshJob;

    public OilFoxHandler(Thing thing) {
        super(thing);
    }

    private void ReadStatus() {
        synchronized (communication) {
            if (getThing().getStatus() == ThingStatus.OFFLINE) {
                login();
            }

            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }

            try {
                String address = (String) getThing().getConfiguration().get(CONFIGURATION_ADDRESS);

                Properties prop = communication.Summary(address);
                this.updateProperty(PROPERTY_VERSION, prop.getProperty("JSON_VERSION"));

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
        // config = getConfigAs(OilFoxConfiguration.class);
        login();
        startAutomaticRefresh();
    }

    private void startAutomaticRefresh() {
        BigDecimal refresh = (BigDecimal) getThing().getConfiguration().get(CONFIGURATION_REFRESH);

        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            ReadStatus();
        }, 0, refresh.longValue(), TimeUnit.HOURS);
    }

    private void login() {
        String address = (String) getThing().getConfiguration().get(CONFIGURATION_ADDRESS);
        String email = (String) getThing().getConfiguration().get(CONFIGURATION_EMAIL);
        String password = (String) getThing().getConfiguration().get(CONFIGURATION_PASSWORD);

        try {
            communication.Login(address, email, password);

            updateStatus(ThingStatus.ONLINE);
        } catch (IOException e) {
            logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
}
