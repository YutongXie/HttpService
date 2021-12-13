package com.zheqiushui.httpservice;

import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.loadbalance.ClientLoadBalanceService;
import com.zheqiushui.httpservice.loadbalance.ClientLoadBalancerFactoryBean;
import com.zheqiushui.httpservice.loadbalance.ServicePingCheck;
import com.zheqiushui.httpservice.model.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 22:48
 */
public class HttpClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String serverAddress;
    private boolean regionBasedEnabled = false;
    private String authRealName;
    private String authUserName;
    private String authPassword;
    private String authMethod = "Basic";
    private String protocol = "http";
    private String region;

    private ClientLoadBalanceService loadBalanceService;
    private org.eclipse.jetty.client.HttpClient httpClient;

    public void setArgs(Properties properties) {
        if (properties != null) {
            properties.stringPropertyNames().forEach(propName -> {
                System.setProperty(propName, properties.getProperty(propName));
            });
        }
    }

    public void init() {
        initHttpClient();
        initLoadBalancer();

    }

    private void initHttpClient() {
        SslContextFactory sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
        sslContextFactory.setTrustAll(true);
        String trustStore = getTrustStore();
        String trustStorePass = getTrustStorePassword();

        if (StringUtils.isNotBlank(trustStore)) {
            Resource resource = JarResource.newClassPathResource(trustStore);
            sslContextFactory.setTrustStoreResource(resource);
        }

        if (StringUtils.isNotBlank(trustStorePass)) {
            Resource resource = JarResource.newClassPathResource(trustStorePass);
            sslContextFactory.setTrustStoreResource(resource);
        }

        httpClient = new org.eclipse.jetty.client.HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(true);
        registerStopHook();
        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("failed to start up http client.", e);
        }
    }

    private void registerStopHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                httpClient.stop();
            } catch (Exception e) {
                logger.error("failed to stop http client.", e);
            }
        }));
    }

    private void initLoadBalancer() {
        loadBalanceService = new ClientLoadBalanceService();
        ClientLoadBalancerFactoryBean factoryBean = new ClientLoadBalancerFactoryBean();
        factoryBean.setServerAddress(getServerAddress());
        factoryBean.setRegionBasedEnabled(isRegionBasedEnabled());
        factoryBean.setClientLocationRegion(getRegion());

        ServicePingCheck servicePingCheck = new ServicePingCheck();
        servicePingCheck.setHttpClient(this);
        factoryBean.setServicePingCheck(servicePingCheck);

        try {
            loadBalanceService.setLoadBalancer(factoryBean.createInstance());
        } catch (Exception e) {
            logger.error("failed to init client side load balancer", e);
        }

    }

    public Object doRequest(byte[] bytes) throws Exception {

        ContentResponse response = sendHttpRequest(bytes);
        if (response.getStatus() != HttpStatus.OK_200) {
            return null;
        } else {
            return response.getContent();
        }
    }

    private ContentResponse sendHttpRequest(byte[] bytes) throws Exception {
        return httpClient.POST(getEndPoint())
                .header("Authorization", authMethod + " " + getAccessToken())
                .content(new BytesContentProvider(bytes))
                .send();
    }


    private byte[] sendHttpRequestByAsync(byte[] bytes) throws Exception {
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.POST(getEndPoint())
                .header("Authorization", authMethod + " " + getAccessToken())
                .content(new BytesContentProvider(bytes))
                .send(listener);
        Response response = listener.get(httpClient.getConnectTimeout(), TimeUnit.SECONDS);
        if (response.getStatus() == HttpStatus.OK_200) {
            try (InputStream inputStream = listener.getInputStream()) {
                return IOUtils.toByteArray(inputStream);
            }
        } else {
            return null;
        }

    }


    private URI getEndPoint() throws Exception {
        Server server = loadBalanceService.chooseServer();
        String endPoint = server.getScheme() + "://" + server.getId() + Constants.REQUEST_PATH_BUSINESS;
        return new URI(endPoint);
    }

    private String getAccessToken() {
        return Base64.getEncoder().encodeToString("test token".getBytes(StandardCharsets.UTF_8));
    }

    private String getTrustStore() {
        return System.getProperty("http.trust.store");
    }

    private String getTrustStorePassword() {
        return System.getProperty("http.trust.store.password");
    }

    public ContentResponse doHealthCheck(String url) throws ExecutionException, InterruptedException, TimeoutException {
        return httpClient.newRequest(url)
                .header("Authorization", authMethod + " " + getAccessToken())
                .timeout(10, TimeUnit.SECONDS)
                .send();
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public boolean isRegionBasedEnabled() {
        return regionBasedEnabled;
    }

    public void setRegionBasedEnabled(boolean regionBasedEnabled) {
        this.regionBasedEnabled = regionBasedEnabled;
    }

    public String getAuthRealName() {
        return authRealName;
    }

    public void setAuthRealName(String authRealName) {
        this.authRealName = authRealName;
    }

    public String getAuthUserName() {
        return authUserName;
    }

    public void setAuthUserName(String authUserName) {
        this.authUserName = authUserName;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
