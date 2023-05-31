package software.tnb.product.application;

import software.tnb.common.config.TestConfiguration;
import software.tnb.common.utils.WaitUtils;
import software.tnb.product.endpoint.Endpoint;
import software.tnb.product.log.Log;
import software.tnb.product.log.stream.LogStream;
import software.tnb.product.mapstruct.MapstructConfiguration;
import software.tnb.product.util.maven.Maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public abstract class App {
    private static final Pattern LOG_STARTED_REGEX = Pattern.compile("(?m)^.*Apache Camel.*started in.*$");
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    protected final String name;
    protected final String logFilePrefix;
    protected Log log;
    protected LogStream logStream;
    protected Endpoint endpoint;
    protected boolean started = false;

    public App(String name) {
        this.name = name;
        ensureDirNotPresent();
        logFilePrefix = name + "-" + new Date().getTime() + "-";
    }

    private void ensureDirNotPresent() {
        final File target = TestConfiguration.appLocation().resolve(name).toFile();
        if (target.exists()) {
            try {
                LOG.debug("Deleting directory with existing application sources {}", target);
                FileUtils.deleteDirectory(target);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't delete existing application sources directory", e);
            }
        }
    }

    public abstract void start();

    public abstract void stop();

    public abstract boolean isReady();

    public abstract boolean isFailed();

    public Log getLog() {
        return log;
    }

    public String getEndpoint() {
        return endpoint.getAddress();
    }

    public String getName() {
        return name;
    }

    public Path getLogPath(Phase phase) {
        return TestConfiguration.appLocation().resolve(logFilePrefix + phase.name().toLowerCase() + ".log");
    }

    public Path getLogPath() {
        return getLogPath(Phase.RUN);
    }

    public void waitUntilReady() {
        WaitUtils.waitFor(() -> isReady() && isCamelStarted(), this::isFailed, 1000L, "Waiting until the integration " + name + " is running");
        started = true;
    }

    private boolean isCamelStarted() {
        return getLog().containsRegex(LOG_STARTED_REGEX);
    }

    public void updatePlugin(String pluginName, Plugin plugin) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom annotationProcessorPaths = new Xpp3Dom("annotationProcessorPaths");

        Xpp3Dom path = new Xpp3Dom("path");

        Xpp3Dom groupId = new Xpp3Dom("groupId");
        groupId.setValue("org.mapstruct");

        Xpp3Dom artifactId = new Xpp3Dom("artifactId");
        artifactId.setValue("mapstruct-processor");

        Xpp3Dom version = new Xpp3Dom("version");
        version.setValue(MapstructConfiguration.mapstructMapperVersion());

        path.addChild(groupId);
        path.addChild(artifactId);
        path.addChild(version);
        annotationProcessorPaths.addChild(path);
        configuration.addChild(annotationProcessorPaths);

        File pom = TestConfiguration.appLocation().resolve(name).resolve("pom.xml").toFile();
        Model model = Maven.loadPom(pom);

        for (Plugin p : model.getBuild().getPlugins()) {
            if ("maven-compiler-plugin".equals(p.getArtifactId())) {
                p.setConfiguration(configuration);
                break;
            }
        }
        Maven.writePom(pom, model);
    }

    protected void customizePlugins(List<Plugin> mavenPlugins) {
        File pom = TestConfiguration.appLocation().resolve(name).resolve("pom.xml").toFile();
        Model model = Maven.loadPom(pom);

        if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
            mavenPlugins.forEach(model.getBuild().getPlugins()::add);
        }

        Maven.writePom(pom, model);
    }
}
