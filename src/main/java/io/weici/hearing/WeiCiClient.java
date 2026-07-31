package io.weici.hearing;

 import org.json.JSONArray;
 import org.json.JSONObject;

import java.io.BufferedReader;
	import java.io.InputStreamReader;
	import java.net.HttpURLConnection;
	import java.net.URL;
	import java.net.URLEncoder;
	import java.util.HashMap;
	import java.util.Map;

	public class WeiCiClient {
    private String session;
    private final String username;
    private final String passwordHash;
    private final Map<String, String> baseParams;

    public WeiCiClient(String username, String password) {
	this.username = username;
        this.passwordHash = CryptoUtils.calcPasswordHash(password);
        this.baseParams = new HashMap<>();
        baseParams.put("app_version", "457");
        baseParams.put("bound_id", "795c275ac8704417a0bbca97c15bd67e1"); // 替换为你的bound_id
        baseParams.put("app_id", "8");
        baseParams.put("device", "0");
			baseParams.put("platform", "1");
			baseParams.put("is_wifi", "1");
			}

			// -------- 内部辅助：发送GET请求 --------
			private JSONObject doGet(String urlStr, Map<String, String> params) throws Exception {
			StringBuilder sb = new StringBuilder(urlStr);
			if (params != null && !params.isEmpty()) {
            sb.append("?");
		for (Map.Entry<String, String> e : params.entrySet()) {
		sb.append(URLEncoder.encode(e.getKey(), "UTF-8"))
		.append("=")
		.append(URLEncoder.encode(e.getValue(), "UTF-8"))
			.append("&");
            }
				sb.deleteCharAt(sb.length() - 1);
				}

						HttpURLConnection conn = (HttpURLConnection) new URL(sb.toString()).openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);

			int code = conn.getResponseCode();
			if (code != 200) throw new RuntimeException("HTTP错误: " + code);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line;
			StringBuilder resp = new StringBuilder();
			while ((line = reader.readLine()) != null) resp.append(line);
			reader.close();
			conn.disconnect();

			return new JSONObject(resp.toString());
			}

			// -------- 1. 登录 --------
			public boolean login() throws Exception {
			Map<String, String> params = new HashMap<>(baseParams);
			params.put("user_code", username);
			params.put("password", passwordHash);
        params.put("login_type", "1");
        params.put("access_token", "");
        params.put("auth_code", "");
        params.put("session", "");

			JSONObject resp = doGet("https://api.weicistudy.com/account/login", params);
			if (resp.optInt("result_code") == 200) {
            this.session = resp.optString("session");
            return true;
			}
			return false;
			}

			// -------- 2. 获取班课信息 --------
				public JSONObject getClassInfo() throws Exception {
				Map<String, String> params = new HashMap<>(baseParams);
			params.put("user_code", username);
			params.put("session", session);
        return doGet("https://api.weicistudy.com/gaozhong/weici/group/student/has/class", params);
		}

		// -------- 3. 获取任务列表 --------
			public JSONObject getTaskList() throws Exception {
			Map<String, String> params = new HashMap<>(baseParams);
			params.put("user_code", username);
			params.put("session", session);
        params.put("task_ids", "");
        return doGet("https://api.weicistudy.com/gaozhong/weici/group/student/v31/tasklist", params);
		}

			// -------- 4. 获取任务目录 --------
			public JSONObject getTaskCatalog(int taskId, int taskType) throws Exception {
			Map<String, String> params = new HashMap<>(baseParams);
			params.put("user_code", username);
			params.put("session", session);
        params.put("task_id", String.valueOf(taskId));
        params.put("task_type", String.valueOf(taskType));
        return doGet("https://api.weicistudy.com/gaozhong/weici/group/task/hearing/catalog", params);
		}

			// -------- 5. 获取某天题目 --------
			public JSONObject getTaskTest(int dayId, int taskType, int source) throws Exception {
			Map<String, String> params = new HashMap<>(baseParams);
			params.put("user_code", username);
			params.put("session", session);
        params.put("day_id", String.valueOf(dayId));
        params.put("task_type", String.valueOf(taskType));
        params.put("source", String.valueOf(source));
        return doGet("https://api.weicistudy.com/gaozhong/weici/group/task/hearing/test", params);
			}

			public String getSession() { return session; }
			public Map<String, String> getBaseParams() { return baseParams; }
			public String getUsername() { return username; }
			}
