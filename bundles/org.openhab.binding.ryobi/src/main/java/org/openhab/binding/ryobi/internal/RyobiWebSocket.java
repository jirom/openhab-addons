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

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
@NonNullByDefault
public class RyobiWebSocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiWebSocket.class);

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

    @OnWebSocketMessage
    public void onMessage(String message) {
        LOGGER.debug("Got message: {}", message);

        if (message.contains("Thanks") && session != null) {
            session.close(StatusCode.NORMAL, "I'm done");
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
                authenticateLatch.await();
            } catch (InterruptedException e) {
                throw new UnauthenticatedException("Timed out waiting to authenticate", e);
            }
        }

        if (isAuthenticated && session != null) {
            session.getRemote().sendString(gson.toJson(request));
        } else {
            throw new UnauthenticatedException("Lost authentication status. Must call authenticate() again.");
        }
    }

    public void authenticate() throws IOException {
        if (session == null) {
            return;
        }

        final RyobiWebSocketAuthRequest authRequest = new RyobiWebSocketAuthRequest(username, apiKey);
        session.getRemote().sendString(gson.toJson(authRequest));
        isAuthenticated = true;
        authenticateLatch.countDown();
    }

    public static class UnauthenticatedException extends Exception {
        public UnauthenticatedException(final String message) {
            super(message);
        }

        public UnauthenticatedException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
