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
    void network_not_referenced_in_docker_compose_is_cleaned_up_properly() {
        var networkName = "cleaned-up-not-referenced-in-docker-compose-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        assertTrue(true);
    }

    @Test
    void network_mentioned_in_docker_compose_file_with_zookeeper_and_external_container_is_not_cleaned_up_afterwards() throws Exception {
        var networkName = "not-cleaned-up-with-zookeeper-and-external-container-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        var compose = new DockerComposeContainer<>(file("docker-compose.zookeeper.yml"))
                .withLocalCompose(true)
                .withEnv("TEST_NETWORK_NAME", networkName);
        compose.start();

        assertTrue(true);
    }

    @Test
    void network_mentioned_in_docker_compose_file_with_zookeeper_and_without_external_container_is_not_cleaned_up_afterwards() throws Exception {
        var networkName = "not-cleaned-up-with-zookeper-and-without-external-container-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        // make sure we initialize network
        network.getId();

        var compose = new DockerComposeContainer<>(file("docker-compose.zookeeper.yml"))
                .withLocalCompose(true)
                .withEnv("TEST_NETWORK_NAME", networkName);
        compose.start();

        assertTrue(true);
    }

    @Test
    void network_mentioned_in_docker_compose_file_with_busybox_and_external_container_is_cleaned_up_properly() throws Exception {
        var networkName = "cleaned-up-with-busybox-and-external-container-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        var compose = new DockerComposeContainer<>(file("docker-compose.busybox.yml"))
                .withLocalCompose(true)
                .withEnv("TEST_NETWORK_NAME", networkName);
        compose.start();

        assertTrue(true);
    }

    @Test
    void network_is_cleaned_up_properly_if_you_disconnect_containers_from_it() throws Exception {
        var networkName = "cleaned-up-when-containers-disconnected-" + UUID.randomUUID().toString();
        var network = Network
                .builder()
                .createNetworkCmdModifier(cmd -> cmd.withName(networkName))
                .build();

        var container = new GenericContainer<>("busybox").withNetwork(network);
        container.start();

        var compose = new DockerComposeContainer<>(file("docker-compose.zookeeper.yml"))
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
