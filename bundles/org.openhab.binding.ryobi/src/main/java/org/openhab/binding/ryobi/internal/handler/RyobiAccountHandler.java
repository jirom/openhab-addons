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
package org.openhab.binding.ryobi.internal.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.ryobi.internal.RyobiAccountManager;
import org.openhab.binding.ryobi.internal.RyobiWebSocket;
import org.openhab.binding.ryobi.internal.RyobiWebSocket.Callback;
import org.openhab.binding.ryobi.internal.RyobiWebSocket.UnauthenticatedException;
import org.openhab.binding.ryobi.internal.RyobiWebSocketClient;
import org.openhab.binding.ryobi.internal.config.RyobiAccountConfig;
import org.openhab.binding.ryobi.internal.models.BasicDevice;
import org.openhab.binding.ryobi.internal.models.DetailedDevice;
import org.openhab.binding.ryobi.internal.models.DetailedDevice.AttributeValue;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketDoorRequest;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketLightRequest;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketRequest;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
public class RyobiAccountHandler extends BaseBridgeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiAccountHandler.class);

    private final RyobiAccountManager accountManager;
    private final RyobiWebSocketClient webSocketClient;
    private final WebSocketCallbackHandler webSocketCallbackHandler;
    private @Nullable RyobiAccountConfig config;
    private Optional<String> apiKey = Optional.empty();
    private WebSocketSupplier webSocketSupplier = new WebSocketSupplier();

    @NonNullByDefault
    public interface DeviceUpdateListener {
        String getDeviceId();

        void onDeviceUpdate(final String deviceType, final AttributeValue attributeValue);
    }

    private final RetryListener retryListener = new RetryListener() {
        @Override
        public <V> void onRetry(@Nullable Attempt<V> attempt) {
            if (attempt.hasException()) {
                LOGGER.warn("Call failed. Attempting to reauthenticate first.", attempt.getExceptionCause());
                reauthenticate();
                webSocketSupplier.reset();
            } else {
                LOGGER.debug("Call succeeded after {} attempts.", attempt.getAttemptNumber());
            }
        }
    };

    public RyobiAccountHandler(Bridge bridge, RyobiAccountManager ryobiAccountManager,
            RyobiWebSocketClient ryobiWebSocketClient) {
        super(bridge);
        this.accountManager = ryobiAccountManager;
        this.webSocketClient = ryobiWebSocketClient;
        this.webSocketCallbackHandler = new WebSocketCallbackHandler();
    }

    public void addListener(final DeviceUpdateListener deviceUpdateListener) throws IOException {
        webSocketSupplier.addListener(deviceUpdateListener);
    }

    public void removeListener(final DeviceUpdateListener deviceUpdateListener) {
        webSocketSupplier.removeListener(deviceUpdateListener);
    }

    private class WebSocketSupplier implements Supplier<RyobiWebSocket> {
        private List<DeviceUpdateListener> deviceUpdateListeners = new ArrayList<>();
        private @Nullable RyobiWebSocket webSocket = null;

        @Override
        public synchronized RyobiWebSocket get() {
            if (webSocket == null) {
                try {
                    webSocket = webSocketClient.open(config.username, getApiKey(), webSocketCallbackHandler);
                    for (final DeviceUpdateListener listener : deviceUpdateListeners) {
                        subscribe(listener.getDeviceId());
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Could not open websocket connection", e);
                }
            }

            return webSocket;
        }

        public void addListener(final DeviceUpdateListener deviceUpdateListener) throws IOException {
            deviceUpdateListeners.add(deviceUpdateListener);
            if (webSocket != null) {
                subscribe(deviceUpdateListener.getDeviceId());
            } else {
                LOGGER.debug("There is no websocket is subscription will be added on connection");
            }
        }

        public void removeListener(final DeviceUpdateListener deviceUpdateListener) {
            deviceUpdateListeners.remove(deviceUpdateListener);
        }

        private void subscribe(final String deviceId) throws IOException {
            final Retryer<Void> retryer = createRetryer();
            try {
                retryer.call(() -> {
                    webSocket.subscribeDevice(deviceId);
                    return null;
                });
            } catch (ExecutionException | RetryException e) {
                throw new IOException("Failed to subscribe device.", e);
            }
        }

        public synchronized void reset() {
            this.webSocket = null;
        }
    }

    @Override
    public void initialize() {
        LOGGER.info("Initializing ryobi account");

        config = getConfigAs(RyobiAccountConfig.class);
        scheduler.execute(() -> {
            try {
                this.apiKey = Optional.of(getApiKey());
            } catch (IOException e) {
                LOGGER.error("Could not get api key!", e);
                // TODO Actually check errors properly first!
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Can not access device as username and/or password are invalid");
                return;
            }

            LOGGER.info("Successfully got the api key: {}", apiKey);
            updateStatus(ThingStatus.ONLINE);
        });
    }

    private synchronized String getApiKey() throws IOException {
        if (!apiKey.isPresent()) {
            apiKey = Optional.of(accountManager.getApiKey(config.username, config.password));
        }

        return apiKey.orElseThrow();
    }

    private void reauthenticate() {
        try {
            this.apiKey = Optional.of(getApiKey());
        } catch (IOException e) {
            LOGGER.error("Could not get api key!", e);
            // TODO Actually check errors properly first!
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Can not access device as username and/or password are invalid");
            return;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private void sendMessage(final RyobiWebSocketRequest request) throws ExecutionException, RetryException {
        final Retryer<Void> retryer = createRetryer();
        retryer.call(() -> {
            webSocketSupplier.get().sendMessage(request);
            return null;
        });
    }

    public boolean updateDoorState(final String garageDoorOpenerId, final OnOffType state) {
        LOGGER.debug("Updating door state to: {}", state);
        final int doorState = state.as(DecimalType.class).intValue();

        try {
            final RyobiWebSocketDoorRequest request = new RyobiWebSocketDoorRequest(
                    RyobiWebSocketDoorRequest.DoorState.fromValue(doorState), garageDoorOpenerId);
            sendMessage(request);

            LOGGER.info("Successfully updated door state to: {}", state);
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not update door state.", e);
        }

        return false;
    }

    public boolean updateLightState(final String garageDoorOpenerId, final OnOffType state) {
        LOGGER.debug("Updating light state to: {}", state);
        final boolean isLightOn = state.as(DecimalType.class).intValue() == 1;
        try {
            final RyobiWebSocketLightRequest request = new RyobiWebSocketLightRequest(
                    isLightOn ? RyobiWebSocketLightRequest.LightState.ON : RyobiWebSocketLightRequest.LightState.OFF,
                    garageDoorOpenerId);
            sendMessage(request);

            LOGGER.info("Successfully updated light state to: {}", state);
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not update light state.", e);
        }

        return false;
    }

    public List<BasicDevice> getDevices() {
        final Retryer<List<BasicDevice>> retryer = createRetryer();

        try {
            return retryer.call(() -> accountManager.getDevices());
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Could not get devices! Returning empty collection", e);
            return Collections.emptyList();
        }
    }

    public Optional<DetailedDevice> getDevice(final String deviceId) {
        final Retryer<Optional<DetailedDevice>> retryer = createRetryer();

        try {
            return retryer.call(() -> Optional.of(accountManager.getDetailedDevice(deviceId)));
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Could not get devices! Returning nothing", e);
            return Optional.empty();
        }
    }

    private <T> Retryer<T> createRetryer() {
        return RetryerBuilder.<T> newBuilder().retryIfExceptionOfType(IOException.class)
                .retryIfExceptionOfType(UnauthenticatedException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.exponentialWait(10, TimeUnit.SECONDS)).withRetryListener(retryListener)
                .build();
    }

    private class WebSocketCallbackHandler implements Callback {
        @Override
        public void onDeviceUpdate(String deviceId, String deviceType, AttributeValue attributeValue) {
            webSocketSupplier.deviceUpdateListeners.forEach(listener -> {
                if (listener.getDeviceId().equals(deviceId)) {
                    listener.onDeviceUpdate(deviceType, attributeValue);
                }
            });
        }

        @Override
        public synchronized void onError(Throwable e) {
            LOGGER.error("Received socket error. Disposing current socket", e);
            apiKey = Optional.empty();
            webSocketSupplier.reset();
        }
    }
}
