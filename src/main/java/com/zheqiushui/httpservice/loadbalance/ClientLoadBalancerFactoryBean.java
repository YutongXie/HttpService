package com.zheqiushui.httpservice.loadbalance;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.HttpServer;
import com.zheqiushui.httpservice.model.Constants;
import com.zheqiushui.httpservice.model.ServiceServer;
import com.zheqiushui.httpservice.util.RegionUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.print.event.PrintJobAttributeEvent;
import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 22:30
 */
public class ClientLoadBalancerFactoryBean extends AbstractFactoryBean<BaseLoadBalancer> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ServicePingCheck servicePingCheck;
    private String serverAddress;
    private String namespace = "qiushui.zhe";
    private boolean regionBasedEnabled = false;
    private String clientLocationRegion;

    @Override
    public Class<?> getObjectType() {
        return BaseLoadBalancer.class;
    }

    @Override
    public BaseLoadBalancer createInstance() throws Exception {
        DefaultClientConfigImpl clientConfig = DefaultClientConfigImpl.getClientConfigWithDefaultValues();

        setupSystemConfig(clientConfig);
        setupCustomizeConfig(clientConfig);

        BaseLoadBalancer baseLoadBalancer = LoadBalancerBuilder
                .newBuilder()
                .withPing(servicePingCheck)
                .buildFixedServerListLoadBalancer(this.getLoadBalanceServers(serverAddress));
        if(isRegionBasedEnabled()) {

        } else {
            CommonRoundRobinRule rule = new CommonRoundRobinRule();
            rule.initWithNiwsConfig(clientConfig);
            baseLoadBalancer.setRule(rule);
        }

        return null;
    }

    private List<Server> getLoadBalanceServers(String originalURL) {
        List<Server> resultList = new ArrayList<>();
        String[] urlArray = originalURL.split(Constants.SERVER_ADDRESS_SPLITER);
        for (String url : urlArray) {
            String[] urlFactors = url.split(Constants.SERVER_ADDRESS_REGION_DATA_CENTER_SPLITER);
            String region = null;
            String dataCenterFlag = null;
            String id;

            if(urlFactors.length == 3) {
                region = StringUtils.trimToEmpty(urlFactors[0]);
                dataCenterFlag = StringUtils.trimToEmpty(urlFactors[1]);
                id = StringUtils.trimToEmpty(urlFactors[2]);
            } else if(urlFactors.length ==2) {
                String first = StringUtils.trimToEmpty(urlFactors[0]);
                if(!first.equalsIgnoreCase(Constants.DATA_CENTER_INDICATOR_PRIMARY)
                    && !first.equalsIgnoreCase(Constants.DATA_CENTER_INDICATOR_SECONDARY)) {
                    region = first; // region#server
                } else {
                    dataCenterFlag = first; // data center# server
                }
                id = StringUtils.trimToEmpty(urlFactors[1]);;
            } else if(urlFactors.length == 1) {
                id = StringUtils.trimToEmpty(urlFactors[0]);
            } else {
                throw new RuntimeException("Invalid server address -" + originalURL);
            }

            ServiceServer server = new ServiceServer(id);

            if(StringUtils.isNotBlank(region)) {
                server.setZone(region);
            }

            if(StringUtils.isNotBlank(dataCenterFlag)) {
                server.setDataCenter(dataCenterFlag);
            }
            resultList.add(server);
        }
        return resultList;
    }

    private void setupSystemConfig(DefaultClientConfigImpl config) {

    }
    private void setupCustomizeConfig(DefaultClientConfigImpl config) {
        config.setProperty(CommonClientConfigKey.NFLoadBalancerPingInterval, 30);
//        config.setProperty(CommonClientConfigKey.ServerListRefreshInterval, 300 * 100);

    }

    public ServicePingCheck getServicePingCheck() {
        return servicePingCheck;
    }

    public void setServicePingCheck(ServicePingCheck servicePingCheck) {
        this.servicePingCheck = servicePingCheck;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isRegionBasedEnabled() {
        return regionBasedEnabled;
    }

    public void setRegionBasedEnabled(boolean regionBasedEnabled) {
        this.regionBasedEnabled = regionBasedEnabled;
    }

    public String getClientLocationRegion() {
        if(StringUtils.isNotBlank(clientLocationRegion)) {
            String region = RegionUtil.determineClientRegion();
            return region;
        }
        return clientLocationRegion;
    }

    public void setClientLocationRegion(String clientLocationRegion) {
        this.clientLocationRegion = clientLocationRegion;
    }
}
