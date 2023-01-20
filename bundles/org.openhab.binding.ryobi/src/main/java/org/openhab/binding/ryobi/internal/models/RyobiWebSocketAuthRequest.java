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

import com.google.gson.annotations.SerializedName;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiWebSocketAuthRequest extends RyobiWebSocketRequest {
    // '{"jsonrpc":"2.0","id":3,"method":"srvWebSocketAuth","params": {"varName": "GDO_EMAIL_GOES_HERE","apiKey":
    // "APIKEY_GOES_HERE"}}'
    public RyobiWebSocketAuthRequest(final String username, final String apiKey) {
        super("srvWebSocketAuth", Map.of("varName", username, "apiKey", apiKey));
    }

    @SerializedName("id")
    public int getId() {
        return 3;
    }
}
