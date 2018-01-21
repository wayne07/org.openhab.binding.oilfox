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

import java.math.BigDecimal;

/**
 * The {@link OilFoxConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Roland Moser - Initial contribution
 */
public class OilFoxConfiguration {

    public String address;
    public String email;
    public String password;
    public BigDecimal refresh;
}
