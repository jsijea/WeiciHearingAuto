#一个完成维词万词王的小工具
import json
import time
import urllib.parse
from weici import WeiCiStudy  # 确保 weici.py 在同一目录下

def main():
    print("=== 维词万词王工具 ===")
    phone = input("请输入手机号: ").strip()
    password = input("请输入密码: ").strip()
    count_str = input("请输入 count (默认为 2): ").strip()
    count = int(count_str) if count_str else 2

    # 初始化并登录（内部会打印一次登录成功信息）
    bot = WeiCiStudy(phone, password)
    if not bot.login():
        print("❌ 登录失败，程序退出")
        return
    # 此处不再额外打印，避免重复

    # 固定 class_id 为 7751
    class_id = 7751

    # 构造 json_data
    json_data = [{
        "user_code": phone,
        "class_id": class_id,
        "app_id": 8,
        "count": count,
        "finish_time": int(time.time())
    }]
    json_data_str = json.dumps(json_data, ensure_ascii=False)

    # 表单顺序固定（不含 param）
    form_items = [
        ("json_data", json_data_str),
        ("is_wifi", "1"),
        ("app_version", "457"),
        ("user_code", phone),
        ("bound_id", bot.base_params["bound_id"]),
        ("session", bot.session_cookie),
        ("app_id", "8"),
        ("device", "0"),
        ("platform", "1"),
    ]

    # URL 编码后 AES 加密生成 param
    body_to_encrypt = urllib.parse.urlencode(form_items)
    param = WeiCiStudy._aes_ecb_pkcs5_encrypt(body_to_encrypt, WeiCiStudy.AES_KEY)
    print(f"🔐 生成的 param: {param[:50]}...")

    # 发送 POST 请求
    url = "https://api.weicistudy.com/gaozhong/weici/group/v30/arena/king/upload"
    post_data = [("param", param)] + form_items
    try:
        resp = bot.session.post(url, data=post_data)
        resp.raise_for_status()
        result = resp.json()
        print("✅ 上传成功，服务器响应：")
        print(json.dumps(result, indent=2, ensure_ascii=False))
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print("响应内容:", e.response.text)

if __name__ == "__main__":
    main()