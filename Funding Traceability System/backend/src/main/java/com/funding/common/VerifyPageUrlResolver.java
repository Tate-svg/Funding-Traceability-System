package com.funding.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 扫码核验页地址解析器
 *
 * 生成的二维码要能被任意设备扫描访问，因此不能写死 localhost（扫码设备上的 localhost 指向手机自己）。
 * 解析优先级：
 * 1. 显式配置 verify.page-base-url（公网域名 / 反向代理场景，通过环境变量注入）
 * 2. 反向代理转发头 X-Forwarded-Host / X-Forwarded-Proto
 * 3. 当前请求的 Origin / Referer（用户用什么地址打开系统，二维码就用什么地址）
 * 4. 本机局域网 IPv4 + verify.page-port（在 localhost 上操作时兜底，同网段手机可直接扫码）
 * 5. 最后回退 localhost（无网络环境下的保守值）
 */
@Component
public class VerifyPageUrlResolver {

    /** 前端 hash 路由的公开核验页路径 */
    private static final String VERIFY_PATH = "/#/verify";

    @Value("${verify.page-base-url:}")
    private String configuredBase;

    @Value("${verify.page-port:8081}")
    private int pagePort;

    /**
     * 解析核验页基础地址（末尾不带斜杠），供拼接核验码使用
     */
    public String resolve() {
        String configured = normalize(configuredBase);
        if (!configured.isEmpty()) {
            return configured;
        }
        HttpServletRequest request = currentRequest();
        String forwarded = forwardedOrigin(request);
        if (forwarded != null) {
            return forwarded + VERIFY_PATH;
        }
        String origin = requestOrigin(request);
        if (origin != null) {
            return origin + VERIFY_PATH;
        }
        String lan = lanIp();
        if (lan != null) {
            return "http://" + lan + ":" + pagePort + VERIFY_PATH;
        }
        return "http://localhost:" + pagePort + VERIFY_PATH;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    /**
     * 反向代理场景：优先使用转发头，保持与用户实际访问地址一致
     */
    private String forwardedOrigin(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String host = firstNonBlank(request.getHeader("X-Forwarded-Host"));
        if (host == null) {
            return null;
        }
        String proto = firstNonBlank(request.getHeader("X-Forwarded-Proto"));
        return (proto == null ? "http" : proto.toLowerCase()) + "://" + host;
    }

    /**
     * 从 Origin / Referer 提取来源地址；本机回环地址不可用于扫码，返回 null 走局域网兜底
     */
    private String requestOrigin(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String origin = originOf(request.getHeader("Origin"));
        if (origin != null) {
            return origin;
        }
        return originOf(request.getHeader("Referer"));
    }

    private String originOf(String url) {
        String value = firstNonBlank(url);
        if (value == null) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            scheme = scheme.toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || isLoopbackHost(host)) {
                return null;
            }
            int port = uri.getPort();
            boolean defaultPort = ("http".equals(scheme) && (port == -1 || port == 80))
                    || ("https".equals(scheme) && (port == -1 || port == 443));
            return scheme + "://" + host + (port > 0 && !defaultPort ? ":" + port : "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isLoopbackHost(String host) {
        String value = host.toLowerCase();
        return "localhost".equals(value) || value.startsWith("127.") || "::1".equals(value) || "[::1]".equals(value);
    }

    /**
     * 取本机局域网 IPv4：跳过回环、虚拟网卡与点对点隧道，优先常见家用/办公网段
     */
    private String lanIp() {
        List<String> candidates = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isPointToPoint()) {
                    continue;
                }
                if (isVirtualInterface(network)) {
                    continue;
                }
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        candidates.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            return null;
        }
        if (candidates.isEmpty()) {
            return null;
        }
        for (String ip : candidates) {
            if (ip.startsWith("192.168.")) {
                return ip;
            }
        }
        for (String ip : candidates) {
            if (ip.startsWith("10.")) {
                return ip;
            }
        }
        return candidates.get(0);
    }

    private boolean isVirtualInterface(NetworkInterface network) {
        String name = network.getName() + " " + network.getDisplayName();
        name = name == null ? "" : name.toLowerCase();
        String[] virtualKeywords = {"vmware", "virtualbox", "vbox", "hyper-v", "vethernet", "wsl", "docker", "tun", "tap", "vpn", "loopback"};
        for (String keyword : virtualKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String trimmed = firstNonBlank(value);
        if (trimmed == null) {
            return "";
        }
        trimmed = trimmed.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String firstNonBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
