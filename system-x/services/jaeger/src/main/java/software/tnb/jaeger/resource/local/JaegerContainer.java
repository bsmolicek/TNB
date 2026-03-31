package software.tnb.jaeger.resource.local;

import software.tnb.jaeger.service.configuration.JaegerConfiguration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

public class JaegerContainer extends GenericContainer<JaegerContainer> {

    public JaegerContainer(String image, Map<String, String> env) {
        super(image);
        withEnv(env);
        // Use explicit port mappings instead of host networking to work on macOS and Windows
        Stream.of(JaegerConfiguration.CollectorPort.values(), JaegerConfiguration.QueryPort.values())
            .flatMap(Stream::of)
            .forEach(port -> addFixedExposedPort(port.portNumber(), port.portNumber()));
        waitingFor(Wait.forLogMessage(".*ListenSocket created.*", 1).withStartupTimeout(Duration.ofMinutes(2)));
    }
}
