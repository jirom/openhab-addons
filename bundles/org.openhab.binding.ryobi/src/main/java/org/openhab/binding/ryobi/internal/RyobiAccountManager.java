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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.ryobi.internal.models.BasicDevice;
import org.openhab.binding.ryobi.internal.models.DetailedDevice;
import org.openhab.binding.ryobi.internal.models.DetailedDeviceResponse;
import org.openhab.binding.ryobi.internal.models.DevicesResponse;
import org.openhab.binding.ryobi.internal.models.LoginRequest;
import org.openhab.binding.ryobi.internal.models.LoginResponse;
import org.openhab.core.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * @author Manuel Gerome Navarro - Initial contribution
 */
@NonNullByDefault
public class RyobiAccountManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RyobiAccountManager.class);
    private static final URI BASE_URI = URI.create("https://tti.tiwiconnect.com");

    private final Gson gson;
    private final HttpClient httpClient;
    private String apiKey = "";

    public RyobiAccountManager(HttpClient httpClient) {
        this.gson = new Gson();
        this.httpClient = httpClient;
    }

    public synchronized String getApiKey(final String username, final String password) throws IOException {
        if (apiKey.isEmpty()) {
            this.apiKey = generateApiKey(username, password);
        }

        return apiKey;
    }

    public synchronized List<BasicDevice> getDevices() throws IOException {
        final URI devicesUri = UriBuilder.fromUri(BASE_URI).path("/api/devices").build();

        final String serializedResponse = HttpUtil.executeUrl("GET", devicesUri.toString(), getHeaders(), null, null,
                10_000);
        final DevicesResponse response = gson.fromJson(serializedResponse, DevicesResponse.class);
        return response.devices;
    }

    public synchronized DetailedDevice getDetailedDevice(final String deviceId) throws IOException {
        final URI devicesUri = UriBuilder.fromUri(BASE_URI).path("/api/devices").path(deviceId).build();

        final String serializedResponse = HttpUtil.executeUrl("GET", devicesUri.toString(), getHeaders(), null, null,
                10_000);
        final DetailedDeviceResponse response = gson.fromJson(serializedResponse, DetailedDeviceResponse.class);
        return response.getDevice();
    }

    private String generateApiKey(final String username, final String password) throws IOException {
        final URI loginUri = UriBuilder.fromUri(BASE_URI).path("/api/login").build();

        final LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;
        final String serializedRequest = gson.toJson(request);

        final ContentResponse contentResponse;
        try {
            contentResponse = httpClient.POST(loginUri).header("x-tc-transform", "tti-app")
                    .timeout(10, TimeUnit.SECONDS)
                    .content(new StringContentProvider(serializedRequest, StandardCharsets.UTF_8),
                            "application/json; charset=utf-8")
                    .send();
        } catch (InterruptedException | TimeoutException e) {
            throw new IOException("Could not get the api key", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpResponseException) {
                final Response response = ((HttpResponseException) e.getCause()).getResponse();
                LOGGER.error("Got error with status={}, reason={}, headers={}", response.getStatus(),
                        response.getReason(), response.getHeaders());
            }
            throw new IOException("Could not get the api key", e);
        }

        final LoginResponse loginResponse = gson.fromJson(contentResponse.getContentAsString(), LoginResponse.class);
        return loginResponse.result.auth.apiKey;
    }

    private Properties getHeaders() {
        final Properties properties = new Properties();
        properties.setProperty("x-tc-transform", "tti-app");
        properties.setProperty("x-tc-transformversion", "0.2"); // Only needed for /api/devices
        return properties;
    }
}
