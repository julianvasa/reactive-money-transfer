package com.moneytransfer.reactive;

import io.vertx.core.Vertx;

/**
 * Main class which deploys MainVerticle
 */
public class Application {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName());
    }
}
