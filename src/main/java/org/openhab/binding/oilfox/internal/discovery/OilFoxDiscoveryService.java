package org.openhab.binding.oilfox.internal.discovery;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.oilfox.OilFoxBindingConstants;
import org.openhab.binding.oilfox.handler.OilFoxBridgeHandler;
import org.openhab.binding.oilfox.handler.OilFoxStatusListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

public class OilFoxDiscoveryService extends AbstractDiscoveryService implements OilFoxStatusListener {
    private final Logger logger = LoggerFactory.getLogger(OilFoxDiscoveryService.class);

    private final static int SEARCH_TIME = 60;

    private ServiceRegistration<?> reg = null;

    private OilFoxBridgeHandler oilFoxBridgeHandler;

    public OilFoxDiscoveryService(OilFoxBridgeHandler oilFoxBridgeHandler) {
        super(OilFoxBindingConstants.SUPPORTED_DEVICE_TYPES, SEARCH_TIME);
        this.oilFoxBridgeHandler = oilFoxBridgeHandler;
    }

    public void activate() {
        oilFoxBridgeHandler.registerOilFoxStatusListener(this);
    }

    @Override
    public void deactivate() {
        oilFoxBridgeHandler.unregisterOilFoxStatusListener(this);
    }

    @Override
    protected void startScan() {
        try {
            oilFoxBridgeHandler.summary(false);
        } catch (MalformedURLException e) {
            logger.error("Exception occurred during execution: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Exception occurred during execution: {}", e.getMessage(), e);
        }
    }

    public void start(BundleContext bundleContext) {
        if (reg != null) {
            return;
        }
        reg = bundleContext.registerService(DiscoveryService.class.getName(), this, new Hashtable<String, Object>());
    }

    public void stop() {
        if (reg != null) {
            reg.unregister();
        }
        reg = null;
    }

    @Override
    public void onOilFoxRemoved(ThingUID bridge, String oilfox) {
    }

    @Override
    public void onOilFoxAdded(ThingUID bridge, String name, String id, String hwid) {
	String n = name;
	if (n == null) {
	    n = "Oilfox " + id;
	}

        ThingTypeUID uid = OilFoxBindingConstants.THING_TYPE_OILFOX;
        ThingUID thingUID = new ThingUID(uid, bridge, id);
        if (thingUID != null) {
            logger.trace("Discovered new oilfox {}.", id);

            Map<String, Object> properties = new HashMap<>(3);
            properties.put(OilFoxBindingConstants.PROPERTY_VERSION, "unknown");
            properties.put(OilFoxBindingConstants.PROPERTY_OILFOXID, id);
            properties.put(OilFoxBindingConstants.PROPERTY_OILFOXHWID, hwid);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withLabel(n).withBridge(bridge)
                    .withProperties(properties).build();
            thingDiscovered(discoveryResult);
        }
    }

    @Override
    public void onOilFoxRefresh(JsonArray devices) {
    }
}
