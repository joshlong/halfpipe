package halfpipe.properties;

import com.netflix.config.*;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.netflix.config.ConfigurationManager.*;
import static com.netflix.config.ConfigurationBasedDeploymentContext.*;

/**
 * User: spencergibb
 * Date: 4/9/14
 * Time: 1:30 PM
 */
public class ArchaiusInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ArchaiusInitializer.class);

    private AtomicBoolean initialized = new AtomicBoolean(false);

    @Inject
    ConfigurableApplicationContext context;

    @Autowired(required = false)
    List<PropertiesSourceFactory> factories;

    private FixedDelayPollingScheduler pollingScheduler;

    @SuppressWarnings("deprecated")
    public synchronized void initializeArchaius() {
        if (initialized.compareAndSet(false, true)) {
            ConfigurableEnvironment env = context.getEnvironment();

            String urls = env.getProperty("archaius.urls");
            if (urls != null)
                System.setProperty("archaius.configurationSource.additionalUrls", urls);

            String defaultFileName = env.getProperty("archauis.file.name", "application.yml");
            System.setProperty("archaius.configurationSource.defaultFileName", defaultFileName);

            String id = context.getEnvironment().getProperty("application.id");
            if (id == null) {
                throw new IllegalStateException("application.id may not be null");
            }
            //this is deprecated, but currently it seams the only way to set it initially
            System.setProperty(DEPLOYMENT_APPLICATION_ID_PROPERTY, id);

            //TODO: support for other DeploymentContexts

            ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();

            //support to add other Configurations (Jdbc, DynamoDb, Zookeeper, jclouds, etc...)
            if (factories != null && !factories.isEmpty()) {
                for (PropertiesSourceFactory factory: factories) {
                    config.addConfiguration(factory.getConfiguration(), factory.getName());
                }
            }

            //TODO: move all of theses to PropertiesSourceFactory impls with @Ordered
            try {
                pollingScheduler = new FixedDelayPollingScheduler();
                UrlPropertiesSource propertiesSource = new UrlPropertiesSource();
                DynamicConfiguration dynamicConfiguration = new DynamicConfiguration(propertiesSource, pollingScheduler);
                config.addConfiguration(dynamicConfiguration, URL_CONFIG_NAME);
            } catch (Throwable e) {
                logger.warn("Failed to create default dynamic configuration", e);
            }

            //TODO: sys/env above urls?
            if (!Boolean.getBoolean(DISABLE_DEFAULT_SYS_CONFIG)) {
                SystemConfiguration sysConfig = new SystemConfiguration();
                config.addConfiguration(sysConfig, SYS_CONFIG_NAME);
            }
            if (!Boolean.getBoolean(DISABLE_DEFAULT_ENV_CONFIG)) {
                EnvironmentConfiguration envConfig = new EnvironmentConfiguration();
                config.addConfiguration(envConfig, ENV_CONFIG_NAME);
            }

            ConcurrentCompositeConfiguration appOverrideConfig = new ConcurrentCompositeConfiguration();
            config.addConfiguration(appOverrideConfig, APPLICATION_PROPERTIES);
            config.setContainerConfigurationIndex(config.getIndexOfConfiguration(appOverrideConfig));

            ConfigurationManager.install(config);
        }
    }

    @PreDestroy
    public void destroy() {
        pollingScheduler.stop();
    }
}
