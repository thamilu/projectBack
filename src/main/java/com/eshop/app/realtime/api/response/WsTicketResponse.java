package com.eshop.app.realtime.api.response;

/** A short-lived, single-use ticket the client uses to authenticate a WebSocket handshake. */
public record WsTicketResponse(String ticket, long expiresInSeconds) {}
