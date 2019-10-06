package org.openhab.binding.oilfox.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.oilfox.OilFoxBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OilFoxHandler extends BaseThingHandler implements OilFoxStatusListener {

    private final Logger logger = LoggerFactory.getLogger(OilFoxHandler.class);

    public OilFoxHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initializeThing((getBridge() == null) ? null : getBridge().getStatus());
    }

    private void initializeThing(ThingStatus bridgeStatus) {
        String oilfoxid = this.getThing().getProperties().get(OilFoxBindingConstants.PROPERTY_OILFOXID);
        if (oilfoxid == null) {
            logger.error("OilFoxId is not set in {}", this.getThing().getUID());
            return;
        }

        logger.debug("initializeThing thing {} bridge status {}", getThing().getUID(), bridgeStatus);

        if (getBridge() != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                ((OilFoxBridgeHandler) this.getBridge().getHandler()).registerOilFoxStatusListener(this);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
    }

    @Override
    public void onOilFoxRemoved(ThingUID bridge, String oilfox) {
    }

    @Override
    public void onOilFoxAdded(ThingUID bridge, String name, String id, String hwid) {
    }

    @Override
    public void onOilFoxRefresh(JsonArray devices) {
        String oilfoxid = this.getThing().getProperties().get(OilFoxBindingConstants.PROPERTY_OILFOXID);
        for (JsonElement device : devices) {
            if (!device.isJsonObject())
                continue;

            JsonObject object = device.getAsJsonObject();

            String deviceid = object.get("id").getAsString();
            if (!oilfoxid.equals(deviceid) )
                continue;

            BigInteger tankHeight = object.get("tankHeight").getAsBigInteger();
            this.updateState(OilFoxBindingConstants.CHANNEL_HEIGHT, DecimalType.valueOf(tankHeight.toString()));
            BigInteger tankVolume = object.get("tankVolume").getAsBigInteger();
            this.updateState(OilFoxBindingConstants.CHANNEL_VOLUME, DecimalType.valueOf(tankVolume.toString()));
            BigInteger tankOffset = object.get("tankOffset").getAsBigInteger();
            this.updateState(OilFoxBindingConstants.CHANNEL_OFFSET, DecimalType.valueOf(tankOffset.toString()));
            //TODO: "tankShape" : "SQUARED"
            //TODO: "tankIsUsableVolume": false
            //TODO: "tankUsableVolume": 1000
            //TODO: "productId": "UUID"
            //TODO: "notificationInfoEnabled": true,
            //TODO: "notificationInfoPercentage": 25,
            //TODO: "notificationAlertEnabled": true,
            //TODO: "notificationAlertPercentage": 15,
            //TODO: "measurementIntervalInSeconds": 86400

            JsonObject metering = object.get("metering").getAsJsonObject();
            BigDecimal value = metering.get("value").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_VALUE, DecimalType.valueOf(value.toString()));
            BigDecimal fillingpercentage = metering.get("fillingPercentage").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_FILLINGPERCENTAGE,
                DecimalType.valueOf(fillingpercentage.toString()));
            BigDecimal liters = metering.get("liters").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_LITERS, DecimalType.valueOf(liters.toString()));
            BigDecimal currentOilHeight = metering.get("currentOilHeight").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_CURRENTOILHEIGHT,
                DecimalType.valueOf(currentOilHeight.toString()));
            //TODO: "serverDate": 1568035451021
            BigInteger battery = metering.get("battery").getAsBigInteger();
            this.updateState(OilFoxBindingConstants.CHANNEL_BATTERYLEVEL, DecimalType.valueOf(battery.toString()));

            //TODO: "address"
            //TODO: "partner"
            //TODO: "chartData"
            updateStatus(ThingStatus.ONLINE);
            return;
        }

        // Oilfox not found
        updateStatus(ThingStatus.OFFLINE);
    }
}
