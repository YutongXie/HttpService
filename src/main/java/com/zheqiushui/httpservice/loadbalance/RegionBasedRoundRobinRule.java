package com.zheqiushui.httpservice.loadbalance;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.zheqiushui.httpservice.model.Constants;
import com.zheqiushui.httpservice.model.ServiceServer;
import com.zheqiushui.httpservice.util.RegionUtil;
import jdk.nashorn.internal.ir.ReturnNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/13 21:47
 */
public class RegionBasedRoundRobinRule extends RoundRobinRule {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AtomicInteger nextPrimaryServerCyclicCounter;
    private AtomicInteger nextNonPrimarySeverCyclicCounter;

    private String preferredRegion;

    public RegionBasedRoundRobinRule() {
        this.nextPrimaryServerCyclicCounter = new AtomicInteger(0);
        this.nextNonPrimarySeverCyclicCounter = new AtomicInteger(0);
    }

    public String getPreferredRegion() {
        return preferredRegion;
    }

    public void setPreferredRegion(String preferredRegion) {
        this.preferredRegion = preferredRegion;
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
//            List<Server> reachablePrimaryServers = getReachablePrimaryServer(reachableServers);
//            List<Server> reachableNonPrimaryServers = getReachableNonPrimaryServer(reachableServers);

            List<Server> allServers = loadBalancer.getAllServers();
            int upCount = reachableServers.size();
            int serverCount = allServers.size();

            if (upCount == 0 || serverCount == 0) {
                return null;
            }

            Set<String> availableRegions = getAvaialbeRegions(reachableServers);
            if (!CollectionUtils.isEmpty(availableRegions)) {
                if (availableRegions.contains(preferredRegion)) {
                    List<Server> servers = getCorrespondingRegionAvailableServers(reachableServers, preferredRegion);
                    server = getServer(servers);
                }

                if (server == null) {
                    String nextPreferredRegion = routingBasedChooseRegion(preferredRegion, availableRegions);
                    if (StringUtils.isNotBlank(nextPreferredRegion)) {
                        List<Server> servers = getCorrespondingRegionAvailableServers(reachableServers, nextPreferredRegion);
                        server = getServer(servers);

                        if (server == null) {
                            String anotherRegion = routingBasedChooseRegion(nextPreferredRegion, availableRegions);
                            List<Server> anotherServers = getCorrespondingRegionAvailableServers(reachableServers, anotherRegion);
                            server = getServer(anotherServers);
                        }
                    }
                }
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

    private String routingBasedChooseRegion(String preferredRegion, Set<String> availableRegions) {
        if (CollectionUtils.isEmpty(availableRegions)) {
            return null;
        }
        String selectedRegion = availableRegions.iterator().next();
        if (availableRegions.size() == 1) {
            return selectedRegion;
        }

        return RegionUtil.getBestRegion(preferredRegion, availableRegions);
    }

    private List<Server> getCorrespondingRegionAvailableServers(List<Server> servers, String region) {
        return servers.stream().filter(server -> region.equalsIgnoreCase(server.getZone())).collect(Collectors.toList());
    }

    private Set<String> getAvaialbeRegions(List<Server> servers) {
        Set<String> availableRegions = new HashSet<>();
        servers.forEach(server -> {
            if (StringUtils.isNotBlank(server.getZone())) {
                availableRegions.add(server.getZone());
            }
        });
        return availableRegions;
    }

    private Server getServer(List<Server> reachableServers) {
        if (CollectionUtils.isEmpty(reachableServers)) {
            return null;
        }

        Server server = null;
        List<Server> reachablePrimaryServers = getReachablePrimaryServer(reachableServers);
        List<Server> reachableNonPrimaryServers = getReachableNonPrimaryServer(reachableServers);

        if (!CollectionUtils.isEmpty(reachablePrimaryServers)) {
            int nextIndex = incrementAndGetPrimaryModulo(reachablePrimaryServers.size());
            server = reachablePrimaryServers.get(nextIndex);
        }

        if (server == null && !CollectionUtils.isEmpty(reachableNonPrimaryServers)) {
            int nextIndex = incrementAndGetNonPrimaryModulo(reachableNonPrimaryServers.size());
            server = reachableNonPrimaryServers.get(nextIndex);
        }

        return server;
    }
}
