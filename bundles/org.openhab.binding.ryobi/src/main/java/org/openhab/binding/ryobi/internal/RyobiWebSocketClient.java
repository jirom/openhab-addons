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

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiWebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiWebSocketClient.class);
    private static final URI BASE_URI = URI.create("wss://tti.tiwiconnect.com/api/wsrpc");

    private final WebSocketClient webSocketClient;

    public RyobiWebSocketClient() {
        this.webSocketClient = new WebSocketClient();
    }

    public RyobiWebSocket open(final String username, final String apiKey) throws IOException {
        try {
            webSocketClient.start();
        } catch (Exception e) {
            throw new IOException("Failed to start websocket client.", e);
        }

        final RyobiWebSocket socket = new RyobiWebSocket(username, apiKey);
        try {
            webSocketClient.connect(socket, BASE_URI).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }

        return socket;
    }
}
