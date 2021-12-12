package com.zheqiushui.httpservice.model;
//import org.netflix.loadbalancer.Server;

import com.netflix.loadbalancer.Server;
import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 19:35
 */
@Data
public class HttpServer extends Server {

    private String dataCenter;
    public HttpServer(String host, int port) {
        super(host, port);
    }

    public HttpServer(String scheme, String host, int port) {
        super(scheme, host, port);
    }

    public HttpServer(String id) {
        super(id);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
