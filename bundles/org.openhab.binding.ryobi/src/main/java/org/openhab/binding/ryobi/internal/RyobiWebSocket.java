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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketAuthRequest;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
@NonNullByDefault
public class RyobiWebSocket implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiWebSocket.class);
    private static final long TIMEOUT_SEC = 10;

    private final Gson gson;
    private final CountDownLatch closeLatch;
    private final CountDownLatch authenticateLatch;
    private @Nullable Session session;

    private final String username;
    private final String apiKey;

    private boolean isAuthenticated;

    public RyobiWebSocket(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
        this.closeLatch = new CountDownLatch(1);
        this.authenticateLatch = new CountDownLatch(1);
        this.gson = new Gson();
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOGGER.debug("Connection closed: {} - {}", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOGGER.debug("Got connect: {}", session);
        this.session = session;
        try {
            authenticate();
        } catch (IOException e) {
            LOGGER.error("Could not authenticate", e);
            throw new IllegalStateException(e);
        }
    }

    private void onSuccessfulAuthentication() {
        isAuthenticated = true;
        authenticateLatch.countDown();
        LOGGER.debug("Successfully authenticated.");
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        LOGGER.debug("Got message: {}", message);

        try {
            final JsonElement jsonMessage = JsonParser.parseString(message);

            final JsonElement method = jsonMessage.getAsJsonObject().get("method");
            final JsonElement params = jsonMessage.getAsJsonObject().get("params");
            if (method != null) {
                switch (method.getAsString()) {
                    case "authorizedWebSocket": {
                        if (params != null) {
                            final JsonElement authorized = params.getAsJsonObject().get("authorized");
                            if (authorized.getAsBoolean()) {
                                onSuccessfulAuthentication();
                            } else {
                                LOGGER.error("Could not successfully authenticate. Received response: {}", message);
                            }
                        }
                        break;
                    }
                }
            }

            final JsonElement result = jsonMessage.getAsJsonObject().get("method");
            if (result != null && result.isJsonObject()) {
                final JsonElement authorized = result.getAsJsonObject().get("authorized");
                if (result != null && authorized.isJsonPrimitive()) {
                    if (authorized.getAsBoolean()) {
                        onSuccessfulAuthentication();
                    } else {
                        LOGGER.error("Could not successfully authenticate. Received response: {}", message);
                    }
                }
            }
        } catch (JsonParseException | IllegalStateException e) {
            LOGGER.error("Could not parse websocket response: {}", message, e);
        }

        if (message.contains("Thanks") && session != null) {
            if (this.session != null) {
                session.close(StatusCode.NORMAL, "I'm done");
            }
        }
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        LOGGER.error("WebSocket Error", cause);
    }

    public <T extends RyobiWebSocketRequest> void sendMessage(final T request)
            throws IOException, UnauthenticatedException {
        if (!isAuthenticated && authenticateLatch.getCount() > 0) {
            try {
                boolean isAuthenticationSuccessful = authenticateLatch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
                if (!isAuthenticationSuccessful) {
                    throw new UnauthenticatedException("Timed out waiting to authenticate");
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Authentication was interrupted.. Probably shuttin down?", e);
            }
        }

        if (isAuthenticated && session != null) {
            session.getRemote().sendString(gson.toJson(request));
        } else {
            throw new UnauthenticatedException("Lost authentication status. Must call authenticate() again.");
        }
    }

    private void authenticate() throws IOException {
        final RyobiWebSocketAuthRequest authRequest = new RyobiWebSocketAuthRequest(username, apiKey);
        session.getRemote().sendString(gson.toJson(authRequest));
    }

    public static class UnauthenticatedException extends Exception {
        public UnauthenticatedException(final String message) {
            super(message);
        }

        public UnauthenticatedException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.session != null) {
            this.session.close();
        }
    }
}
