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

import static org.openhab.binding.ryobi.internal.RyobiBindingConstants.CHANNEL_GARAGE_DOOR;
import static org.openhab.binding.ryobi.internal.RyobiBindingConstants.CHANNEL_LIGHT;

import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ryobi.internal.RyobiBindingConstants;
import org.openhab.binding.ryobi.internal.config.RyobiGarageDoorConfig;
import org.openhab.binding.ryobi.internal.models.DetailedDevice;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiGarageDoorHandler extends BaseThingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiGarageDoorHandler.class);

    private @Nullable RyobiGarageDoorConfig config;

    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param thing the thing that should be handled, not null
     */
    public RyobiGarageDoorHandler(Thing thing) {
        super(thing);
    }

    private void refreshStatus() {
        final RyobiAccountHandler accountHandler = (RyobiAccountHandler) getBridge().getHandler();
        if (accountHandler == null) {
            LOGGER.error("Could not find account handler. Not refreshing.");
            return;
        }

        final Optional<DetailedDevice> detailedDevice = accountHandler.getDevice(getId());

        if (!detailedDevice.isPresent()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Could not get state for device: " + getId());
            return;
        }

        accountHandler.getDevice(getId()).ifPresent(device -> {
            final Map<String, DetailedDevice.AttributeValue> garageDoorAttributes = device.attributes
                    .get(DetailedDevice.ATTR_GARAGE_DOOR);
            final OnOffType doorState = OnOffType.from(((double) garageDoorAttributes.get("doorState").value) == 1);
            updateState(CHANNEL_GARAGE_DOOR, doorState);

            final Map<String, DetailedDevice.AttributeValue> lightAttributes = device.attributes
                    .get(DetailedDevice.ATTR_GARAGE_LIGHT);
            final OnOffType lightState = OnOffType.from(((boolean) lightAttributes.get("lightState").value));
            updateState(CHANNEL_GARAGE_DOOR, lightState);
        });
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        LOGGER.debug("Received command:{} for channel:{}", command, channelUID);

        final RyobiAccountHandler accountHandler = (RyobiAccountHandler) getBridge().getHandler();
        if (accountHandler == null) {
            return;
        }

        if (command instanceof RefreshType) {
            refreshStatus();
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_GARAGE_DOOR: {
                if (command instanceof OnOffType) {
                    final OnOffType newState = (OnOffType) command;
                    final boolean updatedSuccessfully = accountHandler.updateDoorState(getId(), newState);
                    if (updatedSuccessfully) {
                        updateState(CHANNEL_GARAGE_DOOR, newState);
                    }
                } else {
                    LOGGER.error("Unknown command:{} for channel:{}", command, channelUID);
                }

                break;
            }

            case CHANNEL_LIGHT: {
                if (command instanceof OnOffType) {
                    final OnOffType newState = (OnOffType) command;
                    final boolean updatedSuccessfully = accountHandler.updateLightState(getId(), newState);
                    if (updatedSuccessfully) {
                        updateState(CHANNEL_LIGHT, newState);
                    }
                } else {
                    LOGGER.error("Unknown command:{} for channel:{}", command, channelUID);
                }

                break;
            }

            default: {
                LOGGER.warn("Unknown channel: {}", channelUID);
            }
        }

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information:
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
    }

    @Override
    public void initialize() {
        config = getConfigAs(RyobiGarageDoorConfig.class);
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            refreshStatus();
        });
    }

    private String getId() {
        if (config != null && config.id != null && !config.id.isEmpty()) {
            return config.id;
        }

        final String idFromProperties = getThing().getProperties().get(RyobiBindingConstants.GARAGE_DOOR_OPENER_ID);
        if (idFromProperties != null && !idFromProperties.isEmpty()) {
            return idFromProperties;
        }

        throw new IllegalStateException("Unknown device id!");
    }
}
