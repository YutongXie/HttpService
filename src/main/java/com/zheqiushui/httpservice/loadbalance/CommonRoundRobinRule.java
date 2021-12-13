package com.zheqiushui.httpservice.loadbalance;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.model.Constants;
import com.zheqiushui.httpservice.model.ServiceServer;
import jdk.nashorn.internal.ir.ReturnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import sun.net.NetworkServer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/13 21:47
 */
public class CommonRoundRobinRule extends RoundRobinRule {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AtomicInteger nextPrimaryServerCyclicCounter;
    private AtomicInteger nextNonPrimarySeverCyclicCounter;

    public CommonRoundRobinRule() {
        this.nextPrimaryServerCyclicCounter = new AtomicInteger(0);
        this.nextNonPrimarySeverCyclicCounter = new AtomicInteger(0);
    }

    @Override
    public Server choose(ILoadBalancer loadBalancer, Object key) {
        if (loadBalancer == null) {
            return null;
        }

        Server server = null;
        int count = 0;
        while (count++ < 5) {
            List<Server> reachableServers = loadBalancer.getReachableServers();
            List<Server> reachablePrimaryServers = getReachablePrimaryServer(reachableServers);
            List<Server> reachableNonPrimaryServers = getReachableNonPrimaryServer(reachableServers);

            List<Server> allServers = loadBalancer.getAllServers();
            int upCount = reachableServers.size();
            int serverCount = allServers.size();

            if (upCount == 0 || serverCount == 0) {
                return null;
            }

            if (!CollectionUtils.isEmpty(reachablePrimaryServers)) {
                int nextServerIndex = incrementAndGetPrimaryModulo(reachablePrimaryServers.size());
                server = reachablePrimaryServers.get(nextServerIndex);
            }

            if (server == null && !CollectionUtils.isEmpty(reachableNonPrimaryServers)) {
                int nextServerIndex = incrementAndGetNonPrimaryModulo(reachableNonPrimaryServers.size());
                server = reachableNonPrimaryServers.get(nextServerIndex);
            }

            if (server == null) {
                Thread.yield();
                continue;
            }

            if (server.isAlive() && server.isReadyToServe()) {
                return server;
            }
            server = null;
        }
        if (count >= 5) {
            logger.warn("Still no available servers after retry 5 times");
        }
        return null;
    }

    private List<Server> getReachablePrimaryServer(List<Server> reachableServers) {
        if (CollectionUtils.isEmpty(reachableServers)) {
            return null;
        }
        return reachableServers.stream().filter(server -> {
            if (server instanceof ServiceServer && Constants.DATA_CENTER_INDICATOR_PRIMARY.equalsIgnoreCase(((ServiceServer) server).getDataCenter())) {
                return true;
            } else return false;
        }).collect(Collectors.toList());
    }

    private List<Server> getReachableNonPrimaryServer(List<Server> reachableServers) {
        if (CollectionUtils.isEmpty(reachableServers)) {
            return null;
        }
        return reachableServers.stream().filter(server -> {
            if (server instanceof ServiceServer && !Constants.DATA_CENTER_INDICATOR_PRIMARY.equalsIgnoreCase(((ServiceServer) server).getDataCenter())) {
                return true;
            } else return false;
        }).collect(Collectors.toList());
    }

    @Override
    public Server choose(Object key) {
        return choose(getLoadBalancer(), key);
    }

    private int incrementAndGetNonPrimaryModulo(int modulo) {
        for (; ; ) {
            int current = nextNonPrimarySeverCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextPrimaryServerCyclicCounter.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    private int incrementAndGetPrimaryModulo(int modulo) {
        for (; ; ) {
            int current = nextPrimaryServerCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextPrimaryServerCyclicCounter.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
