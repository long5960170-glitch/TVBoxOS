package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.Intent;

import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 家庭版配置源：GitHub / 局域网 NAS / 电脑配置管家。
 * 只用于本机家庭环境，不对外分发。
 */
public class FamilyConfig {
    public static final int SOURCE_GITHUB = 0;
    public static final int SOURCE_NAS = 1;
    public static final int SOURCE_PC = 2;

    public static final String KEY_GITHUB = "family_source_github";
    public static final String KEY_NAS = "family_source_nas";
    public static final String KEY_PC = "family_source_pc";

    // 云端配置固定文件名 my.json；换配置时覆盖它即可，盒子地址不用改。
    public static final String DEFAULT_GITHUB =
            "https://ghfast.top/https://raw.githubusercontent.com/long5960170-glitch/tvbox-config/main/tvbox/my.json";
    public static final String GITHUB_DIRECT =
            "https://raw.githubusercontent.com/long5960170-glitch/tvbox-config/main/tvbox/my.json";
    // 电脑配置管家的局域网地址；电脑 IP 变了可在电视上长按修改。
    public static final String DEFAULT_PC = "http://192.168.100.100:8899/c/default.json";
    public static final String DEFAULT_NAS = "";

    public static class Source {
        public final int key;
        public final String label;
        public final String url;

        public Source(int key, String label, String url) {
            this.key = key;
            this.label = label;
            this.url = url == null ? "" : url.trim();
        }
    }

    public static void ensureDefaults() {
        if (!Hawk.contains(KEY_GITHUB)) Hawk.put(KEY_GITHUB, DEFAULT_GITHUB);
        if (!Hawk.contains(KEY_NAS)) Hawk.put(KEY_NAS, DEFAULT_NAS);
        if (!Hawk.contains(KEY_PC)) Hawk.put(KEY_PC, DEFAULT_PC);

        // 第一次安装、还没配置过地址时，默认走 GitHub，装上就能看。
        String current = Hawk.get(HawkConfig.API_URL, "");
        if (current == null || current.trim().isEmpty()) {
            String url = get(SOURCE_GITHUB).url;
            if (!url.isEmpty()) {
                Hawk.put(HawkConfig.API_URL, url);
                Hawk.put(HawkConfig.LIVE_API_URL, url);
                HistoryHelper.setApiHistory(url);
                HistoryHelper.setLiveApiHistory(url);
            }
        }
    }

    public static Source get(int key) {
        if (key == SOURCE_NAS) {
            return new Source(key, "局域网 NAS", Hawk.get(KEY_NAS, DEFAULT_NAS));
        }
        if (key == SOURCE_PC) {
            return new Source(key, "电脑管家", Hawk.get(KEY_PC, DEFAULT_PC));
        }
        return new Source(SOURCE_GITHUB, "GitHub 云端", Hawk.get(KEY_GITHUB, DEFAULT_GITHUB));
    }

    public static void setNas(String url) {
        Hawk.put(KEY_NAS, url == null ? "" : url.trim());
    }

    public static void setPc(String url) {
        Hawk.put(KEY_PC, url == null ? "" : url.trim());
    }

    /**
     * 依次尝试：GitHub 加速、GitHub 直连、NAS、电脑管家。
     * 返回第一个能打开的地址；全都不通返回 null。
     */
    public static Source autoPick() {
        Source github = get(SOURCE_GITHUB);
        if (!github.url.isEmpty() && probe(github.url)) return github;
        if (probe(GITHUB_DIRECT)) {
            return new Source(SOURCE_GITHUB, "GitHub 直连", GITHUB_DIRECT);
        }
        Source nas = get(SOURCE_NAS);
        if (!nas.url.isEmpty() && probe(nas.url)) return nas;
        Source pc = get(SOURCE_PC);
        if (!pc.url.isEmpty() && probe(pc.url)) return pc;
        return null;
    }

    public static boolean probe(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url.trim()).openConnection();
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TVBoxFamily/1.0");
            conn.setRequestProperty("Cache-Control", "no-cache");
            // 只读开头一小段，够判断能不能连通即可。
            conn.setRequestProperty("Range", "bytes=0-2047");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 保存选中的地址，清掉该地址的旧缓存，然后重新启动首页强制重新拉取。
     */
    public static void apply(Context context, String url) {
        String value = url == null ? "" : url.trim();
        if (value.isEmpty()) return;

        Hawk.put(HawkConfig.API_URL, value);
        Hawk.put(HawkConfig.LIVE_API_URL, value);
        HistoryHelper.setApiHistory(value);
        HistoryHelper.setLiveApiHistory(value);

        // API_URL 对应的配置缓存；删掉可避免刚换源还显示旧内容。
        try {
            String cacheName = MD5.encode(value);
            if (cacheName != null) {
                File cache = new File(context.getFilesDir(), cacheName);
                if (cache.exists()) cache.delete();
            }
        } catch (Throwable ignored) {
        }

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("useCache", false);
        context.startActivity(intent);
    }
}
