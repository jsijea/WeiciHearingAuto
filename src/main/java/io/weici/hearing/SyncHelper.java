package io.weici.hearing;

 import org.json.JSONArray;
 import org.json.JSONObject;

import java.io.BufferedReader;
	import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SyncHelper {
    private final WeiCiClient client;

    public SyncHelper(WeiCiClient client) {
        this.client = client;
    }

    /**
     * 提交任务同步（含AES加密的param）
     * @param taskId 任务ID
     * @param dayId  天次ID
     * @param userAnswers 用户作答映射 contentId -> 选项文本
     * @param correctAnswers 正确答案映射 contentId -> 正确选项
     * @param duration 耗时（秒）
     * @return 是否成功
     */
    public boolean submitSync(int taskId, int dayId,
                              Map<Integer, String> userAnswers,
                              Map<Integer, String> correctAnswers,
                              int duration) throws Exception {
        // 1. 构建 data_list（每个子题的作答记录）
        JSONArray dataArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : userAnswers.entrySet()) {
            int cid = entry.getKey();
            String chosen = entry.getValue();
            String correct = correctAnswers.get(cid);
            int result = (chosen.equals(correct)) ? 0 : 1; // 0正确 1错误
            JSONObject item = new JSONObject();
            item.put("test_id", cid);
            item.put("answer", chosen);
            item.put("duration", 0);
            item.put("result", result);
            item.put("repeat_points", 0);
            item.put("revise_num", 0);
            item.put("sound_type", 3);
            dataArray.put(item);
        }

        // 2. 构建 sync_record JSON
        JSONObject record = new JSONObject();
        record.put("user_code", client.getUsername());
        record.put("task_id", taskId);
        record.put("push_id", 0);
        record.put("day", dayId);
        record.put("finish_word", 1);
        record.put("finish_time", System.currentTimeMillis() / 1000);
        record.put("duration", duration);
        record.put("data", dataArray.toString());

        JSONArray records = new JSONArray();
        records.put(record);
        String jsonDataStr = records.toString();

        // 3. 构建表单参数（顺序必须固定，与Python一致）
        Map<String, String> base = client.getBaseParams();
        StringBuilder formBody = new StringBuilder();
        // 按顺序拼接：json_data, is_wifi, app_version, user_code, bound_id, session, app_id, device, platform
        appendParam(formBody, "json_data", jsonDataStr);
        appendParam(formBody, "is_wifi", base.get("is_wifi"));
        appendParam(formBody, "app_version", base.get("app_version"));
        appendParam(formBody, "user_code", client.getUsername());
        appendParam(formBody, "bound_id", base.get("bound_id"));
        appendParam(formBody, "session", client.getSession());
        appendParam(formBody, "app_id", base.get("app_id"));
        appendParam(formBody, "device", base.get("device"));
        appendParam(formBody, "platform", base.get("platform"));

        String bodyToEncrypt = formBody.toString();
        // 4. AES加密得到param
        String param = CryptoUtils.aesEcbEncrypt(bodyToEncrypt);

        // 5. 发送POST（包含param和原所有字段）
        String url = "https://api.weicistudy.com/gaozhong/weici/group/v30/student/task/sync";
        Map<String, String> finalParams = new HashMap<>();
        finalParams.put("param", param);
        // 把刚才所有字段再放入（顺序无所谓，但必须包含）
        finalParams.put("json_data", jsonDataStr);
        finalParams.put("is_wifi", base.get("is_wifi"));
        finalParams.put("app_version", base.get("app_version"));
        finalParams.put("user_code", client.getUsername());
        finalParams.put("bound_id", base.get("bound_id"));
        finalParams.put("session", client.getSession());
        finalParams.put("app_id", base.get("app_id"));
        finalParams.put("device", base.get("device"));
        finalParams.put("platform", base.get("platform"));

        return doPost(url, finalParams);
    }

    private boolean doPost(String urlStr, Map<String, String> params) throws Exception {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (postData.length() > 0) postData.append("&");
            postData.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        byte[] postBytes = postData.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            out.write(postBytes);
        }

        int code = conn.getResponseCode();
        if (code != 200) throw new RuntimeException("HTTP错误: " + code);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder resp = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) resp.append(line);
        reader.close();
        conn.disconnect();

        JSONObject result = new JSONObject(resp.toString());
        return result.optInt("result_code") == 200;
    }

    private void appendParam(StringBuilder sb, String key, String value) throws Exception {
        if (sb.length() > 0) sb.append("&");
        sb.append(URLEncoder.encode(key, "UTF-8"))
          .append("=")
          .append(URLEncoder.encode(value, "UTF-8"));
    }
}
