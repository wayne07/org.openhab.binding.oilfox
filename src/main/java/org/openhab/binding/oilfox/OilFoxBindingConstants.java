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
package org.openhab.binding.oilfox;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link OilFoxBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Roland Moser - Initial contribution
 */
@NonNullByDefault
public class OilFoxBindingConstants {

    private static final String BINDING_ID = "oilfox";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_OILFOX = new ThingTypeUID(BINDING_ID, "oilfox");

    // List of all Channel ids
    public static final String CHANNEL_VOLUME = "volume";
    public static final String CHANNEL_HEIGHT = "height";
    public static final String CHANNEL_OFFSET = "offset";

    // List of all Channel ids (read-only)
    public static final String CHANNEL_METERING = "metering";

    // List of all Properties
    public static final String PROPERTY_VERSION = "version";
    public static final String PROPERTY_HARDWAREID = "hardwareId";

    // List of all Configurations
    public static final String CONFIGURATION_ADDRESS = "address";
    public static final String CONFIGURATION_EMAIL = "email";
    public static final String CONFIGURATION_PASSWORD = "password";
    public static final String CONFIGURATION_REFRESH = "refresh";
}
