package com.zheqiushui.httpservice.model;

import org.eclipse.jetty.http.HttpVersion;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 19:22
 */
public class Constants {

    public static final String REQUEST_PATH_PATTERN_ROOT = "/*";
    public static final String REQUEST_PATH_PATTERN_HEALTH_CHECK = "/healthcheck/*";
    public static final String REQUEST_PATH_PATTERN_BUSINESS = "/reqeust/*";

    public static final String REQUEST_PATH_HEALTH_CHECK = "/healthcheck/";
    public static final String REQUEST_PATH_BUSINESS = "/request";

    public static final String DOS_FILTER_DEFAULT_DELAY_MS = "-1";
    public static final String DOS_FILTER_DEFAULT_MAX_REQUET_PERSEC = "50";

    public static final String SERVER_ADDRESS_SPLITER = ",";
    public static final String SERVER_ADDRESS_REGION_DATA_CENTER_SPLITER = "#";

    public static final String CLIENT_LOCATED_REGION_US_LATM = "uslatm";
    public static final String CLIENT_LOCATED_REGION_APAC = "apac";
    public static final String CLIENT_LOCATED_REGION_EMEA = "emea";

    public static final int REQUEST_BODY_SIZE_LIMIT_BUSINESS = 65536;
    public static final int REQUEST_BODY_SIZE_LIMIT_HEALTH_CHECK = 1;
    public static final int REQUEST_HEADER_SIZE_LIMIT = 8192;

    public static final String[] COMPRESSION_GZIP_INCLUDED_METHODS = {"GET", "PUT"};
    public static final String[] COMPRESSION_GZIP_INCLUDED_MIME_TYPES = {"text/html", "text/xml", "applicatoin/json"};
    public static final String[] COMPRESSION_GZIP_INCLUDED_PATH = {"/*"};

    public static final String[] SSL_EXCLUDE_PROTOCOLS = {"SSLv3"};
    public static final String[] SSL_EXCLUDE_CIPHERSUITES = {};

    public static final String HTTP_PROTOCOL_1_1 = HttpVersion.HTTP_1_1.asString();

    public static final String DATA_CENTER_INDICATOR_PRIMARY = "p";
    public static final String DATA_CENTER_INDICATOR_SECONDARY = "s";

}
