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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class BasicDevice {
    @SerializedName("varName")
    public String id = "";
    public List<String> deviceTypeIds = List.of();
    public MetaData metaData = new MetaData();

    public static class MetaData {
        public String name = "";
        public String socketId = "";
        public int version = 0;
    }
}
