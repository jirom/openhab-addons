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

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
public enum DoorState {
    CLOSED(0),
    OPEN(1),
    CLOSING(2),
    OPENING(3),
    FAULT(4),;

    public final int value;

    DoorState(int value) {
        this.value = value;
    }

    public static DoorState fromValue(double value) {
        return fromValue(Double.valueOf(value).intValue());
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
