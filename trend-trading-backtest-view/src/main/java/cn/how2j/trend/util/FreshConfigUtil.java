package cn.how2j.trend.util;

import cn.hutool.http.HttpUtil;

import java.util.HashMap;

/**
 * 在配置信息更新时，启动该类，使用post方式访问http://localhost:8041/actuator/bus-refresh 地址
 */
public class FreshConfigUtil {
    public static void main(String[] args) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        String result = HttpUtil.createPost("http://localhost:8011/actuator/bus-refresh").addHeaders(headers)
                .execute().body();
        System.out.println("result: " + result);
        System.out.println("refresh 完成");

    }
}
