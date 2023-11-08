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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ryobi.internal.RyobiAccountManager;
import org.openhab.binding.ryobi.internal.RyobiWebSocket;
import org.openhab.binding.ryobi.internal.RyobiWebSocketClient;
import org.openhab.binding.ryobi.internal.config.RyobiAccountConfig;
import org.openhab.binding.ryobi.internal.models.BasicDevice;
import org.openhab.binding.ryobi.internal.models.DetailedDevice;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketDoorRequest;
import org.openhab.binding.ryobi.internal.models.RyobiWebSocketLightRequest;
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

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiAccountHandler extends BaseBridgeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiAccountHandler.class);
    private static final Integer RAPID_REFRESH_SECONDS = 5;
    private static final Integer NORMAL_REFRESH_SECONDS = 60;

    private final RyobiAccountManager accountManager;
    private final RyobiWebSocketClient webSocketClient;
    private @Nullable RyobiAccountConfig config;
    private @Nullable String apiKey;

    private @Nullable Future<?> normalPollFuture;
    private @Nullable Future<?> rapidPollFuture;

    public RyobiAccountHandler(Bridge bridge, RyobiAccountManager ryobiAccountManager,
            RyobiWebSocketClient ryobiWebSocketClient) {
        super(bridge);
        this.accountManager = ryobiAccountManager;
        this.webSocketClient = ryobiWebSocketClient;
    }

    @Override
    public void initialize() {
        LOGGER.info("Initializing ryobi account");

        config = getConfigAs(RyobiAccountConfig.class);
        scheduler.execute(() -> {
            try {
                this.apiKey = accountManager.getApiKey(config.username, config.password);
            } catch (IOException e) {
                LOGGER.error("Could not get api key!", e);
                // TODO Actually check errors properly first!
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Can not access device as username and/or password are invalid");
                return;
            }

            LOGGER.info("Successfully got the api key: {}", apiKey);
            updateStatus(ThingStatus.ONLINE);

            restartPolls(false);
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public boolean updateDoorState(final String garageDoorOpenerId, final OnOffType state) {
        LOGGER.debug("Updating door state to: {}", state);
        final int doorState = state.as(DecimalType.class).intValue();
        try {
            final RyobiWebSocket socket = webSocketClient.open(config.username,
                    accountManager.getApiKey(config.username, config.password));

            final RyobiWebSocketDoorRequest request = new RyobiWebSocketDoorRequest(
                    RyobiWebSocketDoorRequest.DoorState.fromValue(doorState), garageDoorOpenerId);
            socket.sendMessage(request);

            LOGGER.info("Successfully updated door state to: {}", state);
            restartPolls(true);
            return true;
        } catch (IOException | RyobiWebSocket.UnauthenticatedException e) {
            LOGGER.error("Could not update door state.", e);
        }

        return false;
    }

    public boolean updateLightState(final String garageDoorOpenerId, final OnOffType state) {
        LOGGER.debug("Updating light state to: {}", state);
        final boolean isLightOn = state.as(DecimalType.class).intValue() == 1;
        try {
            final RyobiWebSocket socket = webSocketClient.open(config.username,
                    accountManager.getApiKey(config.username, config.password));

            final RyobiWebSocketLightRequest request = new RyobiWebSocketLightRequest(
                    isLightOn ? RyobiWebSocketLightRequest.LightState.ON : RyobiWebSocketLightRequest.LightState.OFF,
                    garageDoorOpenerId);
            socket.sendMessage(request);

            LOGGER.info("Successfully updated light state to: {}", state);
            restartPolls(true);
            return true;
        } catch (IOException | RyobiWebSocket.UnauthenticatedException e) {
            LOGGER.error("Could not update light state.", e);
        }

        return false;
    }

    public List<BasicDevice> getDevices() {
        try {
            return accountManager.getDevices();
        } catch (IOException e) {
            LOGGER.error("Could not get devices! Returning empty collection", e);
            return Collections.emptyList();
        }
    }

    public Optional<DetailedDevice> getDevice(final String deviceId) {
        try {
            return Optional.of(accountManager.getDetailedDevice(deviceId));
        } catch (IOException e) {
            LOGGER.error("Could not get devices! Returning empty collection", e);
            return Optional.empty();
        }
    }

    private void stopPolls() {
        stopNormalPoll();
        stopRapidPoll();
    }

    private synchronized void stopNormalPoll() {
        stopFuture(normalPollFuture);
        normalPollFuture = null;
    }

    private synchronized void stopRapidPoll() {
        stopFuture(rapidPollFuture);
        rapidPollFuture = null;
    }

    private void stopFuture(@Nullable Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private synchronized void restartPolls(boolean rapid) {
        stopPolls();
        if (rapid) {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::normalPoll, 35, NORMAL_REFRESH_SECONDS,
                    TimeUnit.SECONDS);
            rapidPollFuture = scheduler.scheduleWithFixedDelay(this::rapidPoll, 3, RAPID_REFRESH_SECONDS,
                    TimeUnit.SECONDS);
        } else {
            normalPollFuture = scheduler.scheduleWithFixedDelay(this::normalPoll, 0, NORMAL_REFRESH_SECONDS,
                    TimeUnit.SECONDS);
        }
    }

    private void normalPoll() {
        stopRapidPoll();
        fetchData();
    }

    private void rapidPoll() {
        fetchData();
    }

    private synchronized void fetchData() {
        try {
            getDevices();
        } catch (InterruptedException e) {
            // we were shut down, ignore
        }
    }
}
