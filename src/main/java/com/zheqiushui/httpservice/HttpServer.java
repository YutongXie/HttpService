package com.zheqiushui.httpservice;

import com.zheqiushui.httpservice.model.Constants;
import com.zheqiushui.httpservice.servlet.BusinessServlet;
import com.zheqiushui.httpservice.servlet.GuardServlet;
import com.zheqiushui.httpservice.servlet.HealthCheckServlet;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.Properties;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 20:40
 */
public class HttpServer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Server server;
    private Integer port;
    private Integer tlsPort;

    public void setArgs(Properties properties) {
        if (properties != null) {
            properties.stringPropertyNames().forEach(propName -> {
                System.setProperty(propName, properties.getProperty(propName));
            });
        }
    }

    public void init() {
        try {
            if (getPort() == null && getTlsPort() == null) {
                throw new IllegalArgumentException("Invalid port");
            }

            if (getTlsPort() == null) {
                createServer(getPort());
            } else {
                createServer();
                setupConnector();
            }

            setupHandler();
            setupShutdown();

            server.start();
            server.join();
        } catch (Exception ex) {
            logger.error("failed to startup server.", ex);
        }
    }

    public void setupConnector() {
        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setSecureScheme("https");
        configuration.setSecurePort(getTlsPort());
        configuration.setSendServerVersion(false);
        configuration.setSendDateHeader(false);
        configuration.setOutputBufferSize(32768);
        configuration.setRequestHeaderSize(Constants.REQUEST_HEADER_SIZE_LIMIT);

        ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(configuration));
        httpConnector.setPort(getPort());
        httpConnector.setIdleTimeout(30000);

        SslContextFactory factory = new SslContextFactory.Server();
        setupKeyStore(factory);
        setupAdditionalSSLConfig(factory);

        HttpConfiguration httpConfiguration = new HttpConfiguration(configuration);
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setStsMaxAge(2000);
        secureRequestCustomizer.setStsIncludeSubDomains(true);
        httpConfiguration.addCustomizer(secureRequestCustomizer);

        ServerConnector httpsConnecter = new ServerConnector(server,
                new SslConnectionFactory(factory, Constants.HTTP_PROTOCOL_1_1),
                new HttpConnectionFactory(configuration));

        httpsConnecter.setPort(getTlsPort());
        httpsConnecter.setIdleTimeout(50000);
        server.setConnectors(new Connector[]{httpsConnecter, httpConnector});
    }

    private void setupKeyStore(SslContextFactory sslContextFactory) {
        String keyStore = getKeyStore();
        String keyStorePass = getKeyStorePassword();

        if (StringUtils.isNotBlank(keyStore)) {
            Resource resource = JarResource.newClassPathResource(keyStore);
            sslContextFactory.setKeyStoreResource(resource);
            sslContextFactory.setKeyStorePassword(keyStorePass);
        }
    }

    private void setupAdditionalSSLConfig(SslContextFactory sslContextFactory) {
        sslContextFactory.addExcludeProtocols(Constants.SSL_EXCLUDE_PROTOCOLS);
        sslContextFactory.addExcludeCipherSuites(Constants.SSL_EXCLUDE_CIPHERSUITES);

    }

    private void createServer(int port) {
        server = new Server(port);
    }

    private void createServer() {
        int maxPoolSize = getPoolSize();
        if (maxPoolSize > 0) {
            QueuedThreadPool pool = new QueuedThreadPool();
            pool.setMaxThreads(maxPoolSize);
            server = new Server(pool);
        } else {
            server = new Server();
        }
    }

    private void setupHandler() {
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/*");
        servletContextHandler.setMaxFormContentSize(Constants.REQUEST_BODY_SIZE_LIMIT_BUSINESS);
        servletContextHandler.setAllowNullPathInfo(false);

        ServletHolder guardHolder = new ServletHolder(new GuardServlet());
        servletContextHandler.addServlet(guardHolder, Constants.REQUEST_PATH_PATTERN_ROOT);

        ServletHolder healthCheckHolder = new ServletHolder(new HealthCheckServlet());
        servletContextHandler.addServlet(healthCheckHolder, Constants.REQUEST_PATH_PATTERN_HEALTH_CHECK);

        ServletHolder businessHolder = new ServletHolder(new BusinessServlet());
        servletContextHandler.addServlet(businessHolder, Constants.REQUEST_PATH_PATTERN_BUSINESS);

        setupFilters(servletContextHandler);

        Handler handler;
        if (isServerAuthEnabled()) {
            handler = getSecurityHandler(servletContextHandler);
        } else {
            handler = servletContextHandler;
        }
        server.setHandler(buildCompressionHandler(handler));
    }

    private Handler buildCompressionHandler(Handler handler) {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.addIncludedMethods(Constants.COMPRESSION_GZIP_INCLUDED_METHODS);
        gzipHandler.addIncludedMimeTypes(Constants.COMPRESSION_GZIP_INCLUDED_MIME_TYPES);
        gzipHandler.addIncludedPaths(Constants.COMPRESSION_GZIP_INCLUDED_PATH);
        // support 1-9,
        // 1: fastest speed, but lower compression ratio
        // 9: lowest compression speed, but highest compression ratio
        // default is 6
        gzipHandler.setCompressionLevel(4);
        gzipHandler.setHandler(handler);
        return gzipHandler;
    }

    private void setupDosFilter(ServletContextHandler servletContextHandler) {
        EnumSet<DispatcherType> scope = EnumSet.of(DispatcherType.REQUEST);

        FilterHolder holder = new FilterHolder(DoSFilter.class);
        String maxReqPerSec = System.getProperty("http.Dos.MaxRequestPerSec");
        String delayMs = System.getProperty("http.Dos.delayMs");

        if (StringUtils.isNotBlank(maxReqPerSec)) {
            holder.setInitParameter("maRequestsPerSec", maxReqPerSec);

        } else {
            holder.setInitParameter("maRequestsPerSec", Constants.DOS_FILTER_DEFAULT_MAX_REQUET_PERSEC);
        }

        if (StringUtils.isNotBlank(delayMs)) {
            holder.setInitParameter("delayMs", delayMs);

        } else {
            holder.setInitParameter("delayMs", Constants.DOS_FILTER_DEFAULT_DELAY_MS);
        }

        holder.setInitParameter("remotePort", "true");
        holder.setInitParameter("enabled", "true");
        holder.setInitParameter("trackSessions", "true");
        servletContextHandler.addFilter(holder, "/*", scope);
    }

    private void setupFilters(ServletContextHandler servletContextHandler) {
        setupDosFilter(servletContextHandler);
    }

    private Handler getSecurityHandler(Handler handler) {
        HashLoginService hashLoginService = new HashLoginService("Qiushui.Zhe");
        hashLoginService.setUserStore(buildUserStore());
        server.addBean(hashLoginService);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setAuthenticate(true);
        // Only allow users with that roles can access
        constraint.setRoles(new String[]{"user", "admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();

        constraintSecurityHandler.setConstraintMappings(new ConstraintMapping[]{mapping});
        constraintSecurityHandler.setAuthenticator(new BasicAuthenticator());
        constraintSecurityHandler.setLoginService(hashLoginService);
        constraintSecurityHandler.setHandler(handler);

        return constraintSecurityHandler;

    }

    private boolean isServerAuthEnabled() {
        String authEnabled = System.getProperty("http.server.AuthEnabled");
        if (StringUtils.isNotBlank(authEnabled)) {
            return Boolean.parseBoolean(authEnabled);
        } else {
            return false;
        }
    }

    private UserStore buildUserStore() {
        UserStore userStore = new UserStore();
        userStore.addUser("qiushui_1", new Password("1001"), new String[]{"admin"});
        return userStore;
    }

    private int getPoolSize() {
        String poolSize = System.getProperty("http.server.maxPoolSize");
        if (StringUtils.isNotBlank(poolSize)) {
            return Integer.parseInt(poolSize);
        } else {
            return -1;
        }
    }

    private void setupShutdown() {
        server.setStopAtShutdown(true);
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(Integer tlsPort) {
        this.tlsPort = tlsPort;
    }

    private String getKeyStore() {
        return System.getProperty("http.server.keystore");
    }

    private String getKeyStorePassword() {
        return System.getProperty("http.server.keystore.password");
    }
}
