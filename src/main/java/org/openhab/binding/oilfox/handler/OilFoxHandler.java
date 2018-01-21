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
    public void onOilFoxRefresh(OilFoxBridgeHandler bridge) {
        try {
            battery(bridge);
            latestMeter(bridge);
            updateStatus(ThingStatus.ONLINE);
        } catch (MalformedURLException e) {
            logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        } catch (IOException e) {
            logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

    }

    public void battery(OilFoxBridgeHandler bridge) throws MalformedURLException, IOException {
        String oilfoxid = this.getThing().getProperties().get(OilFoxBindingConstants.PROPERTY_OILFOXID);
        if (oilfoxid == null) {
            logger.error("OilFoxId is not set in {}", this.getThing().getUID());
            return;
        }

        JsonElement responseObject = bridge.Query("/v1/oilfox/battery/" + oilfoxid);
        logger.info(responseObject.toString());

        if (responseObject.isJsonObject()) {
            JsonObject object = responseObject.getAsJsonObject();
            BigInteger percentage = object.get("percentage").getAsBigInteger();
            this.updateState(OilFoxBindingConstants.CHANNEL_BATTERYLEVEL, DecimalType.valueOf(percentage.toString()));
        }
    }

    public void latestMeter(OilFoxBridgeHandler bridge) throws MalformedURLException, IOException {
        String oilfoxid = this.getThing().getProperties().get(OilFoxBindingConstants.PROPERTY_OILFOXID);
        if (oilfoxid == null) {
            logger.error("OilFoxId is not set in {}", this.getThing().getUID());
            return;
        }

        JsonElement responseObject = bridge.Query("/v1/tank/" + oilfoxid + "/latestmeter");
        logger.info(responseObject.toString());

        if (responseObject.isJsonObject()) {
            JsonObject object = responseObject.getAsJsonObject();
            JsonObject metering = object.get("metering").getAsJsonObject();

            BigDecimal value = metering.get("value").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_VALUE, DecimalType.valueOf(value.toString()));
            BigDecimal fillingpercentage = metering.get("fillingpercentage").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_FILLINGPERCENTAGE,
                    DecimalType.valueOf(fillingpercentage.toString()));
            BigDecimal liters = metering.get("liters").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_LITERS, DecimalType.valueOf(liters.toString()));
            BigDecimal currentOilHeight = metering.get("currentOilHeight").getAsBigDecimal();
            this.updateState(OilFoxBindingConstants.CHANNEL_CURRENTOILHEIGHT,
                    DecimalType.valueOf(currentOilHeight.toString()));
        }
    }

    public void refreshTank(JsonElement tank) {
        BigInteger height = tank.getAsJsonObject().get("height").getAsBigInteger();
        this.updateState(OilFoxBindingConstants.CHANNEL_HEIGHT, DecimalType.valueOf(height.toString()));
        BigInteger volume = tank.getAsJsonObject().get("volume").getAsBigInteger();
        this.updateState(OilFoxBindingConstants.CHANNEL_VOLUME, DecimalType.valueOf(volume.toString()));
        BigInteger offset = tank.getAsJsonObject().get("distanceFromTankToOilFox").getAsBigInteger();
        this.updateState(OilFoxBindingConstants.CHANNEL_OFFSET, DecimalType.valueOf(offset.toString()));
    }

}
