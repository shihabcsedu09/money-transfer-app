package com.moneytransfer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Configuration to disable problematic metrics in containerized environments.
 * This prevents NullPointerException when system metrics are not available.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration.class
})
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
public class MetricsConfig {
    // This configuration will disable the problematic metrics auto-configuration
    // when running in production profile (containerized environment)
} 