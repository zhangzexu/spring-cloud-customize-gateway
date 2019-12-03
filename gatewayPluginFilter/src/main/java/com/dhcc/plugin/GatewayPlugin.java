package com.dhcc.plugin;

import com.alibaba.fastjson.JSONObject;

/**
 * @Title
 * @ClassName GatewayPlugin
 * @Desription
 * @Author zhangzexu
 * @Date 2019/12/3 09:12
 * @Version V1.0
 */
public interface GatewayPlugin {
    public JSONObject send(String url, byte[] Bytes);
}
