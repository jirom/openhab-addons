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
public class DetailedDevice extends BasicDevice {
    public static final String ATTR_GARAGE_DOOR = "garageDoor_7";
    public static final String ATTR_GARAGE_LIGHT = "garageLight_7";

    public Map<String, Map<String, AttributeValue>> attributes = Map.of();

    public static class AttributeValue {
        public Object value = new Object();
        public long lastSet;
    }
}
