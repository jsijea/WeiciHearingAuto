package io.weici.hearing;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
	import java.util.regex.Matcher;
	import java.util.regex.Pattern;

	public class MainActivity extends AppCompatActivity {

    private EditText etPhone, etPassword, etTaskId, etDuration, etWrongCount, etWrongIds;
    private CheckBox cbSelfSelect, cbShowPhone, cbShowPassword, cbOutputLog;
    private Button btnStart, btnCopy;
    private TextView tvLog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private StringBuilder htmlLog = new StringBuilder();
    private int logCounter = 0;               // 计数器
    private static final int LOG_FLUSH_THRESHOLD = 10; // 每10条刷新一次UI

		// 颜色数组：红、蓝、紫、粉
		private static final String[] COLORS = {"#FF0000", "#0000FF", "#800080", "#FF69B4"};

		@Override
		protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etTaskId = findViewById(R.id.et_task_id);
        etDuration = findViewById(R.id.et_duration);
        etWrongCount = findViewById(R.id.et_wrong_count);
        etWrongIds = findViewById(R.id.et_wrong_ids);
        cbSelfSelect = findViewById(R.id.cb_self_select);
        cbShowPhone = findViewById(R.id.cb_show_phone);
        cbShowPassword = findViewById(R.id.cb_show_password);
        cbOutputLog = findViewById(R.id.cb_output_log);
        btnStart = findViewById(R.id.btn_start);
				btnCopy = findViewById(R.id.btn_copy);
				tvLog = findViewById(R.id.tv_log);

					etDuration.setInputType(InputType.TYPE_CLASS_TEXT);

						btnCopy.setOnClickListener(new View.OnClickListener() {
						@Override
					public void onClick(View v) {
                String logText = tvLog.getText().toString();
			if (!logText.isEmpty()) {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("log", logText);
		clipboard.setPrimaryClip(clip);
		}
		}
        });

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        etPhone.setText(prefs.getString("phone", ""));
        etPassword.setText(prefs.getString("password", ""));
        etTaskId.setText(prefs.getString("task_id", ""));

        int duration = prefs.getInt("duration", -1);
        if (duration > 0) etDuration.setText(String.valueOf(duration));

        int wrongCnt = prefs.getInt("wrong_count", -1);
        if (wrongCnt >= 0) etWrongCount.setText(String.valueOf(wrongCnt));

        etWrongIds.setText(prefs.getString("wrong_ids", ""));
        cbSelfSelect.setChecked(prefs.getBoolean("self_select", false));
        cbOutputLog.setChecked(prefs.getBoolean("output_log", false));

        boolean showPhone = prefs.getBoolean("show_phone", false);
        cbShowPhone.setChecked(showPhone);
        applyPhoneVisibility(showPhone);

				boolean showPassword = prefs.getBoolean("show_password", false);
					cbShowPassword.setChecked(showPassword);
					applyPasswordVisibility(showPassword);

			cbShowPhone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyPhoneVisibility(isChecked);
                getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("show_phone", isChecked).apply();
					}
					});

			cbShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyPasswordVisibility(isChecked);
                getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("show_password", isChecked).apply();
					}
					});

					btnStart.setOnClickListener(new View.OnClickListener() {
					@Override
							public void onClick(View v) {
							btnStart.setEnabled(false);
								htmlLog.setLength(0);
							logCounter = 0;
						tvLog.setText("");
                saveAllSettings();
			new Thread(new Runnable() {
		@Override
		public void run() {
		runTask();
	// 任务结束后强制刷新一次
	mainHandler.post(new Runnable() {
	@Override
	public void run() {
		tvLog.setText(Html.fromHtml(htmlLog.toString()));
			}
		});
			}
		}).start();
		}
	});

	htmlLog.append("日志将显示在此处...<br/>");
        tvLog.setText(Html.fromHtml(htmlLog.toString()));
			}

			// ==================== 界面控制 ====================
		private void applyPhoneVisibility(boolean show) {
        if (show) {
	etPhone.setInputType(InputType.TYPE_CLASS_TEXT);
	} else {
	etPhone.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        etPhone.setSelection(etPhone.getText().length());
		}

		private void applyPasswordVisibility(boolean show) {
        if (show) {
		etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			} else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        etPassword.setSelection(etPassword.getText().length());
		}

		private void saveAllSettings() {
        SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putString("phone", etPhone.getText().toString().trim());
        editor.putString("password", etPassword.getText().toString().trim());
        editor.putString("task_id", etTaskId.getText().toString().trim());

	String durStr = etDuration.getText().toString().trim();
	if (!durStr.isEmpty()) {
	int seconds = parseDuration(durStr);
		editor.putInt("duration", seconds);
        }
        String cntStr = etWrongCount.getText().toString().trim();
        if (!cntStr.isEmpty()) {
            try { editor.putInt("wrong_count", Integer.parseInt(cntStr)); } catch (NumberFormatException ignored) {}
				}
				editor.putString("wrong_ids", etWrongIds.getText().toString().trim());
			editor.putBoolean("self_select", cbSelfSelect.isChecked());
        editor.putBoolean("output_log", cbOutputLog.isChecked());
			editor.apply();
			}

					// ==================== 工具方法 ====================
					private int parseDuration(String input) {
					if (input == null || input.trim().isEmpty()) return 1;
				String cleaned = input.trim().replace("，", ",").replaceAll("\\s+", "");
					if (cleaned.isEmpty()) return 1;
					if (!cleaned.contains(",")) {
					try {
					int sec = Integer.parseInt(cleaned);
                return Math.max(sec, 1);
            } catch (NumberFormatException e) { return 1; }
        } else {
		String[] parts = cleaned.split(",");
	try {
	if (parts.length == 2) {
	int min = Integer.parseInt(parts[0]);
		int sec = Integer.parseInt(parts[1]);
		return Math.max(min * 60 + sec, 1);
		} else if (parts.length == 3) {
	int hour = Integer.parseInt(parts[0]);
	int min = Integer.parseInt(parts[1]);
	int sec = Integer.parseInt(parts[2]);
		return Math.max(hour * 3600 + min * 60 + sec, 1);
		}
		} catch (NumberFormatException ignored) {}
        }
	return 1;
    }

		private int circleToNumber(char c) {
        if (c >= '\u2460' && c <= '\u2468') return c - '\u2460' + 1;
        if (c >= '\u2469' && c <= '\u2473') return c - '\u2469' + 10;
        return -1;
    }

    private int extractQuestionNumberFromSubject(String subject) {
	Pattern p = Pattern.compile("^(\\d+)\\s*\\.");
        Matcher m = p.matcher(subject);
        if (m.find()) return Integer.parseInt(m.group(1));
        return -1;
		}

			private String extractOptionLetter(String answer) {
			if (answer == null || answer.isEmpty()) return "?";
			char first = answer.charAt(0);
			if (first >= 'A' && first <= 'D') return String.valueOf(first);
			return String.valueOf(first);
			}

			// ==================== 提取加粗/着色纯文本 ====================
				private List<String> extractStyledTexts(String html) {
					List<String> texts = new ArrayList<>();
				Pattern startPattern = Pattern.compile("<(strong|span\\s+style=[\"']color:#[0-9a-fA-F]{6}[\"'])>", Pattern.CASE_INSENSITIVE);
					Matcher matcher = startPattern.matcher(html);

				while (matcher.find()) {
					String tagName = matcher.group(1).split("\\s")[0];
					int startIdx = matcher.end();
				int depth = 1;
            int endIdx = startIdx;
            String searchFrom = html.substring(startIdx);
				Pattern tagPattern = Pattern.compile("</?" + tagName + ">", Pattern.CASE_INSENSITIVE);
				Matcher tagMatcher = tagPattern.matcher(searchFrom);
				while (tagMatcher.find() && depth > 0) {
                if (tagMatcher.group().startsWith("</")) {
			depth--;
		} else {
		depth++;
	}
	if (depth == 0) {
	endIdx = startIdx + tagMatcher.start();
		break;
		}
		}
		if (depth == 0) {
		String innerHtml = html.substring(startIdx, endIdx);
			String text = innerHtml.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
			text = text.replaceAll("\\s+", " ");
                texts.add(text);
				}
				}
					return texts;
						}

					private Map<Integer, List<String>> parseHighlights(String html) {
				Map<Integer, List<String>> result = new HashMap<>();
			if (html == null || html.isEmpty()) return result;

        List<String> styledTexts = extractStyledTexts(html);
	for (String text : styledTexts) {
	List<String> sentences = splitByCircleNumbers(text);
	for (String sentence : sentences) {
		if (sentence.isEmpty()) continue;
		int qNum = circleToNumber(sentence.charAt(0));
		if (qNum != -1) {
		if (!result.containsKey(qNum)) {
			result.put(qNum, new ArrayList<String>());
		}
		result.get(qNum).add(sentence);
	}
	}
	}
        return result;
		}

    private List<String> splitByCircleNumbers(String text) {
	List<String> parts = new ArrayList<>();
	Pattern p = Pattern.compile("([\\u2460-\\u2473])([^\\u2460-\\u2473]*)");
        Matcher m = p.matcher(text);
        while (m.find()) {
		parts.add(m.group(1) + m.group(2));
	}
	return parts;
    }

		private String highlightSpeakers(String text) {
        if (text == null) return "";
        return text.replaceAll("\\b(M:)", "<font color=\"#ff9800\">$1</font>")
		.replaceAll("\\b(W:)", "<font color=\"#ff9800\">$1</font>");
		}

			private String htmlToPlainText(String html) {
			if (html == null || html.isEmpty()) return "";
        String plain = html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
        return plain.replaceAll("\\s+", " ");
		}

			// ==================== 核心任务 ====================
				private void runTask() {
			final String phone = etPhone.getText().toString().trim();
				final String password = etPassword.getText().toString().trim();
				final String taskIdStr = etTaskId.getText().toString().trim();

			if (phone.isEmpty() || password.isEmpty()) {
		log("❌ 手机号和密码不能为空");
		enableButton();
		return;
        }

        int customTaskId = -1;
        if (!taskIdStr.isEmpty()) {
		try {
		customTaskId = Integer.parseInt(taskIdStr);
		} catch (NumberFormatException e) {
			log("❌ 任务ID必须是数字");
			enableButton();
                return;
				}
				}

					int duration = parseDuration(etDuration.getText().toString().trim());
						final boolean selfSelect = cbSelfSelect.isChecked();
						final boolean outputLog = cbOutputLog.isChecked();

				int wrongCount = 2;
			Set<Integer> customWrongIds = new HashSet<>();

			if (selfSelect) {
            String idsStr = etWrongIds.getText().toString().trim();
				if (!idsStr.isEmpty()) {
					idsStr = idsStr.replace("，", ",").replaceAll("\\s+", "");
					String[] parts = idsStr.split(",");
                for (String p : parts) {
                    if (p.isEmpty()) continue;
				try {
			int id = Integer.parseInt(p);
		if (id > 0) customWrongIds.add(id);
		} catch (NumberFormatException ignored) {}
		}
            }
			} else {
            String countStr = etWrongCount.getText().toString().trim();
				if (!countStr.isEmpty()) {
                try {
				wrongCount = Integer.parseInt(countStr);
			if (wrongCount < 0) wrongCount = 0;
			} catch (NumberFormatException e) {
			wrongCount = 2;
			}
            }
			}

				try {
				WeiCiClient client = new WeiCiClient(phone, password);
            log("⏳ 正在登录...");
            if (!client.login()) {
			log("❌ 登录失败，请检查账号密码");
			enableButton();
			return;
            }
				log("✅ 登录成功，session: " + client.getSession());

				log("⏳ 获取班课信息...");
            JSONObject classResp = client.getClassInfo();
            if (classResp.optInt("result_code") != 200) {
			log("❌ 获取班课失败: " + classResp);
                enableButton();
                return;
				}
            int classId = classResp.optInt("class_id");
            log("📋 班课ID: " + classId);

            JSONObject taskResp = client.getTaskList();
				if (taskResp.optInt("result_code") != 200) {
			log("❌ 获取任务列表失败: " + taskResp);
                enableButton();
                return;
					}
					JSONArray tasks = taskResp.optJSONArray("tasks");
				if (tasks == null || tasks.length() == 0) {
					log("⚠️ 没有任务");
					enableButton();
						return;
						}

						List<Integer> taskIds = new ArrayList<>();
					if (customTaskId > 0) {
					taskIds.add(customTaskId);
					} else {
					int taskCount = tasks.length();
                if (taskCount == 1) {
			int singleId = tasks.getJSONObject(0).optInt("id");
			if (singleId > 0) taskIds.add(singleId);
			} else {
				log("⚠️ 检测到 " + taskCount + " 个任务，请选择其中一个：");
				for (int i = 0; i < taskCount; i++) {
				JSONObject task = tasks.getJSONObject(i);
			int tid = task.optInt("id");
			String name = task.optString("task_name", "未命名");
			log("  📌 任务 " + (i + 1) + ": ID=" + tid + "，名称=" + name);
			}
			log("💡 请将任务ID填入上方输入框后重新运行。");
				enableButton();
				return;
                }
					}

				if (taskIds.isEmpty()) {
                log("⚠️ 没有有效的任务ID，请检查输入或任务列表");
                enableButton();
					return;
					}

				SyncHelper syncHelper = new SyncHelper(client);

					for (int taskId : taskIds) {
					log("🚀 开始处理任务 " + taskId);
					JSONObject catalogResp = client.getTaskCatalog(taskId, 8);
					if (catalogResp.optInt("result_code") != 200) {
                    log("❌ 获取目录失败: " + catalogResp);
                    continue;
					}
						JSONArray catalogs = catalogResp.optJSONArray("task_catalog");
						if (catalogs == null || catalogs.length() == 0) {
                    log("⚠️ 任务 " + taskId + " 无目录，跳过");
                    continue;
					}

						for (int j = 0; j < catalogs.length(); j++) {
                    JSONObject catalog = catalogs.getJSONObject(j);
                    int dayId = catalog.optInt("id");
                    int source = catalog.optInt("source", 0);
                    log("📂 处理 day_id=" + dayId + " source=" + source);

                    JSONObject testResp = client.getTaskTest(dayId, 8, source);
                    if (testResp.optInt("result_code") != 200) {
					log("❌ 获取题目失败: " + testResp);
					continue;
                    }
                    JSONArray taskTest = testResp.optJSONArray("task_test");
                    if (taskTest == null || taskTest.length() == 0) {
					log("⚠️ day_id=" + dayId + " 无题目");
					continue;
						}

						// ----- 解析数据 -----
						Map<Integer, String> mainHtmlMap = new HashMap<>();
						Map<Integer, List<Integer>> mainSubIds = new HashMap<>();
						Map<Integer, Map<Integer, List<String>>> mainHighlights = new HashMap<>();

						Map<Integer, String> correctMap = new HashMap<>();
						Map<Integer, List<String>> optionsMap = new HashMap<>();
						Map<Integer, String> subjectMap = new HashMap<>();
						Map<Integer, Integer> subToMain = new HashMap<>();
							Map<Integer, Integer> subQNum = new HashMap<>();

							for (int k = 0; k < taskTest.length(); k++) {
							JSONObject mainQ = taskTest.getJSONObject(k);
							int mainId = mainQ.optInt("id");
							String html = mainQ.optString("original_text", "");
							mainHtmlMap.put(mainId, html);

							Map<Integer, List<String>> qmap = parseHighlights(html);
							mainHighlights.put(mainId, qmap);

							JSONArray subs = new JSONArray(mainQ.optString("sub"));
							List<Integer> subList = new ArrayList<>();
							for (int m = 0; m < subs.length(); m++) {
                            JSONObject sub = subs.getJSONObject(m);
                            int cid = sub.optInt("id");
								if (cid == 0) continue;
								String correct = sub.optString("answer");
                            if (correct.isEmpty()) continue;
                            String subject = sub.optString("subject");
                            correctMap.put(cid, correct);
						subjectMap.put(cid, subject);
						subToMain.put(cid, mainId);
					subList.add(cid);

					int qn = extractQuestionNumberFromSubject(subject);
						subQNum.put(cid, qn);

					List<String> opts = new ArrayList<>();
					for (String key : new String[]{"answer_a", "answer_b", "answer_c", "answer_d"}) {
					String val = sub.optString(key);
					if (!val.isEmpty() && !opts.contains(val)) opts.add(val);
					}
						if (!opts.contains(correct)) opts.add(correct);
						optionsMap.put(cid, opts);
                        }
                        mainSubIds.put(mainId, subList);
                    }

                    if (correctMap.isEmpty()) {
                        log("⚠️ day_id=" + dayId + " 没有可答题");
					continue;
                    }

                    // ----- 全局排序 -----
                    List<Integer> allIds = new ArrayList<>();
						for (int k = 0; k < taskTest.length(); k++) {
							JSONObject mainQ = taskTest.getJSONObject(k);
                        int mainId = mainQ.optInt("id");
					List<Integer> subIds = mainSubIds.get(mainId);
                        if (subIds != null) allIds.addAll(subIds);
						}
						final Map<Integer, Integer> subIndexMap = new HashMap<>();
						for (int i = 0; i < allIds.size(); i++) {
                        subIndexMap.put(allIds.get(i), i + 1);
                    }

                    // ----- 确定错题 -----
                    Set<Integer> wrongIds = new HashSet<>();
                    if (selfSelect) {
					for (int uid : customWrongIds) {
						if (uid >= 1 && uid <= allIds.size()) wrongIds.add(allIds.get(uid - 1));
                        }
							} else {
							int total = allIds.size();
							int real = Math.min(wrongCount, total);
								List<Integer> shuffled = new ArrayList<>(allIds);
							Collections.shuffle(shuffled);
							for (int i = 0; i < real; i++) wrongIds.add(shuffled.get(i));
								}

								// ----- 模拟作答（随机错选） -----
							Map<Integer, String> userAnswers = new HashMap<>();
							Random rand = new Random();
						for (int cid : allIds) {
							String correct = correctMap.get(cid);
					if (wrongIds.contains(cid)) {
					List<String> opts = optionsMap.get(cid);
					List<String> wrongOpts = new ArrayList<>();
						for (String o : opts) {
						if (!o.equals(correct)) wrongOpts.add(o);
						}
								String wrong;
								if (!wrongOpts.isEmpty()) {
									wrong = wrongOpts.get(rand.nextInt(wrongOpts.size()));
								} else {
							wrong = correct;
						}
						userAnswers.put(cid, wrong);
                        } else {
						userAnswers.put(cid, correct);
							}
							}

							// ========== 输出错题详情（按题号顺序，原文在大题顶部） ==========
							if (outputLog && !wrongIds.isEmpty()) {
                        // 1. 将所有错题按题号排序
                        List<Integer> sortedWrongIds = new ArrayList<>(wrongIds);
                        Collections.sort(sortedWrongIds, new Comparator<Integer>() {
						@Override
                            public int compare(Integer a, Integer b) {
							return subIndexMap.get(a).compareTo(subIndexMap.get(b));
                            }
							});

							// 2. 按大题分组（使用 LinkedHashMap 保持插入顺序）
							Map<Integer, List<Integer>> mainIdToWrongs = new LinkedHashMap<>();
							for (int cid : sortedWrongIds) {
                            int mainId = subToMain.get(cid);
                            if (!mainIdToWrongs.containsKey(mainId)) {
							mainIdToWrongs.put(mainId, new ArrayList<Integer>());
                            }
								mainIdToWrongs.get(mainId).add(cid);
							}

							// 3. 遍历每个大题
							for (Map.Entry<Integer, List<Integer>> entry : mainIdToWrongs.entrySet()) {
                            int mainId = entry.getKey();
                            List<Integer> wrongInMain = entry.getValue(); // 已按题号排序

									// 获取原文HTML并转为纯文本
									String html = mainHtmlMap.get(mainId);
										if (html == null) html = "";
										String plain = htmlToPlainText(html);
										if (plain.isEmpty()) plain = "（无听力原文）";

											// 建立错题到颜色的映射（按错题在该大题中出现的顺序）
												Map<Integer, String> idColorMap = new HashMap<>();
											for (int i = 0; i < wrongInMain.size() && i < COLORS.length; i++) {
										idColorMap.put(wrongInMain.get(i), COLORS[i]);
									}

                            // 标注原文：对每个错题，找到对应句子并标色（使用StringBuilder提高效率）
                            String annotated = plain;
                            Map<Integer, List<String>> hMap = mainHighlights.get(mainId);
                            if (hMap != null) {
							for (int sid : wrongInMain) {
							int qNum = subQNum.getOrDefault(sid, -1);
							if (qNum != -1 && hMap.containsKey(qNum)) {
							List<String> sentences = hMap.get(qNum);
							String color = idColorMap.get(sid);
								for (String s : sentences) {
								if (annotated.contains(s)) {
								annotated = annotated.replace(s, "<font color=\"" + color + "\">" + s + "</font>");
								}
								}
								}
                                }
								}
								// 高亮说话者
								annotated = highlightSpeakers(annotated);

									// ---- 先输出原文 ----
									log("【听力原文】<br/>" + annotated + "<br/>");

									// ---- 然后输出该大题下每个错题的详情 ----
								for (int sid : wrongInMain) {
									int idx = subIndexMap.get(sid);
									String subject = subjectMap.get(sid);
										List<String> opts = optionsMap.get(sid);
										String correct = correctMap.get(sid);
									String chosen = userAnswers.get(sid);
									boolean isWrong = !chosen.equals(correct);

                                StringBuilder sb = new StringBuilder();
                                sb.append("📝 第 ").append(idx).append(" 题 (ID: ").append(sid).append(")<br/>");
                                sb.append("   题干: ").append(subject).append("<br/>");
                                if (opts != null && opts.size() >= 4) {
							sb.append("   A. ").append(opts.get(0)).append("<br/>");
						sb.append("   B. ").append(opts.get(1)).append("<br/>");
					sb.append("   C. ").append(opts.get(2)).append("<br/>");
					sb.append("   D. ").append(opts.get(3)).append("<br/>");
					} else if (opts != null) {
					StringBuilder os = new StringBuilder("[");
					for (int i = 0; i < opts.size(); i++) {
					if (i > 0) os.append(", ");
					os.append(opts.get(i));
						}
						os.append("]");
						sb.append("   选项: ").append(os).append("<br/>");
						}
					sb.append("   正确答案: ").append(correct).append("<br/>");
					sb.append("   用户选择: ").append(chosen).append(isWrong ? "  （错误）" : "  （正确）").append("<br/>");
					log(sb.toString());
					}
					}
						}

						// ========== 汇总输出（始终显示） ==========
                    List<String> correctLetters = new ArrayList<>();
                    for (int i = 0; i < allIds.size(); i++) correctLetters.add(extractOptionLetter(correctMap.get(allIds.get(i))));
                    StringBuilder csb = new StringBuilder("✅ 正确答案汇总（所有题目）:<br/>");
						for (int s = 0; s < correctLetters.size(); s += 5) {
                        int e = Math.min(s + 5, correctLetters.size());
                        StringBuilder g = new StringBuilder();
                        for (int i = s; i < e; i++) g.append(correctLetters.get(i));
					csb.append(s + 1).append("-").append(e).append(" <font color=\"green\">").append(g).append("</font><br/>");
                    }
                    log(csb.toString());

                    List<String> submitLetters = new ArrayList<>();
						for (int i = 0; i < allIds.size(); i++) {
					int cid = allIds.get(i);
					if (wrongIds.contains(cid)) submitLetters.add(extractOptionLetter(userAnswers.get(cid)));
					else submitLetters.add(extractOptionLetter(correctMap.get(cid)));
                    }
                    StringBuilder ssb = new StringBuilder("📤 提交答案汇总:<br/>");
						for (int s = 0; s < submitLetters.size(); s += 5) {
                        int e = Math.min(s + 5, submitLetters.size());
                        StringBuilder g = new StringBuilder();
                        for (int i = s; i < e; i++) g.append(submitLetters.get(i));
							ssb.append(s + 1).append("-").append(e).append(" <font color=\"blue\">").append(g).append("</font><br/>");
							}
							log(ssb.toString());

							int right = 0, wrong = 0;
							for (int cid : allIds) {
							if (userAnswers.get(cid).equals(correctMap.get(cid))) right++; else wrong++;
							}
							log("📊 day_id=" + dayId + " 共 " + allIds.size() + " 题，正确 " + right + "，错误 " + wrong);

						// ========== 提交错题 ==========
						for (int k = 0; k < taskTest.length(); k++) {
                        JSONObject mainQ = taskTest.getJSONObject(k);
                        JSONArray subs = new JSONArray(mainQ.optString("sub"));
                        JSONArray ansArr = new JSONArray();
                        for (int m = 0; m < subs.length(); m++) {
						JSONObject sub = subs.getJSONObject(m);
						int cid = sub.optInt("id");
						if (cid == 0) continue;
						String chosen = userAnswers.get(cid);
						if (chosen == null) continue;
						JSONObject a = new JSONObject();
						a.put("content_id", cid);
						a.put("right", chosen.equals(correctMap.get(cid)) ? 1 : 0);
						a.put("answer", chosen);
						ansArr.put(a);
                        }
                        JSONObject item = new JSONObject();
                        item.put("test_id", mainQ.optInt("id"));
                        item.put("word_id", 0);
					item.put("answer", ansArr.toString());
					item.put("flag", 1);
					item.put("time", System.currentTimeMillis() / 1000);
					item.put("extra", "");
					item.put("from", 12);
                        item.put("json", mainQ.toString());
				item.put("type", 2);
			JSONArray errArr = new JSONArray();
			errArr.put(item);
		Map<String, String> form = new HashMap<>();
			form.put("error", errArr.toString());
			form.put("user_code", client.getUsername());
		form.put("session", client.getSession());
			form.putAll(client.getBaseParams());
		boolean ok = postError(form);
	log(ok ? "  ✅ 大题 " + mainQ.optInt("id") + " 提交成功" : "  ❌ 大题 " + mainQ.optInt("id") + " 提交失败");
	}

		// ========== 同步 ==========
			boolean syncOk = syncHelper.submitSync(taskId, dayId, userAnswers, correctMap, duration);
			log(syncOk ? "  ✅ 任务 " + taskId + " 同步成功（day_id=" + dayId + "，耗时 " + duration + "秒）"
			: "  ❌ 任务 " + taskId + " 同步失败");
			}
            }
            log("✅ 所有任务处理完成！");
			} catch (Exception e) {
            log("❌ 发生异常: " + e.getMessage());
            e.printStackTrace();
				} finally {
				enableButton();
			}
			}

			private boolean postError(Map<String, String> params) {
			try {
            java.net.URL url = new java.net.URL("https://api.weicistudy.com/gaozhong/weici/sync/v2/word/error");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
		if (sb.length() > 0) sb.append("&");
			sb.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8")).append("=").append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
		}
	byte[] data = sb.toString().getBytes("UTF-8");
	conn.setRequestProperty("Content-Length", String.valueOf(data.length));
	conn.getOutputStream().write(data);
		int code = conn.getResponseCode();
				if (code != 200) return false;
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
					StringBuilder resp = new StringBuilder();
					String line;
				while ((line = br.readLine()) != null) resp.append(line);
            br.close();
	conn.disconnect();
	JSONObject json = new JSONObject(resp.toString());
	return json.optInt("result_code") == 200;
        } catch (Exception e) {
				return false;
				}
					}

			// ========== 优化日志输出：批量刷新 ==========
    private void log(final String msg) {
mainHandler.post(new Runnable() {
            @Override
            public void run() {
                htmlLog.append(msg).append("<br/>");
                logCounter++;
                // 每达到阈值或消息包含"完成"时刷新
                if (logCounter >= LOG_FLUSH_THRESHOLD || msg.contains("完成") || msg.contains("错误")) {
                    tvLog.setText(Html.fromHtml(htmlLog.toString()));
                    logCounter = 0;
                }
            }
        });
    }

    private void enableButton() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                btnStart.setEnabled(true);
                // 确保最后刷新一次
                tvLog.setText(Html.fromHtml(htmlLog.toString()));
            }
        });
    }
}
