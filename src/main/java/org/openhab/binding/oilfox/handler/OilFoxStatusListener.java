/**
 * The {@link OilFoxStatusListener} is notified when an OilFox is added to or removed from the account.
 *
 *
 */
package org.openhab.binding.oilfox.handler;

import org.eclipse.smarthome.core.thing.ThingUID;

import com.google.gson.JsonArray;

public interface OilFoxStatusListener {
    /**
     * This method is called whenever an OilFox is removed.
     *
     * @param bridge The bridge the removed OilFox was connected to.
     * @param oilfox The OilFox which is removed.
     */
    void onOilFoxRemoved(ThingUID bridge, String oilfox);

    /**
     * This method is called whenever an OilFox is added.
     *
     * @param bridge The bridge the added OilFox was connected to.
     * @param name The OilFox which is added.
     * @param id The OilFox which is added.
     * @param hwid The OilFox which is added.
     */
    void onOilFoxAdded(ThingUID bridge, String name, String id, String hwid);

    void onOilFoxRefresh(JsonArray devices);
}
