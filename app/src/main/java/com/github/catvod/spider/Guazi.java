package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.net.OkResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Guazi extends Spider {

    private String[] hosts = {
            "https://apinew.uozvr.com",
            "https://api.w32z7vtd.com",
            "https://api.6a7nnf7.com",
            "https://api.umygrx3.com",
            "https://api.rmedphk.com"
    };
    private int hostIndex = 0;
    private String host = hosts[hostIndex];

    // AES 固定密钥
    private static final String AES_KEY = "OITxa5OqAYjhswxx";
    private static final String AES_IV = "rCMNwZASNBKZ8mXV";

    // RSA 公钥/私钥
    private static final String RSA_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDUM5+/y8sPsWkd1/RQS64X259EUwxFXFE5HlA65MqrxnPs0JqoSRojSDy5QhwvROlaD6TwRQHKMY2OAZ6SnQeUJsChTEFIR9qUkwrs3/MVUMxjsv6JS6Oe/juclyJGTgVmDhB55EafXsD0SQYVj/QXXsxR6ewR5E2kL52yAAD4yQIDAQAB";
    private static final String RSA_PRIVATE_KEY =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGAe6hKrWLi1zQmjTT1\n" +
                    "ozbE4QdFeJGNxubxld6GrFGximxfMsMB6BpJhpcTouAqywAFppiKetUBBbXwYsYU\n" +
                    "1wNr648XVmPmCMCy4rY8vdliFnbMUj086DU6Z+/oXBdWU3/b1G0DN3E9wULRSwcK\n" +
                    "ZT3wj/cCI1vsCm3gj2R5SqkA9Y0CAwEAAQKBgAJH+4CxV0/zBVcLiBCHvSANm0l7\n" +
                    "HetybTh/j2p0Y1sTXro4ALwAaCTUeqdBjWiLSo9lNwDHFyq8zX90+gNxa7c5EqcW\n" +
                    "V9FmlVXr8VhfBzcZo1nXeNdXFT7tQ2yah/odtdcx+vRMSGJd1t/5k5bDd9wAvYdI\n" +
                    "DblMAg+wiKKZ5KcdAkEA1cCakEN4NexkF5tHPRrR6XOY/XHfkqXxEhMqmNbB9U34\n" +
                    "saTJnLWIHC8IXys6Qmzz30TtzCjuOqKRRy+FMM4TdwJBAJQZFPjsGC+RqcG5UvVM\n" +
                    "iMPhnwe/bXEehShK86yJK/g/UiKrO87h3aEu5gcJqBygTq3BBBoH2md3pr/W+hUM\n" +
                    "WBsCQQChfhTIrdDinKi6lRxrdBnn0Ohjg2cwuqK5zzU9p/N+S9x7Ck8wUI53DKm8\n" +
                    "jUJE8WAG7WLj/oCOWEh+ic6NIwTdAkEAj0X8nhx6AXsgCYRql1klbqtVmL8+95KZ\n" +
                    "K7PnLWG/IfjQUy3pPGoSaZ7fdquG8bq8oyf5+dzjE/oTXcByS+6XRQJAP/5ciy1b\n" +
                    "L3NhUhsaOVy55MHXnPjdcTX0FaLi+ybXZIfIQ2P4rb19mVq1feMbCXhz+L1rG8oa\n" +
                    "t5lYKfpe8k83ZA==\n" +
                    "-----END RSA PRIVATE KEY-----";

    private static final String DEVICE_OLD_KEY = "aLFBMWpxBrIDAD1Si/KVvm41";

    private String deviceId;
    private String deviceKey;
    private String token = "";
    private String tokenId = "";
    private boolean registered = false;

    private Map<String, String> headers;
    private Map<String, Object> cache = new HashMap<>();
    private long cacheTimeout = 300; // 秒
    private Gson gson = new Gson();

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);

        // 随机生成设备信息
        Random rand = new Random();
        deviceId = String.valueOf(864150060000000L + rand.nextInt(10000));
        deviceKey = generateHexString(40); // 20字节

        headers = new HashMap<>();
        headers.put("User-Agent", "Lavf/57.83.100");
        headers.put("code", "GZ0369");
        headers.put("deviceId", deviceId);
        headers.put("lang", "zh_cn");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Version", "2604028");
        headers.put("PackageName", "com.ae06aebdbb.y286327f5a.ofe849883320260517");
        headers.put("Ver", "3.0.3.2");
        headers.put("api-ver", "3.0.3.2");
        headers.put("Referer", host);

        initToken();
    }

    // 注意：基类中没有 getName() 方法，所以删除 @Override
    public String getName() {
        return "瓜子";
    }

    // ---------- 设备认证 ----------
    private void initToken() {
        try {
            if (!registered) signUp();
            refreshToken();
        } catch (Exception e) {
            e.printStackTrace();
            // 兜底token（实际可能失效）
            token = "024212ef0975c5306a1434e113a46463.bc77313e11a248558a6ca244ca980944ec3421fa480c50e0229ad91f1cb15aea582603202cd71796885c9e5163e500f1b72f737059aff1ddb8beea47c5a331d6760540345b7f88b2302a0e6e09589f9dcf3ff9175d8c905f990203f5fc04748008ea7a366571cbf5b09509a873dcfba3cf1d559038f5f5f7ef6e01d1850974aa220eb5178c89e61c24411af9b9a19435e.06fde789ece48d9b33c5dc857e04e9b5838f08264d928b87237d3476c4484b46";
        }
    }

    private void signUp() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("new_key", deviceKey);
        params.put("old_key", DEVICE_OLD_KEY);
        params.put("phone_type", "1");
        params.put("code", "");
        Map<String, Object> result = authRequest("/App/Authentication/Device/signUp", params);
        applyAuth(result);
        registered = true;
    }

    private void signIn() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("new_key", deviceKey);
        params.put("old_key", DEVICE_OLD_KEY);
        Map<String, Object> result = authRequest("/App/Authentication/Device/signIn", params);
        applyAuth(result);
    }

    private void refreshToken() throws Exception {
        Map<String, Object> result = authRequest("/App/Authentication/Authenticator/refresh", new HashMap<>());
        applyAuth(result);
    }

    private void applyAuth(Map<String, Object> result) {
        if (result == null) return;
        String newToken = (String) result.get("token");
        if (!TextUtils.isEmpty(newToken)) token = newToken;
        String newTokenId = (String) result.get("app_user_id");
        if (!TextUtils.isEmpty(newTokenId)) tokenId = newTokenId;
    }

    private void ensureToken() throws Exception {
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(tokenId)) {
            if (registered) signIn();
            else signUp();
            refreshToken();
        }
    }

    // ---------- 核心请求 ----------
    private Map<String, Object> authRequest(String path, Map<String, String> params) throws Exception {
        return sendEncryptedRequest(params, path, true);
    }

    private Map<String, Object> getData(Map<String, String> data, String path) throws Exception {
        return getData(data, path, true);
    }

    private Map<String, Object> getData(Map<String, String> data, String path, boolean useCache) throws Exception {
        String cacheKey = path + "_" + data.hashCode();
        if (useCache && cache.containsKey(cacheKey)) {
            Object[] entry = (Object[]) cache.get(cacheKey);
            long timestamp = (long) entry[1];
            if (System.currentTimeMillis() / 1000 - timestamp < cacheTimeout) {
                return (Map<String, Object>) entry[0];
            }
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            for (int i = 0; i < hosts.length; i++) {
                host = hosts[hostIndex];
                headers.put("Referer", host);
                try {
                    Map<String, Object> result = sendEncryptedRequest(data, path, false);
                    if (result != null) {
                        if (useCache) cache.put(cacheKey, new Object[]{result, System.currentTimeMillis() / 1000});
                        return result;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hostIndex = (hostIndex + 1) % hosts.length;
            }
            // 重试前刷新token
            if (attempt < 2) {
                try { ensureToken(); } catch (Exception ignored) {}
                hostIndex = 0;
            }
        }
        return null;
    }

    private Map<String, Object> sendEncryptedRequest(Map<String, String> data, String path, boolean isAuth) throws Exception {
        if (!isAuth) ensureToken();

        // 1. AES加密参数
        String jsonParams = gson.toJson(data);
        String requestKey = aesEncrypt(jsonParams, AES_KEY, AES_IV).toUpperCase();

        // 2. RSA加密 keys
        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("iv", AES_IV);
        keyMap.put("key", AES_KEY);
        String keysJson = gson.toJson(keyMap);
        String keys = rsaEncrypt(keysJson, RSA_PUBLIC_KEY);

        // 3. 生成签名
        String t = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = "token_id=,token=" + token + ",phone_type=1,request_key=" + requestKey +
                ",app_id=1,time=" + t + ",keys=" + keys + "*&zvdvdvddbfikkkumtmdwqppp?|4Y!s!2br";
        String signature = md5(signStr).toUpperCase();

        // 4. 构建请求体
        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("token_id", "");
        body.put("phone_type", "1");
        body.put("time", t);
        body.put("phone_model", "xiaomi-25031");
        body.put("keys", keys);
        body.put("request_key", requestKey);
        body.put("signature", signature);
        body.put("app_id", "1");
        body.put("ad_version", "1");

        // 5. 发送POST
        String url = host + path;
        OkResult result = OkHttp.post(url, body, headers);
        if (result.getCode() != 200) {
            throw new Exception("HTTP " + result.getCode());
        }

        Map<String, Object> resp = gson.fromJson(result.getBody(), new TypeToken<Map<String, Object>>(){}.getType());
        if (resp.containsKey("code") && !"200".equals(String.valueOf(resp.get("code")))) {
            throw new Exception("业务错误: " + resp.get("code"));
        }

        Map<String, Object> dataSection = (Map<String, Object>) resp.get("data");
        if (dataSection == null) throw new Exception("缺少data字段");

        String encryptedResponse = (String) dataSection.get("response_key");
        String encryptedKeys = (String) dataSection.get("keys");

        // 6. 解密响应
        String decryptedKeysJson = rsaDecrypt(encryptedKeys, RSA_PRIVATE_KEY);
        Map<String, String> keyInfo = gson.fromJson(decryptedKeysJson, new TypeToken<Map<String, String>>(){}.getType());
        String respKey = keyInfo.get("key");
        String respIv = keyInfo.get("iv");
        String decryptedData = aesDecrypt(encryptedResponse, respKey, respIv);
        return gson.fromJson(decryptedData, new TypeToken<Map<String, Object>>(){}.getType());
    }

    // ---------- 加解密工具 ----------
    private String aesEncrypt(String text, String key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        return bytesToHex(encrypted).toUpperCase();
    }

    private String aesDecrypt(String text, String key, String iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = hexToBytes(text);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    private String rsaEncrypt(String text, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.decode(publicKeyStr, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = factory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.DEFAULT).replace("\n", "");
    }

    private String rsaDecrypt(String encryptedData, String privateKeyStr) throws Exception {
        String pem = privateKeyStr.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\n", "");
        byte[] keyBytes = Base64.decode(pem, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = factory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encrypted = Base64.decode(encryptedData, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    private String md5(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(text.getBytes("UTF-8"));
        return bytesToHex(digest);
    }

    private String generateHexString(int length) {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("0123456789ABCDEF".charAt(rand.nextInt(16)));
        }
        return sb.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // ---------- TVBox 业务方法 ----------
    @Override
    public String homeContent(boolean filter) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> classes = new ArrayList<>();
        String[][] classData = {{"1", "电影"}, {"2", "电视剧"}, {"4", "动漫"}, {"3", "综艺"}, {"64", "短剧"}};
        for (String[] cls : classData) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("type_name", cls[1]);
            item.put("type_id", cls[0]);
            classes.add(item);
        }
        result.put("class", classes);

        // 筛选
        Map<String, List<Map<String, Object>>> filters = new LinkedHashMap<>();
        for (String[] cls : classData) {
            String tid = cls[0];
            List<Map<String, Object>> filterList = new ArrayList<>();

            Map<String, Object> area = new LinkedHashMap<>();
            area.put("key", "area");
            area.put("name", "地区");
            List<Map<String, String>> areaValues = new ArrayList<>();
            areaValues.add(createValue("全部", "0"));
            areaValues.add(createValue("大陆", "大陆"));
            areaValues.add(createValue("香港", "香港"));
            areaValues.add(createValue("台湾", "台湾"));
            areaValues.add(createValue("美国", "美国"));
            areaValues.add(createValue("韩国", "韩国"));
            areaValues.add(createValue("日本", "日本"));
            areaValues.add(createValue("英国", "英国"));
            areaValues.add(createValue("法国", "法国"));
            areaValues.add(createValue("泰国", "泰国"));
            areaValues.add(createValue("印度", "印度"));
            areaValues.add(createValue("其他", "其他"));
            area.put("value", areaValues);
            filterList.add(area);

            Map<String, Object> year = new LinkedHashMap<>();
            year.put("key", "year");
            year.put("name", "年份");
            List<Map<String, String>> yearValues = new ArrayList<>();
            yearValues.add(createValue("全部", "0"));
            for (int y = 2025; y >= 2005; y--) yearValues.add(createValue(String.valueOf(y), String.valueOf(y)));
            yearValues.add(createValue("更早", "2004"));
            year.put("value", yearValues);
            filterList.add(year);

            Map<String, Object> sort = new LinkedHashMap<>();
            sort.put("key", "sort");
            sort.put("name", "排序");
            List<Map<String, String>> sortValues = new ArrayList<>();
            sortValues.add(createValue("最新", "d_id"));
            sortValues.add(createValue("最热", "d_hits"));
            sortValues.add(createValue("推荐", "d_score"));
            sort.put("value", sortValues);
            filterList.add(sort);

            filters.put(tid, filterList);
        }
        result.put("filters", filters);
        return gson.toJson(result);
    }

    private Map<String, String> createValue(String n, String v) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("n", n);
        item.put("v", v);
        return item;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("area", extend.getOrDefault("area", "0"));
        body.put("year", extend.getOrDefault("year", "0"));
        body.put("pageSize", "30");
        body.put("sort", extend.getOrDefault("sort", "d_id"));
        body.put("page", pg);
        body.put("tid", tid);

        Map<String, Object> data = getData(body, "/App/IndexList/indexList");
        List<Map<String, String>> videos = new ArrayList<>();
        if (data != null && data.containsKey("list")) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
            for (Map<String, Object> item : list) {
                int continu = ((Number) item.getOrDefault("vod_continu", 0)).intValue();
                String remarks = continu == 0 ? "电影" : "更新至" + continu + "集";
                Map<String, String> video = new LinkedHashMap<>();
                video.put("vod_id", item.get("vod_id") + "/" + continu);
                video.put("vod_name", (String) item.getOrDefault("vod_name", ""));
                video.put("vod_pic", (String) item.getOrDefault("vod_pic", ""));
                video.put("vod_remarks", remarks);
                videos.add(video);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", videos);
        result.put("page", Integer.parseInt(pg));
        result.put("pagecount", 9999);
        result.put("limit", 30);
        result.put("total", 999999);
        return gson.toJson(result);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String vodId = ids.get(0).split("/")[0];
        String t = String.valueOf(System.currentTimeMillis() / 1000);

        Map<String, String> body1 = new HashMap<>();
        body1.put("token_id", tokenId);
        body1.put("vod_id", vodId);
        body1.put("mobile_time", t);
        body1.put("token", token);
        Map<String, Object> qdata = getData(body1, "/App/IndexPlay/playInfo", false);

        Map<String, String> body2 = new HashMap<>();
        body2.put("vurl_cloud_id", "2");
        body2.put("vod_d_id", vodId);
        Map<String, Object> jdata = getData(body2, "/App/Resource/Vurl/show", false);

        if (qdata == null || !qdata.containsKey("vodInfo")) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("list", new ArrayList<>());
            return gson.toJson(emptyResult);
        }

        Map<String, Object> vodInfo = (Map<String, Object>) qdata.get("vodInfo");
        Map<String, Object> vod = new LinkedHashMap<>();
        vod.put("vod_id", vodId);
        vod.put("vod_name", vodInfo.getOrDefault("vod_name", ""));
        vod.put("vod_pic", vodInfo.getOrDefault("vod_pic", ""));
        vod.put("vod_year", vodInfo.getOrDefault("vod_year", ""));
        vod.put("vod_area", vodInfo.getOrDefault("vod_area", ""));
        vod.put("vod_actor", vodInfo.getOrDefault("vod_actor", ""));
        vod.put("vod_director", vodInfo.getOrDefault("vod_director", ""));
        vod.put("vod_content", ((String) vodInfo.getOrDefault("vod_use_content", "")).trim());
        vod.put("vod_play_from", "瓜子影视");

        List<String> playList = new ArrayList<>();
        if (jdata != null && jdata.containsKey("list")) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) jdata.get("list");
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> item = list.get(i);
                if (item.containsKey("play")) {
                    Map<String, Object> playMap = (Map<String, Object>) item.get("play");
                    List<String> names = new ArrayList<>();
                    List<String> params = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : playMap.entrySet()) {
                        Map<String, Object> val = (Map<String, Object>) entry.getValue();
                        if (val.containsKey("param") && val.get("param") != null) {
                            names.add(entry.getKey());
                            params.add((String) val.get("param"));
                        }
                    }
                    if (!params.isEmpty()) {
                        String playName = list.size() == 1 ? (String) vod.get("vod_name") : String.valueOf(i + 1);
                        String playUrl = params.get(params.size() - 1) + "||" + TextUtils.join("@", names);
                        playList.add(playName + "$" + playUrl);
                    }
                }
            }
        }
        vod.put("vod_play_url", TextUtils.join("#", playList));

        List<Map<String, Object>> resultList = new ArrayList<>();
        resultList.add(vod);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", resultList);
        return gson.toJson(result);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("keywords", key);
        body.put("order_val", "1");
        body.put("page", pg);

        Map<String, Object> data = getData(body, "/App/Index/findMoreVod", false);
        List<Map<String, String>> videos = new ArrayList<>();
        if (data != null && data.containsKey("list")) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
            for (Map<String, Object> item : list) {
                int continu = ((Number) item.getOrDefault("vod_continu", 0)).intValue();
                String remarks = continu == 0 ? "电影" : "更新至" + continu + "集";
                Map<String, String> video = new LinkedHashMap<>();
                video.put("vod_id", item.get("vod_id") + "/" + continu);
                video.put("vod_name", (String) item.getOrDefault("vod_name", ""));
                video.put("vod_pic", (String) item.getOrDefault("vod_pic", ""));
                video.put("vod_remarks", remarks);
                videos.add(video);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", videos);
        result.put("page", Integer.parseInt(pg));
        result.put("pagecount", 9999);
        result.put("limit", 30);
        result.put("total", 999999);
        return gson.toJson(result);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String[] parts = id.split("\\|\\|");
        if (parts.length < 2) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("parse", 0);
            emptyResult.put("playUrl", "");
            emptyResult.put("url", "");
            return gson.toJson(emptyResult);
        }
        String paramStr = parts[0];
        String[] resolutions = parts[1].split("@");
        Map<String, String> params = new HashMap<>();
        for (String pair : paramStr.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) params.put(kv[0], kv[1]);
        }
        if (resolutions.length > 0) {
            // 简单排序（按数字降序）
            List<String> resList = new ArrayList<>();
            for (String r : resolutions) if (r.matches("\\d+")) resList.add(r);
            resList.sort((a, b) -> Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));
            if (!resList.isEmpty()) {
                params.put("resolution", resList.get(0));
                Map<String, Object> data = getData(params, "/App/Resource/VurlDetail/showOne", false);
                if (data != null && data.containsKey("url")) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("parse", 0);
                    result.put("playUrl", "");
                    result.put("url", data.get("url"));
                    result.put("header", "{\"User-Agent\":\"Lavf/57.83.100\",\"Referer\":\"http://WJiZxLXA2.com/\"}");
                    result.put("danmaku", "http://127.0.0.1:9978/proxy?do=diydanmu");
                    return gson.toJson(result);
                }
            }
        }
        Map<String, Object> emptyResult = new LinkedHashMap<>();
        emptyResult.put("parse", 0);
        emptyResult.put("playUrl", "");
        emptyResult.put("url", "");
        return gson.toJson(emptyResult);
    }

    /**
     * 代理方法 - 注意基类中方法名是 proxy，不是 proxyLocal
     */
    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        return null;
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}
