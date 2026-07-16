package com.iams.notification.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default binding when no real SMS gateway is on the classpath/configured (US-NTF-02's degrade-gracefully case). */
@Configuration
public class NoSmsGateway {

    @Bean
    @ConditionalOnMissingBean(SmsGateway.class)
    public SmsGateway noopSmsGateway() {
        return new SmsGateway() {
            @Override
            public boolean configured() {
                return false;
            }

            @Override
            public void send(String phoneNumber, String text) {
                throw new IllegalStateException("No SMS gateway configured");
            }
        };
    }
}
