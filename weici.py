#python版，功能不完善
import hashlib
import json
import random
import time
import urllib.parse
from typing import Dict, List, Optional

import requests
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad


class WeiCiStudy:
    """
    微词学习平台 API 交互类
    支持登录、获取任务、模拟作答并提交结果（包括错题上报与任务同步）
    """

    # 从逆向分析得到的固定盐值
    SALT = "w*#%7@$&c"
    # AES/ECB/PKCS5Padding 加密密钥（UTF-8 字符串，16 字节）
    AES_KEY = "ac14c13680bdf7a0"

    def __init__(self, username: str, password: str):
        """
        初始化会话并计算密码哈希

        :param username: 手机号（用户代码）
        :param password: 明文密码
        """
        self.session = requests.Session()
        self.username = username
        self.password_hash = self._calc_password_hash(password)

        self.base_params = {
            "app_version": "457",
            "bound_id": "795c275ac8704417a0bbca97c15bd67e1",  # 需替换为实际 bound_id
            "app_id": "8",
            "device": "0",
            "platform": "1",
            "is_wifi": "1",
        }
        self.session_cookie = None
        self.task_id = None
        self.class_id = None

    @staticmethod
    def _calc_password_hash(password: str) -> str:
        """
        使用逆向得到的盐值计算密码的 MD5 哈希

        :param password: 明文密码
        :return: 32 位十六进制哈希值
        """
        raw = f"{WeiCiStudy.SALT}{password}{WeiCiStudy.SALT}"
        return hashlib.md5(raw.encode("utf-8")).hexdigest()

    @staticmethod
    def _aes_ecb_pkcs5_encrypt(plaintext: str, key: str) -> str:
        """
        AES/ECB/PKCS5Padding 加密，输出十六进制字符串

        :param plaintext: 待加密的明文字符串
        :param key:       UTF-8 字符串密钥（16/24/32 字节）
        :return:          十六进制密文字符串
        """
        key_bytes = key.encode("utf-8")
        cipher = AES.new(key_bytes, AES.MODE_ECB)
        padded = pad(plaintext.encode("utf-8"), AES.block_size)
        return cipher.encrypt(padded).hex()

    def login(self) -> bool:
        """执行登录，获取 session cookie"""
        login_url = "https://api.weicistudy.com/account/login"
        params = {
            "access_token": "",
            "password": self.password_hash,
            "user_code": self.username,
            "login_type": "1",
            "auth_code": "",
            "session": "",
            **self.base_params,
        }
        try:
            resp = self.session.get(login_url, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"❌ 登录请求异常: {e}")
            return False

        if data.get("result_code") == 200:
            self.session_cookie = data.get("session")
            print(f"✅ 登录成功，session: {self.session_cookie}")
            return True
        else:
            print(f"❌ 登录失败: {data}")
            return False

    def get_class_info(self) -> bool:
        """获取班课信息，保存 class_id"""
        url = "https://api.weicistudy.com/gaozhong/weici/group/student/has/class"
        params = {
            **self.base_params,
            "user_code": self.username,
            "session": self.session_cookie,
        }
        try:
            resp = self.session.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"❌ 获取班课信息异常: {e}")
            return False

        if data.get("result_code") == 200:
            self.class_id = data.get("class_id")
            print(f"📋 班课ID: {self.class_id}")
            return True
        else:
            print(f"❌ 获取班课信息失败: {data}")
            return False

    def get_task_list(self) -> List[Dict]:
        """获取任务列表（可能因缺少具体 task_id 而返回空）"""
        url = "https://api.weicistudy.com/gaozhong/weici/group/student/v31/tasklist"
        params = {
            "task_ids": "",
            "user_code": self.username,
            "session": self.session_cookie,
            **self.base_params,
        }
        try:
            resp = self.session.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"❌ 获取任务列表异常: {e}")
            return []

        if data.get("result_code") == 200:
            tasks = data.get("tasks", [])
            print(f"📋 获取到 {len(tasks)} 个任务")
            for task in tasks:
                print(
                    f"  任务ID: {task.get('id')}, "
                    f"类型: {task.get('task_type')}, "
                    f"名称: {task.get('task_name', '')}"
                )
            return tasks
        else:
            print(f"❌ 获取任务失败: {data}")
            return []

    def get_task_catalog(self, task_id: int, task_type: int = 8) -> List[Dict]:
        """获取任务目录（各天学习内容）"""
        url = "https://api.weicistudy.com/gaozhong/weici/group/task/hearing/catalog"
        params = {
            "task_id": task_id,
            "task_type": task_type,
            "user_code": self.username,
            "session": self.session_cookie,
            **self.base_params,
        }
        try:
            resp = self.session.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"❌ 获取目录异常: {e}")
            return []

        if data.get("result_code") == 200:
            catalogs = data.get("task_catalog", [])
            print(f"  📂 获取到 {len(catalogs)} 个目录")
            return catalogs
        else:
            print(f"  ❌ 获取目录失败: {data}")
            return []

    def get_task_test(self, day_id: int, task_type: int = 8, source: int = 0) -> Dict:
        """获取某一天的具体题目数据"""
        url = "https://api.weicistudy.com/gaozhong/weici/group/task/hearing/test"
        params = {
            "source": source,
            "task_type": task_type,
            "day_id": day_id,
            "user_code": self.username,
            "session": self.session_cookie,
            **self.base_params,
        }
        try:
            resp = self.session.get(url, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"  ❌ 获取题目异常: {e}")
            return {}

        if data.get("result_code") == 200:
            task_test = data.get("task_test", [])
            print(f"  📖 获取到 {len(task_test)} 道大题")
            return data
        else:
            print(f"  ❌ 获取题目失败: {data}")
            return {}

    @staticmethod
    def parse_sub_items(task_test: List[Dict]) -> Dict[int, Dict]:
        """
        解析大题中的子题，提取正确答案和选项列表

        :param task_test: 从 API 返回的 task_test 列表
        :return: { content_id: {"correct": str, "options": List[str]} }
        """
        result = {}
        for task in task_test:
            sub_list = json.loads(task.get("sub", "[]"))
            for sub in sub_list:
                cid = sub.get("id")
                if not cid:
                    continue
                correct = sub.get("answer")
                if not correct:
                    continue
                options = []
                for opt_key in ("answer_a", "answer_b", "answer_c", "answer_d"):
                    val = sub.get(opt_key, "")
                    if val and val not in options:
                        options.append(val)
                if correct not in options:
                    options.append(correct)
                result[cid] = {"correct": correct, "options": options}
        return result

    def build_user_answers(
        self, question_info: Dict[int, Dict], wrong_count: int = 2
    ) -> Dict[int, str]:
        """
        构建用户作答：随机将 wrong_count 道题答错，其余答对

        :param question_info: parse_sub_items 返回的字典
        :param wrong_count:   故意答错的数量
        :return: { content_id: chosen_answer }
        """
        all_ids = list(question_info.keys())
        if len(all_ids) <= wrong_count:
            wrong_ids = []
        else:
            wrong_ids = random.sample(all_ids, wrong_count)

        user_answers = {}
        for cid, info in question_info.items():
            correct = info["correct"]
            if cid in wrong_ids:
                opts = [o for o in info["options"] if o != correct]
                if opts:
                    user_answers[cid] = random.choice(opts)
                else:
                    user_answers[cid] = correct
            else:
                user_answers[cid] = correct
        return user_answers

    def build_submit_item(
        self, main_question: Dict, user_answers: Dict[int, str], correct_answers: Dict[int, str]
    ) -> Dict:
        """
        构建提交到 /sync/v2/word/error 的单条错题数据

        :param main_question:  单道大题的数据
        :param user_answers:   用户作答映射
        :param correct_answers: 正确答案映射
        :return: 提交所需的字典
        """
        sub_list = json.loads(main_question.get("sub", "[]"))
        answer_list = []
        for sub in sub_list:
            cid = sub.get("id")
            if cid and cid in user_answers:
                chosen = user_answers[cid]
                right = 1 if correct_answers.get(cid) == chosen else 0
                answer_list.append(
                    {"content_id": cid, "right": right, "answer": chosen}
                )
        return {
            "test_id": main_question.get("id"),
            "word_id": 0,
            "answer": json.dumps(answer_list, ensure_ascii=False),
            "flag": 1,
            "time": int(time.time()),
            "extra": "",
            "from": 12,
            "json": json.dumps(main_question, ensure_ascii=False),
            "type": 2,
        }

    def submit_one_answer(self, submit_item: Dict) -> bool:
        """提交单道大题的作答结果到 /sync/v2/word/error"""
        url = "https://api.weicistudy.com/gaozhong/weici/sync/v2/word/error"
        form_data = {
            "error": json.dumps([submit_item], ensure_ascii=False),
            "user_code": self.username,
            "session": self.session_cookie,
            **self.base_params,
        }
        try:
            resp = self.session.post(url, data=form_data)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"  ❌ 大题 {submit_item['test_id']} 提交异常: {e}")
            return False

        if data.get("result_code") == 200:
            print(f"  ✅ 大题 {submit_item['test_id']} 提交成功")
            return True
        else:
            print(f"  ❌ 大题 {submit_item['test_id']} 提交失败: {data}")
            return False

    def submit_task_sync(
        self,
        task_id: int,
        day_id: int,
        all_user_answers: Dict[int, str],
        correct_answers: Dict[int, str],
        duration: int = 359999,
    ) -> bool:
        """
        提交任务同步请求（含 AES 加密的 param）

        加密逻辑：
          1. 构建 form body（不含 param），参数按固定顺序排列
          2. urllib.parse.urlencode() 编码整个 dict
          3. AES/ECB/PKCS5Padding 加密该编码后的字符串
          4. 加密结果（hex）作为 param 的值
          5. 发送完整表单（含 param）
        """
        # 构建每个子题的作答记录
        data_list = []
        for cid, chosen in all_user_answers.items():
            correct = correct_answers.get(cid, "")
            result = 0 if chosen == correct else 1  # 0=正确, 1=错误
            data_list.append(
                {
                    "test_id": cid,
                    "answer": chosen,
                    "duration": 359999,
                    "result": result,
                    "repeat_points": 0,
                    "revise_num": 0,
                    "sound_type": 3,
                }
            )

        # 构建 json_data（JSON 字符串）
        sync_record = {
            "user_code": self.username,
            "task_id": task_id,
            "push_id": 0,
            "day": day_id,
            "finish_word": 1,
            "finish_time": int(time.time()),
            "duration": duration,
            "data": json.dumps(data_list, ensure_ascii=False),
        }
        json_data_str = json.dumps([sync_record], ensure_ascii=False)

        # 表单参数（严格按顺序排列，来自抓包分析）
        form_items = [
            ("json_data", json_data_str),
            ("is_wifi", "1"),
            ("app_version", "457"),
            ("user_code", self.username),
            ("bound_id", self.base_params["bound_id"]),
            ("session", self.session_cookie),
            ("app_id", "8"),
            ("device", "0"),
            ("platform", "1"),
        ]

        # URL 编码整个 form body（不含 param）→ 待加密的明文
        body_to_encrypt = urllib.parse.urlencode(form_items)

        # AES 加密 → param
        param = self._aes_ecb_pkcs5_encrypt(body_to_encrypt, self.AES_KEY)

        # 发送 POST 请求（含 param）
        url = "https://api.weicistudy.com/gaozhong/weici/group/v30/student/task/sync"
        form_data_with_param = [("param", param)] + form_items
        try:
            resp = self.session.post(url, data=form_data_with_param)
            resp.raise_for_status()
            data = resp.json()
        except Exception as e:
            print(f"  ❌ 任务同步异常: {e}")
            return False

        if data.get("result_code") == 200:
            print(f"  ✅ 任务 {task_id} 同步成功（day_id={day_id}）")
            return True
        else:
            print(f"  ❌ 任务 {task_id} 同步失败: {data}")
            return False

    def process_task(self, task_id: int, task_type: int = 8) -> bool:
        """
        处理单个任务：
          1. 获取目录
          2. 逐天获取题目
          3. 模拟作答（随机错题）
          4. 提交错题记录
          5. 同步任务进度
        """
        print(f"\n🚀 开始处理任务 {task_id}")
        catalogs = self.get_task_catalog(task_id, task_type)
        if not catalogs:
            print(f"  ⚠️ 任务 {task_id} 没有目录，跳过")
            return False

        all_question_info = {}
        for catalog in catalogs:
            day_id = catalog.get("id")
            if not day_id:
                continue
            source = catalog.get("source", 0)
            print(
                f"  📂 处理目录 day_id={day_id} ({catalog.get('title', '')}) source={source}"
            )
            test_data = self.get_task_test(day_id, task_type, source)
            if not test_data:
                continue
            task_test = test_data.get("task_test", [])
            if not task_test:
                continue

            # 解析所有子题信息
            question_info = self.parse_sub_items(task_test)
            all_question_info.update(question_info)

            # 模拟作答（每个 day 独立）
            user_answers = self.build_user_answers(question_info, wrong_count=2)
            correct_answers = {
                cid: info["correct"] for cid, info in question_info.items()
            }

            # 统计正确/错误
            right_count = sum(
                1 for cid in user_answers if user_answers[cid] == correct_answers[cid]
            )
            wrong_count = len(user_answers) - right_count
            print(
                f"  📊 day_id={day_id} 共 {len(question_info)} 题，"
                f"正确 {right_count} 题，错误 {wrong_count} 题"
            )

            # 提交错题记录（每个大题）
            for t in task_test:
                submit_item = self.build_submit_item(t, user_answers, correct_answers)
                self.submit_one_answer(submit_item)
                time.sleep(0.3)

            # 同步任务进度
            self.submit_task_sync(
                task_id=task_id,
                day_id=day_id,
                all_user_answers=user_answers,
                correct_answers=correct_answers,
                duration=359999,
            )
            time.sleep(0.5)

        print(f"  ✅ 任务 {task_id} 所有操作完成")
        return True

    def run(self, custom_task_id: Optional[int] = None):
        """主入口"""
        if not self.login():
            return
        if not self.get_class_info():
            return

        if custom_task_id:
            self.process_task(custom_task_id)
        else:
            tasks = self.get_task_list()
            if tasks:
                for task in tasks:
                    tid = task.get("id")
                    if tid:
                        self.process_task(tid)
                        time.sleep(1)
            else:
                print("⚠️ 没有从 API 获取到任务列表")
                print("💡 请通过抓包获取 task_id 后，重新运行并输入 task_id")


def main():
    """交互入口"""
    phone = input("请输入手机号: ").strip()
    password = input("请输入密码: ").strip()
    task_id_str = input("请输入任务ID: ").strip()

    bot = WeiCiStudy(phone, password)

    if task_id_str:
        try:
            task_id = int(task_id_str)
            bot.run(custom_task_id=task_id)
        except ValueError:
            print("❌ 任务ID必须是数字")
    else:
        bot.run()


if __name__ == "__main__":
    main()