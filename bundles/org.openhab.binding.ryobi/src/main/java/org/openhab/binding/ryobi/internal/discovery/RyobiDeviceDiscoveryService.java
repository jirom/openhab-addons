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
package org.openhab.binding.ryobi.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.ryobi.internal.RyobiBindingConstants;
import org.openhab.binding.ryobi.internal.factory.RyobiBindingHandlerFactory;
import org.openhab.binding.ryobi.internal.handler.RyobiAccountHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiDeviceDiscoveryService extends AbstractDiscoveryService {

    private final RyobiAccountHandler ryobiAccountHandler;

    public RyobiDeviceDiscoveryService(RyobiAccountHandler ryobiAccountHandler) throws IllegalArgumentException {
        super(RyobiBindingHandlerFactory.SUPPORTED_THING_TYPES_UIDS, 10);
        this.ryobiAccountHandler = ryobiAccountHandler;
    }

    @Override
    protected void startScan() {
        ryobiAccountHandler.getDevices().stream().filter(device -> device.deviceTypeIds.contains("gdoMasterUnit"))
                .map(device -> {
                    final ThingUID thingUID = new ThingUID(RyobiBindingConstants.GARAGE_DOOR_OPENER_THING_TYPE,
                            device.id);
                    return DiscoveryResultBuilder.create(thingUID).withBridge(ryobiAccountHandler.getThing().getUID())
                            .withLabel(device.metaData.name)
                            .withProperty(RyobiBindingConstants.GARAGE_DOOR_OPENER_ID, device.id)
                            .withProperty("version", device.metaData.version).build();
                }).forEach(this::thingDiscovered);
    }
}
