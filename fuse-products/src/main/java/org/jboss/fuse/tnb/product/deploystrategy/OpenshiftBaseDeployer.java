package org.jboss.fuse.tnb.product.deploystrategy;

import org.jboss.fuse.tnb.common.config.OpenshiftConfiguration;
import org.jboss.fuse.tnb.common.openshift.OpenshiftClient;
import org.jboss.fuse.tnb.product.endpoint.Endpoint;
import org.jboss.fuse.tnb.product.integration.builder.AbstractIntegrationBuilder;
import org.jboss.fuse.tnb.product.interfaces.OpenshiftDeployer;
import org.jboss.fuse.tnb.product.log.Log;
import org.jboss.fuse.tnb.product.log.OpenshiftLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.Pod;

public abstract class OpenshiftBaseDeployer implements OpenshiftDeployer, OpenshiftDeployStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(OpenshiftBaseDeployer.class);

    protected AbstractIntegrationBuilder<?> integrationBuilder;
    protected Path baseDirectory;

    protected String name;

    @Override
    public OpenshiftDeployer setIntegrationBuilder(AbstractIntegrationBuilder<?> integrationBuilder) {
        this.integrationBuilder = integrationBuilder;
        return this;
    }

    @Override
    public OpenshiftDeployer setBaseDirectory(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        return this;
    }

    @Override
    public OpenshiftDeployer setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Predicate<Pod> podSelector() {
        final String openshiftDeploymentLabel = OpenshiftConfiguration.openshiftDeploymentLabel();
        return p -> p.getMetadata().getLabels().containsKey(openshiftDeploymentLabel)
            && name.equals(p.getMetadata().getLabels().get(openshiftDeploymentLabel));
    }

    @Override
    public Log getLog() {
        return new OpenshiftLog(podSelector(), name);
    }

    @Override
    public Endpoint getEndpoint() {
        return new Endpoint(() -> "http://" + OpenshiftClient.get().routes()
            .withName(name).get().getSpec().getHost());
    }

    @Override
    public void undeploy() {
        LOG.info("Undeploy integration resources");
        final Map<String, String> labelMap = Map.of(OpenshiftConfiguration.openshiftDeploymentLabel(), name);
        //builds
        //delete build's pod
        OpenshiftClient.get().builds().withLabels(labelMap).list()
            .getItems().forEach(build -> OpenshiftClient.get().pods()
                .withLabels(Map.of("openshift.io/build.name", build.getMetadata().getName())).delete()
            );
        OpenshiftClient.get().builds().withLabels(labelMap).delete();
        OpenshiftClient.get().buildConfigs().withLabels(labelMap).delete();
        OpenshiftClient.get().imageStreams().withLabels(labelMap).delete();
        //app
        OpenshiftClient.get().deploymentConfigs().withLabels(labelMap).delete();
        //network
        OpenshiftClient.get().services().withLabels(labelMap).delete();
        OpenshiftClient.get().routes().withLabels(labelMap).delete();
        //remaining pods
        OpenshiftClient.get().pods().withLabels(labelMap).delete();
    }

    @Override
    public void deploy() {
        preDeploy();
        doDeploy();
        postDeploy();
    }

    public void preDeploy() {
        //do nothing
    }

    public void postDeploy() {
        //do nothing
    }

    public abstract void doDeploy();

    protected void copyResources(Path contextPath, String destinationFolder) {
        //copy resources
        Optional.ofNullable(integrationBuilder.getResources())
            .ifPresent(resources -> {
                final Path resFolder = contextPath.resolve(destinationFolder);
                try {
                    Files.createDirectories(resFolder);
                    resources.forEach(resource -> {
                        try {
                            Files.copy(Paths.get(resource.getContent()), resFolder.resolve(resource.getName()));
                        } catch (IOException e) {
                            LOG.error("unable to copy resource " + resource.getContent() + " to " + destinationFolder, e);
                        }
                    });
                } catch (IOException e) {
                    LOG.error("unable to create directory " + resFolder, e);
                }
            });
    }
}
