package org.openhab.binding.oilfox.internal.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.oilfox.OilFoxBindingConstants;
import org.openhab.binding.oilfox.handler.OilFoxBridgeHandler;
import org.openhab.binding.oilfox.handler.OilFoxStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OilFoxDiscoveryService extends AbstractDiscoveryService implements OilFoxStatusListener {
    private final Logger logger = LoggerFactory.getLogger(OilFoxDiscoveryService.class);

    private final static int SEARCH_TIME = 60;

    private OilFoxBridgeHandler oilFoxBridgeHandler;

    public OilFoxDiscoveryService(OilFoxBridgeHandler oilFoxBridgeHandler) {
        super(SEARCH_TIME);
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
        // TODO Auto-generated method stub

    }

    @Override
    public void onOilFoxRemoved(ThingUID bridge, String oilfox) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onOilFoxAdded(ThingUID bridge, String name, String id, String hwid) {
        ThingTypeUID uid = OilFoxBindingConstants.THING_TYPE_OILFOX;
        ThingUID thingUID = new ThingUID(uid, bridge, id);
        if (thingUID != null) {
            logger.trace("Discovered new oilfox {}.", id);

            Map<String, Object> properties = new HashMap<>(3);
            properties.put(OilFoxBindingConstants.PROPERTY_VERSION, "unknown");
            properties.put(OilFoxBindingConstants.PROPERTY_OILFOXID, id);
            properties.put(OilFoxBindingConstants.PROPERTY_OILFOXHWID, hwid);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withLabel(name).withBridge(bridge)
                    .withProperties(properties).build();
            thingDiscovered(discoveryResult);
        }
    }

    @Override
    public void onOilFoxRefresh(OilFoxBridgeHandler bridge) {
    }
}
