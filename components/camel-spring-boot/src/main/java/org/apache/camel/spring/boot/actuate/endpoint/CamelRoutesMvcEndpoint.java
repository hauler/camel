/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.spring.boot.actuate.endpoint;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Adapter to expose {@link CamelRoutesEndpoint} as an {@link MvcEndpoint}.
 */
@ConfigurationProperties(prefix = "endpoints." + CamelRoutesEndpoint.ENDPOINT_ID)
public class CamelRoutesMvcEndpoint extends EndpointMvcAdapter {

    /**
     * Default path
     */
    public static final String PATH = "/camel/routes";

    private final CamelRoutesEndpoint delegate;

    public CamelRoutesMvcEndpoint(CamelRoutesEndpoint delegate) {
        super(delegate);

        this.setPath(PATH);
        this.delegate = delegate;
    }

    // ********************************************
    // Endpoints
    // ********************************************
    
    @ResponseBody
    @GetMapping(
        value = "/{id}/detail",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object detail(
            @PathVariable String id) {

        return doIfEnabled(() -> {
            Object result = delegate.getRouteDetailsInfo(id);
            if (result == null) {
                throw new NoSuchRouteException("No such route " + id);
            }

            return result;
        });
    }

    @ResponseBody
    @GetMapping(
        value = "/{id}/info",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object info(
            @PathVariable String id) {

        return doIfEnabled(() -> {
            Object result = delegate.getRouteInfo(id);
            if (result == null) {
                throw new NoSuchRouteException("No such route " + id);
            }

            return result;
        });
    }

    @ResponseBody
    @PostMapping(
        value = "/{id}/stop",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object stop(
            @PathVariable String id,
            @RequestAttribute(required = false) Long timeout,
            @RequestAttribute(required = false) Boolean abortAfterTimeout) {

        return doIfEnabled(() -> {
            try {
                delegate.stopRoute(
                    id,
                    Optional.ofNullable(timeout),
                    Optional.of(TimeUnit.SECONDS),
                    Optional.ofNullable(abortAfterTimeout)
                );
            } catch (Exception e) {
                throw new GenericException("Error stopping route " + id, e);
            }

            return ResponseEntity.ok().build();
        });
    }

    @ResponseBody
    @PostMapping(
        value = "/{id}/start",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object start(
            @PathVariable String id) {

        return doIfEnabled(() -> {
            try {
                delegate.startRoute(id);
            } catch (Exception e) {
                throw new GenericException("Error starting route " + id, e);
            }

            return ResponseEntity.ok().build();
        });
    }

    @ResponseBody
    @PostMapping(
        value = "/{id}/suspend",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object suspend(
            @PathVariable String id,
            @RequestAttribute(required = false) Long timeout) {

        return doIfEnabled(() -> {
            try {
                delegate.suspendRoute(
                    id,
                    Optional.ofNullable(timeout),
                    Optional.of(TimeUnit.SECONDS)
                );
            } catch (Exception e) {
                throw new GenericException("Error suspending route " + id, e);
            }

            return ResponseEntity.ok().build();
        });
    }

    @ResponseBody
    @PostMapping(
        value = "/{id}/resume",
        produces = {
            ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public Object resume(
            @PathVariable String id) {

        return doIfEnabled(() -> {
            try {
                delegate.resumeRoute(id);
            } catch (Exception e) {
                throw new GenericException("Error resuming route " + id, e);
            }

            return ResponseEntity.ok().build();
        });
    }

    // ********************************************
    // Helpers
    // ********************************************

    private Object doIfEnabled(Supplier<Object> supplier) {
        if (!delegate.isEnabled()) {
            return getDisabledResponse();
        }

        return supplier.get();
    }

    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public static class GenericException extends RuntimeException {
        public GenericException(String message, Throwable cause) {
            super(message, cause);

        }
    }

    @SuppressWarnings("serial")
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such route")
    public static class NoSuchRouteException extends RuntimeException {
        public NoSuchRouteException(String message) {
            super(message);
        }
    }
}
