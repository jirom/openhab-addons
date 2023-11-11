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
public class RyobiWebSocketNotificationSubscriptionRequest extends RyobiWebSocketRequest {
    private static final String TOPIC_SUFFIX = ".wskAttributeUpdateNtfy";

    // See https://github.com/CJOWood/ryobi_garage/blob/main/custom_components/ryobi_garage/ryobiapi.py#L354C15-L354C41
    public RyobiWebSocketNotificationSubscriptionRequest(final String deviceId) {
        super("wskSubscribe", Map.of("topic", deviceId + TOPIC_SUFFIX));
    }
}
