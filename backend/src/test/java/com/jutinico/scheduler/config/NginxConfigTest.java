package com.jutinico.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * El bloqueo de login usa la IP que nginx pone en X-Forwarded-For (SEC-001). Si nginx la añadiera a la que manda el
 * cliente ($proxy_add_x_forwarded_for), bastaría con cambiar ese header para saltarse el bloqueo.
 */
class NginxConfigTest {

    private static final Path NGINX_CONF = Path.of("..", "frontend", "nginx.conf");

    @Test
    void nginxOverwritesXForwardedForWithTheRealClientIp() throws Exception {
        String conf = Files.readString(NGINX_CONF);

        assertThat(conf).contains("proxy_set_header X-Forwarded-For $remote_addr;");
        assertThat(conf).doesNotContain("$proxy_add_x_forwarded_for");
    }
}
