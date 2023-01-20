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
public class RyobiWebSocketDoorRequest extends RyobiWebSocketRequest {
    public enum DoorState {
        OPEN(1),
        CLOSE(0);

        private final int value;

        DoorState(int value) {
            this.value = value;
        }

        public static DoorState fromValue(int value) {
            for (DoorState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }

            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    // {"jsonrpc":"2.0","method":"gdoModuleCommand","params":{"msgType":16,"moduleType":5,"portId":7,"moduleMsg":{"doorCommand":1},"topic":"GARAGEDOOR_ID"}}
    public RyobiWebSocketDoorRequest(final DoorState doorState, final String deviceId) {
        super("gdoModuleCommand", Map.of("msgType", 16, "moduleType", 5, "portId", 7, "topic", deviceId, "moduleMsg",
                Map.of("doorCommand", doorState.value)));
    }
}
