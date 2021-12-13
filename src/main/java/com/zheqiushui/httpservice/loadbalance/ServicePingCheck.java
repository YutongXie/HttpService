package com.zheqiushui.httpservice.loadbalance;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerPing;
import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.HttpClient;
import com.zheqiushui.httpservice.model.Constants;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 22:36
 */
public class ServicePingCheck extends AbstractLoadBalancerPing {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HttpClient httpClient;

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig iClientConfig) {

    }

    @Override
    public boolean isAlive(Server server) {
        String endPoint = getProtocol(server) + "://" + server.getId() + Constants.REQUEST_PATH_HEALTH_CHECK;
        try {
            ContentResponse response = httpClient.doHealthCheck(endPoint); // TODO: do health check
            if (response.getStatus() == HttpStatus.OK_200) {
                return true;
            }
        } catch (Exception ex) {
            logger.error("failed to do health check for {}", endPoint, ex);
        }

        return false;
    }

    private String getProtocol(Server server) {
        return server.getScheme() == null ? "http" : server.getScheme();
    }
}
