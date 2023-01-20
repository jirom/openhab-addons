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
package org.openhab.binding.ryobi.internal.factory;

import static org.openhab.binding.ryobi.internal.RyobiBindingConstants.ACCOUNT_THING_TYPE;
import static org.openhab.binding.ryobi.internal.RyobiBindingConstants.GARAGE_DOOR_OPENER_THING_TYPE;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.ryobi.internal.RyobiAccountManager;
import org.openhab.binding.ryobi.internal.RyobiWebSocketClient;
import org.openhab.binding.ryobi.internal.discovery.RyobiDeviceDiscoveryService;
import org.openhab.binding.ryobi.internal.handler.RyobiAccountHandler;
import org.openhab.binding.ryobi.internal.handler.RyobiGarageDoorHandler;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.ryobi", service = ThingHandlerFactory.class)
public class RyobiBindingHandlerFactory extends BaseThingHandlerFactory {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(GARAGE_DOOR_OPENER_THING_TYPE, ACCOUNT_THING_TYPE).collect(Collectors.toSet());

    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiBindingHandlerFactory.class);

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private final RyobiAccountManager ryobiAccountManager;
    private final RyobiWebSocketClient ryobiWebSocketClient;

    @Activate
    public RyobiBindingHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        final HttpClient httpClient = httpClientFactory.getCommonHttpClient();
        ryobiAccountManager = new RyobiAccountManager(httpClient);
        ryobiWebSocketClient = new RyobiWebSocketClient();
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        LOGGER.info("Creating handler for thing with id: {}", thingTypeUID);

        if (ACCOUNT_THING_TYPE.equals(thingTypeUID)) {
            final RyobiAccountHandler ryobiAccountHandler = new RyobiAccountHandler((Bridge) thing, ryobiAccountManager,
                    ryobiWebSocketClient);
            registerDeviceDiscoveryService(ryobiAccountHandler);

            return ryobiAccountHandler;
        }

        if (GARAGE_DOOR_OPENER_THING_TYPE.equals(thingTypeUID)) {
            return new RyobiGarageDoorHandler(thing);
        }

        return null;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof RyobiAccountHandler) {
            Optional.ofNullable(discoveryServiceRegs.remove(thingHandler.getThing().getUID()))
                    .ifPresent(ServiceRegistration::unregister);
        }
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private void registerDeviceDiscoveryService(final RyobiAccountHandler ryobiAccountHandler) {
        final RyobiDeviceDiscoveryService ryobiDeviceDiscoveryService = new RyobiDeviceDiscoveryService(
                ryobiAccountHandler);
        this.discoveryServiceRegs.put(ryobiAccountHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), ryobiDeviceDiscoveryService, new Hashtable<>()));
    }
}
