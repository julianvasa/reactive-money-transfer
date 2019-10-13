package com.moneytransfer.reactive.exception;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * In case of exception send a nice json as a response to the client with lots of information about the exception cause, path, status code
 * @author Julian Vasa
 */
public class Exception {
    public static void error(RoutingContext routingContext, int status, String cause) {
        JsonObject error = new JsonObject()
            .put("error", cause)
            .put("code", status)
            .put("path", routingContext.request().path());
        routingContext.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(status)
            .end(error.encodePrettily());
    }
}
