package com.zheqiushui.httpservice.loadbalance;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerPing;
import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.HttpClient;
import com.zheqiushui.httpservice.model.Constants;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 22:36
 */
public class ServicePingCheck extends AbstractLoadBalancerPing {

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
        ContentResponse response = null; // TODO: do health check
        if(response.getStatus() == HttpStatus.OK_200) {
            return true;
        }
        return false;
    }

    private String getProtocol(Server server) {
        return server.getScheme() == null ? "http" : server.getScheme();
    }
}
