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
public class RyobiWebSocketRequest {
    private final String jsonrpc = "2.0";
    private final String method;
    private final Map<String, Object> params;

    // '{"jsonrpc":"2.0","id":3,"method":"srvWebSocketAuth","params": {"varName": "GDO_EMAIL_GOES_HERE","apiKey":
    // "APIKEY_GOES_HERE"}}'
    public RyobiWebSocketRequest(String method, Map<String, Object> params) {
        this.method = method;
        this.params = params;
    }

    @SerializedName("jsonrpc")
    public String getJsonrpc() {
        return jsonrpc;
    }

    @SerializedName("method")
    public String getMethod() {
        return method;
    }

    @SerializedName("params")
    public Map<String, Object> getParams() {
        return params;
    }
}
