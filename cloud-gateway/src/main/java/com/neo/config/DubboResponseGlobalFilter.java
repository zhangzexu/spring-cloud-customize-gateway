package com.neo.config;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * @ClassName DubboResponseGlobalFilter
 * @Desription 协议转换的过滤器类
 * @Author zhangzexu
 * @Date 2019/11/28 17:14
 * @Version V1.0
 */
@Configuration
public class DubboResponseGlobalFilter implements GlobalFilter, Ordered {

    @Value("${plugin.calssName}")
    private String className;

    private static Logger LOGGER = LoggerFactory.getLogger(DubboResponseGlobalFilter.class);
    private volatile List<HttpHeadersFilter> headersFilters;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public DubboResponseGlobalFilter() {

    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);


        final String scheme = requestUrl.getScheme();
        if (isAlreadyRouted(exchange) || "http".equals(scheme) || "https".equals(scheme) || "lb".equals(scheme) || "ws".equals(scheme)) {
            return chain.filter(exchange);
        }
        LOGGER.info("请求的url为{},协议为{}",requestUrl,scheme);
        setAlreadyRouted(exchange);
        /**
         * 获取请求的url 对路径进行重新编码
         */
        final String url = requestUrl.toASCIIString();

        Flux<DataBuffer> flux = exchange.getRequest().getBody();
        AtomicReference<byte[]> atomicReference = new AtomicReference<>();
        /**
         * 获取客户端请求的数据，body体
         */
        flux.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            atomicReference.set(bytes);
        });
        return chain.filter(exchange)
                .then(Mono.defer(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    return response.writeWith(Flux.create(sink -> {
                        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
                        JSONObject json = new JSONObject();
                        Class c = null;
                        DataBuffer dataBuffer = null;
                        String charset = "UTF-8";
                        try {
                            /**
                             * 初始化反射数据，将要调用的类反射获取，反射的类的名称结构，
                             * 用 dubbo 协议举例
                             * 则插件的类名组合为 DubboGatewayImpl
                             */
                            StringBuilder sb = new StringBuilder(className);
                            sb.append(".");
                            char[] name = scheme.toCharArray();
                            name[0] -= 32;
                            sb.append(String.valueOf(name));
                            sb.append("GatewayPluginImpl");
                            c = Class.forName(sb.toString());
                            c.getMethods();
                            Method method = c.getMethod("send", String.class, byte[].class);
                            Object obj = c.getConstructor().newInstance();
                            Object result = method.invoke(obj, url, atomicReference.get());
                            HttpStatus status = HttpStatus.resolve(500);
                            /**
                             * 判断结果是否返回，如果没有数据则直接返回
                             */
                            if (result == null) {

                            } else {
                                json = (JSONObject) result;
                                status = HttpStatus.resolve(json.getInteger("code"));
                                json.remove("code");
                                /**
                                 * 获取字符集编码格式 默认 utf-8
                                 */
                                if (!StringUtils.isEmpty(json.getString("charset"))) {
                                    charset = json.getString("charset");
                                }
                            }
                            response.setStatusCode(status);
                            try {
                                dataBuffer = nettyDataBufferFactory.wrap(json.toJSONString().getBytes(charset));
                            } catch (UnsupportedEncodingException e) {
                                dataBuffer = nettyDataBufferFactory.wrap(e.toString().getBytes(charset));
                                LOGGER.error("返回调用请求数据错误{}",e);
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            try {
                                dataBuffer = nettyDataBufferFactory.wrap(e.toString().getBytes(charset));
                                LOGGER.error("获取远程数据错误{}",e);
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                                LOGGER.error("返回调用请求数据错误{}",ex);
                            }
                            e.printStackTrace();
                        }
                        /**
                         * 将数据进行发射到下一个过滤器
                         */
                        sink.next(dataBuffer);
                        sink.complete();
                    }));

                }));
    }
}
