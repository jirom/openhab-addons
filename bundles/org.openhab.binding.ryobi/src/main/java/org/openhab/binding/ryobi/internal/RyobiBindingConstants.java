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
package org.openhab.binding.ryobi.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RyobiBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiBindingConstants {

    private static final String BINDING_ID = "ryobi";

    // List of all Thing Type UIDs
    public static final ThingTypeUID ACCOUNT_THING_TYPE = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID GARAGE_DOOR_OPENER_THING_TYPE = new ThingTypeUID(BINDING_ID, "garage-door-opener");

    // List of all Channel ids
    public static final String CHANNEL_GARAGE_DOOR = "garage-door";
    public static final String CHANNEL_GARAGE_DOOR_STATE = "garage-door-state";
    public static final String CHANNEL_GARAGE_DOOR_POSITION = "garage-door-position";
    public static final String CHANNEL_GARAGE_ALARM_STATE = "alarm-state";
    public static final String CHANNEL_MOTION_SENSOR = "motion-sensor";
    public static final String CHANNEL_LIGHT = "light";

    // Configuration
    public static final String GARAGE_DOOR_OPENER_ID = "id";
    public static final String ACCOUNT_USERNAME = "username";
    public static final String ACCOUNT_PASSWORD = "password";
}
