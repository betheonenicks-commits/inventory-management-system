package com.iams.notification.infrastructure;

/**
 * US-NTF-02: the gateway abstraction. No real gateway exists in this
 * deployment, so the default binding is {@link NoSmsGateway} and the AC's
 * graceful-degradation rule ("nothing errors, email/in-app still deliver")
 * is the live behavior. A real provider is one @Component implementing this.
 */
public interface SmsGateway {

    /** True when a gateway is actually configured and able to send. */
    boolean configured();

    /** Sends one SMS; only called when {@link #configured()} is true. Failures throw. */
    void send(String phoneNumber, String text);
}
