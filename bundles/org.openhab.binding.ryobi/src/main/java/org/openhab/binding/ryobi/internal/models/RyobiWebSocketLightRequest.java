/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.ryobi.internal.models;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiWebSocketLightRequest extends RyobiWebSocketRequest {
    public enum LightState {
        ON,
        OFF
    }

    // {"jsonrpc":"2.0","method":"gdoModuleCommand","params":{"msgType":16,"moduleType":5,"portId":7,"moduleMsg":{"lightState":<boolean
    // light state>},"topic":"GARAGEDOOR_ID"}}
    public RyobiWebSocketLightRequest(final LightState lightState, final String deviceId) {
        super("gdoModuleCommand", Map.of("msgType", 16, "moduleType", 5, "portId", 7, "topic", deviceId, "moduleMsg",
                Map.of("lightState", lightState == LightState.ON)));
    }
}
