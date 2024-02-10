package com.zeeyeh.devtoolkit.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zeeyeh.devtoolkit.plugin.SimplePlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class UpdateUtil {

    private static final String SERVER_PROTOCOL = "http";
    private static final String SERVER_ADDR = "bmlingqi.gitee.io/api/bonfire_server/server.json";

    public static void getToken(String username, String email, String password) {
        JsonObject serverInfo = getServerInfo();
        String serverAddress = getServerAddress(serverInfo);
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        String token = HttpUtil.getToken(serverAddress + "/member/login", "POST", 10000, new HashMap<>() {
            {
                put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            }
        }, payload);
        try {
            File file = new File(SimplePlugin.getInstance().getDataFolder(), "data.dat");
            if (file.exists()) {
                file.delete();
            }
            if (token == null) {
                file.createNewFile();
                return;
            }
            byte[] encode = Base64.getEncoder().encode(token.getBytes());
            Files.write(file.toPath(), encode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getToken() {
        try {
            File file = new File(SimplePlugin.getInstance().getDataFolder(), "data.dat");
            if (!file.exists()) {
                return null;
            }
            String tokenBase64 = Files.readString(file.toPath());
            if (tokenBase64 == null || tokenBase64.isEmpty()) {
                return null;
            }
            String token = new String(Base64.getDecoder().decode(tokenBase64));
            String tokenContentBase64 = token.split("[.]")[1];
            String tokenContent = new String(Base64.getDecoder().decode(tokenContentBase64));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(tokenContent, JsonObject.class);
            long exp = jsonObject.get("exp").getAsLong();
            Date date = new Date(exp);
            long currentTimeMillis = System.currentTimeMillis();
            Date currentDate = new Date(currentTimeMillis);
            if (date.compareTo(currentDate) < 0) {
                return null;
            }
            return token;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject getServerInfo() {
        String response = HttpUtil.request(SERVER_PROTOCOL + "://" + SERVER_ADDR, "GET", 10000, StandardCharsets.UTF_8, new HashMap<>() {
            {
                put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            }
        }, null);
        Gson gson = new Gson();
        return gson.fromJson(response, JsonObject.class);
    }

    public static List<String> getUpdateTips(JsonObject jsonObject, String... params) {
        if (jsonObject == null) {
            return Collections.emptyList();
        }
        String updateTips = jsonObject.get("update-tips").getAsString();
        updateTips = parseParams(updateTips, params);
        if (updateTips.contains("\\n")) {
            String[] strings = updateTips.split("\\n");
            return Arrays.asList(strings);
        }
        return Collections.singletonList(updateTips);
    }

    public static List<String> getDisabledVersionTips(JsonObject jsonObject, String... params) {
        if (jsonObject == null) {
            return Collections.emptyList();
        }
        String disabledTips = jsonObject.get("disallowed-version-tips").getAsString();
        disabledTips = parseParams(disabledTips, params);
        if (disabledTips.contains("\\n")) {
            String[] strings = disabledTips.split("\\n");
            return Arrays.asList(strings);
        }
        return Collections.singletonList(disabledTips);
    }

    public static List<String> getDisallowedDeviceTips(JsonObject jsonObject, String... params) {
        if (jsonObject == null) {
            return Collections.emptyList();
        }
        String disabledTips = jsonObject.get("disallowed-device-tips").getAsString();
        disabledTips = parseParams(disabledTips, params);
        if (disabledTips.contains("\\n")) {
            String[] strings = disabledTips.split("\\n");
            return Arrays.asList(strings);
        }
        return Collections.singletonList(disabledTips);
    }

    protected static String parseParams(String content, String... params) {
        for (int i = 0; i < params.length; i++) {
            content = content.replace("{" + i + "}", params[i]);
        }
        return content;
    }

    public static String getServerAddress(JsonObject jsonObject) {
        return jsonObject.get("version-manager").getAsJsonObject().get("door").getAsString();
    }

    public static String getLatestVersionName(String name) {
        JsonObject projectInfo = getProjectInfo(name);
        if (projectInfo == null) {
            return null;
        }
        return projectInfo.get("name").getAsString();
    }

    public static String getLatestVersionUrl(String name) {
        JsonObject projectInfo = getProjectInfo(name);
        if (projectInfo == null) {
            return null;
        }
        return projectInfo.get("url").getAsString();
    }

    public static boolean checkLatest(String name, String version) {
        JsonObject dataObject = getProjectInfo(name);
        if (dataObject == null) {
            return false;
        }
        String latestVersion = dataObject.get("name").getAsString();
        return VersionUtil.compare(version, latestVersion) < 0;
    }

    public static boolean isAllow(String name) {
        JsonObject dataObject = getProjectInfo(name);
        if (dataObject == null) {
            return false;
        }
        int status = dataObject.get("status").getAsInt();
        return status == 0;
    }

    public static JsonObject getProjectInfo(String name) {
        JsonObject serverInfo = getServerInfo();
        String serverAddress = getServerAddress(serverInfo);
        serverAddress = serverAddress.endsWith("/") ? serverAddress.substring(0, serverAddress.length() - 1) : serverAddress;
        String saveToken = getToken();
        String token;
        if (saveToken == null) {
            String username = SimplePlugin.getInstance().getConfig().getString("username");
            String email = SimplePlugin.getInstance().getConfig().getString("email");
            String password = SimplePlugin.getInstance().getConfig().getString("password");
            getToken(username, email, password);
        }
        token = getToken();
        if (token == null) {
            return null;
        }
        String finalToken = token;
        String responseString = HttpUtil.request(serverAddress + "/version/getLatest", "POST", 100000, "UTF-8", new HashMap<>() {
            {
                put("Authorization", finalToken);
            }
        }, new HashMap<String, Object>() {
            {
                put("name", name);
            }
        });
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(responseString, JsonObject.class);
        int code = jsonObject.get("code").getAsInt();
        if (code != 0) {
            return null;
        }
        return jsonObject.get("data").getAsJsonObject();
    }

//    public static boolean checkDevices() {
//        String ip = SystemUtil.networkIp();
//        if (ip == null) {
//            return false;
//        }
//        String saveToken = getToken();
//        String token;
//        if (saveToken == null) {
//            String username = SimplePlugin.getInstance().getConfig().getString("username");
//            String email = SimplePlugin.getInstance().getConfig().getString("email");
//            String password = SimplePlugin.getInstance().getConfig().getString("password");
//            getToken(username, email, password);
//        }
//        token = getToken();
//        if (token == null) {
//            return false;
//        }
//        String finalToken = token;
//        String responseString = HttpUtil.request("/member/queryLogged", "POST", 10000, StandardCharsets.UTF_8, new HashMap<>() {
//            {
//                put("Authorization", finalToken);
//                put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
//            }
//        }, null);
//        Gson gson = new Gson();
//        JsonObject jsonObject = gson.fromJson(responseString, JsonObject.class);
//    }

    public static class HttpUtil {
        public static String request(String uri, String method, int timeout, String charset, Map<String, Object> headers, Object body) {
            return request(uri, method, timeout, Charset.forName(charset), headers, body);
        }

        public static String request(String uri, String method, int timeout, Charset charset, Map<String, Object> headers, Object body) {
            HttpURLConnection connection = null;
            DataOutputStream dataOutputStream = null;
            BufferedReader bufferedInputStream = null;
            StringBuilder builder = new StringBuilder();
            try
            {
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(timeout);
                if (headers != null) {
                    for (Map.Entry<String, Object> entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue().toString());
                    }
                }
                if (body != null) {
                    connection.setDoOutput(true);
                    String bodyJsonString = null;
                    if (body instanceof Map<?,?>) {
//                        bodyJsonString = paramBody((Map<String, Object>) body);
                        StringBuilder postData = new StringBuilder();
                        Map<String, Object> map = (Map<String, Object>) body;
                        for (Map.Entry<String, Object> param : map.entrySet()) {
                            if (postData.length() != 0) {
                                postData.append('&');
                            }
                            postData.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
                            postData.append('=');
                            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), StandardCharsets.UTF_8));
                        }
                        bodyJsonString = postData.toString();
                    }
                    try(OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(bodyJsonString.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                connection.connect();
                bufferedInputStream = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
                String line;
                while ((line = bufferedInputStream.readLine()) != null) {
                    builder.append(line);
                }
            }
            catch (IOException e)
            {
                e.fillInStackTrace();
                return builder.toString();
            }
            finally
            {
                if (connection != null) {
                    connection.disconnect();
                }
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
            }
            return builder.toString();
        }

        public static String getToken(String uri, String method, int timeout, Map<String, Object> headers, Object body) {
            HttpURLConnection connection = null;
            BufferedOutputStream dataOutputStream = null;
            BufferedReader bufferedInputStream = null;
            try
            {
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(timeout);
                if (headers != null) {
                    for (Map.Entry<String, Object> entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue().toString());
                    }
                }
                if (body != null) {
                    connection.setDoOutput(true);
                    String bodyJsonString = "";
                    if (body instanceof Map<?,?>) {
//                        bodyJsonString = paramBody((Map<String, Object>) body);
                        StringBuilder postData = new StringBuilder();
                        Map<String, Object> map = (Map<String, Object>) body;
                        for (Map.Entry<String, Object> param : map.entrySet()) {
                            if (postData.length() != 0) {
                                postData.append('&');
                            }
                            postData.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
                            postData.append('=');
                            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), StandardCharsets.UTF_8));
                        }
                        bodyJsonString = postData.toString();
                    }
                    dataOutputStream = new BufferedOutputStream(connection.getOutputStream());
                    dataOutputStream.write(bodyJsonString.getBytes());
                    dataOutputStream.flush();
                }
                connection.connect();
                Map<String, List<String>> headerFields = connection.getHeaderFields();
                Set<String> headerNames = headerFields.keySet();
                if (!headerNames.contains("Authorization")) {
                    return null;
                }
                return connection.getHeaderField("Authorization");
            }
            catch (IOException e)
            {
                e.fillInStackTrace();
                return null;
            }
            finally
            {
                if (connection != null) {
                    connection.disconnect();
                }
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
            }
        }

        public static String paramBody(Map<String, Object> params) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                stringBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            return stringBuilder.substring(1);
        }
    }

    public static void main(String[] args) {
        JsonObject projectInfo = getProjectInfo("JobsCraft");
        System.out.println(projectInfo.get("code").getAsInt());
    }
}
