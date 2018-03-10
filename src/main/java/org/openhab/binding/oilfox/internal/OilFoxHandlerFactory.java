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
package org.openhab.binding.oilfox.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.oilfox.OilFoxBindingConstants;
import org.openhab.binding.oilfox.handler.OilFoxBridgeHandler;
import org.openhab.binding.oilfox.handler.OilFoxHandler;
import org.openhab.binding.oilfox.internal.discovery.OilFoxDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link OilFoxHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Roland Moser - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.oilfox")
@NonNullByDefault
public class OilFoxHandlerFactory extends BaseThingHandlerFactory {

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return OilFoxBindingConstants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(@Nullable Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (OilFoxBindingConstants.SUPPORTED_BRIDGE_TYPES.contains(thingTypeUID)) {
            OilFoxBridgeHandler handler = new OilFoxBridgeHandler((Bridge) thing);
            registerOilFoxDiscoveryService(handler);
            return handler;
        } else if (thingTypeUID.equals(OilFoxBindingConstants.THING_TYPE_OILFOX)) {
            return new OilFoxHandler(thing);
        }
        return null;
    }

    @Override
    protected synchronized void removeHandler(@NonNull ThingHandler thingHandler) {
        if (thingHandler instanceof OilFoxBridgeHandler) {
            ServiceRegistration<?> serviceReg = discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                OilFoxDiscoveryService discoveryService = (OilFoxDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                discoveryService.deactivate();
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private synchronized void registerOilFoxDiscoveryService(OilFoxBridgeHandler bridgeHandler) {
        OilFoxDiscoveryService discoveryService = new OilFoxDiscoveryService(bridgeHandler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }
}
