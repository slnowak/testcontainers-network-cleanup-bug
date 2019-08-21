package com.example.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.File;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkBugTest {

    @Test
    void this_network_is_cleaned_up_properly() {
        var networkName = "cleaned-up-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        assertTrue(true);
    }

    @Test
    void network_mentioned_in_docker_compose_file_is_not_cleaned_afterwards() throws Exception {
        var networkName = "not-cleaned-up-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        var compose = new DockerComposeContainer<>(file("docker-compose.yml"), file("docker-compose.override.yml"))
                .withLocalCompose(true)
                .withEnv("TEST_NETWORK_NAME", networkName);
        compose.start();

        assertTrue(true);
    }

    @Test
    void cleaned_up_properly_if_containers_disconnected_from_network() throws Exception {
        var networkName = "with-disconnect-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        var compose = new DockerComposeContainer<>(file("docker-compose.yml"), file("docker-compose.override.yml"))
                .withLocalCompose(true)
                .withEnv("TEST_NETWORK_NAME", networkName);
        compose.start();

        // disconnect all containers from the network
        DockerClientFactory
                .instance()
                .client()
                .inspectNetworkCmd()
                .withNetworkId(network.getId())
                .exec()
                .getContainers()
                .forEach((containerId, __) ->
                        DockerClientFactory
                                .instance()
                                .client()
                                .disconnectFromNetworkCmd()
                                .withContainerId(containerId)
                                .withNetworkId(network.getId())
                                .exec()
                );

        assertTrue(true);
    }

    private File file(String s) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(s).toURI());
    }
}
