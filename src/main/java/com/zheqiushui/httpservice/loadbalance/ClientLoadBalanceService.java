package com.zheqiushui.httpservice.loadbalance;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.event.PrintJobAttributeEvent;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 22:32
 */
public class ClientLoadBalanceService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ILoadBalancer loadBalancer;

    public Server chooseServer() throws Exception {
        int retryTimes = 0;
        Server server = null;
        while (server == null && retryTimes < 5) {
            server = loadBalancer.chooseServer(null);
            retryTimes++;
        }
        if (server == null) {
            throw new Exception("no available service...");
        }
        return server;
    }

    public ILoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(ILoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
}
