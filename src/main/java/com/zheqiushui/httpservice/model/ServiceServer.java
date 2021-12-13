package com.zheqiushui.httpservice.model;

import com.netflix.loadbalancer.Server;
import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 19:35
 */
public class ServiceServer extends Server {
    private String dataCenter;

    public ServiceServer(String host, int port) {
        super(host, port);
    }

    public ServiceServer(String scheme, String host, int port) {
        super(scheme, host, port);
    }

    public ServiceServer(String id) {
        super(id);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }
}
