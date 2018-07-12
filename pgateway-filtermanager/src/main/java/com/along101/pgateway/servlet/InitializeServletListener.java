package com.along101.pgateway.servlet;

import com.along101.pgateway.common.Constants;
import com.along101.pgateway.core.LogConfigurator;
import com.along101.pgateway.monitor.*;
import com.along101.pgateway.util.IPUtil;
import com.netflix.appinfo.*;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.servo.util.ThreadCpuStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InitializeServletListener implements ServletContextListener {

    private Logger LOGGER = LoggerFactory.getLogger(InitializeServletListener.class);
    private String appName = null;

    private LogConfigurator logConfigurator;


    public InitializeServletListener() {
        String applicationID = ConfigurationManager.getConfigInstance().getString(Constants.DeploymentApplicationID);
        if (applicationID == null || applicationID.isEmpty()) {
            System.setProperty(Constants.DeployConfigUrl, "http://localhost:8080/configs/apollo/10010002");
            ConfigurationManager.getConfigInstance().setProperty(Constants.DeploymentApplicationID, "wirelessgate");
        }
        ConfigurationManager.getConfigInstance().setProperty(Constants.DeploymentApplicationID, "internalgate");


        loadConfiguration();
        configLog();
        registerEureka();
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            initMonitor();
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        } catch (Exception e) {
            LOGGER.error("Error while initializing pgateway.", e);
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        MetricReporter.getInstance().shutdown();
    }

    private void initMonitor() {
        MetricReporter.getInstance().start();

        LOGGER.info("Registering Servo Monitor");
        MonitorRegistry.getInstance().setPublisher(new ServoMonitor());

        LOGGER.info("Starting Poller");
        MetricPoller.startPoller();

        LOGGER.info("Registering Servo Tracer");
        TracerFactory.initialize(new Tracer());

        LOGGER.info("Registering Servo Counter");
        CounterFactory.initialize(new Counter());

        LOGGER.info("Starting CPU stats");
        final ThreadCpuStats stats = ThreadCpuStats.getInstance();
        stats.start();

    }


    private void loadConfiguration() {
        System.setProperty(DynamicPropertyFactory.ENABLE_JMX, "true");

        appName = ConfigurationManager.getDeploymentContext().getApplicationId();

        // Loading properties via archaius.
        if (null != appName) {
            try {
                LOGGER.info(String.format("Loading application properties with app id: %s and environment: %s", appName,
                        ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()));
                ConfigurationManager.loadCascadedPropertiesFromResources(appName);
            } catch (IOException e) {
                LOGGER.error(String.format(
                        "Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
                        appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
            }
        } else {
            LOGGER.warn(
                    "Application identifier not defined, skipping application level properties loading. You must set a property 'archaius.deployment.applicationId' to be able to load application level properties.");
        }

    }

    private void configLog() {
        logConfigurator = new LogConfigurator(appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
        logConfigurator.config();
    }

    private void registerEureka() {
        DynamicBooleanProperty eurekaEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty("eureka.enabled",
                true);
        if (!eurekaEnabled.get())
            return;

        EurekaInstanceConfig eurekaInstanceConfig = new PropertiesInstanceConfig() {
        };
        ConfigurationManager.getConfigInstance().setProperty("eureka.statusPageUrl", "http://" + getTurbineInstance());

        DiscoveryManager.getInstance().initComponent(eurekaInstanceConfig, new DefaultEurekaClientConfig());

        final DynamicStringProperty serverStatus = DynamicPropertyFactory.getInstance()
                .getStringProperty("server." + IPUtil.getLocalIP() + ".status", "up");
        DiscoveryManager.getInstance().getDiscoveryClient().registerHealthCheckCallback(new HealthCheckCallback() {
            @Override
            public boolean isHealthy() {
                return serverStatus.get().toLowerCase().equals("up");
            }
        });

        String version = String.valueOf(System.currentTimeMillis());
        String group = ConfigurationManager.getConfigInstance().getString("server.group", "default");
        String dataCenter = ConfigurationManager.getConfigInstance().getString("server.data-center", "default");

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("version", version);
        metadata.put("group", group);
        metadata.put("dataCenter", dataCenter);

        String turbineInstance = getTurbineInstance();
        if (turbineInstance != null) {
            metadata.put("turbine.instance", turbineInstance);
        }

        ApplicationInfoManager.getInstance().registerAppMetadata(metadata);
    }

    public String getTurbineInstance() {
        String instance = null;
        String ip = IPUtil.getLocalIP();
        if (ip != null) {
            instance = ip + ":" + ConfigurationManager.getConfigInstance().getString("server.internals.port", "8077");
        } else {
            LOGGER.warn("Can't build turbine instance as can't fetch the ip.");
        }
        return instance;
    }
}
