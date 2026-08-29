import argparse
import json
import mimetypes
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from http.cookiejar import CookieJar

#How to run:
#python scripts\e2e_mainline_tests.py --base-url <BASE_URL> --db-user <DB_USER> --db-pass <DB_PASS>

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class HttpResult:
    def __init__(self, status, headers, body):
        self.status = status
        self.headers = headers
        self.body = body

    @property
    def location(self):
        return self.headers.get("Location", "")


class Client:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.jar = CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(self.jar),
            NoRedirect,
        )

    def get(self, path):
        return self.request("GET", path)

    def post_form(self, path, data):
        raw = urllib.parse.urlencode(data).encode("utf-8")
        return self.request(
            "POST",
            path,
            raw,
            {"Content-Type": "application/x-www-form-urlencoded"},
        )

    def post_json(self, path, payload):
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        return self.request(
            "POST",
            path,
            raw,
            {"Content-Type": "application/json"},
        )

    def post_multipart(self, path, fields, files):
        boundary = "----TeachingPlatformE2E" + str(int(time.time() * 1000))
        body = bytearray()
        for name, value in fields.items():
            body.extend(("--" + boundary + "\r\n").encode("utf-8"))
            body.extend(('Content-Disposition: form-data; name="%s"\r\n\r\n' % name).encode("utf-8"))
            body.extend(str(value).encode("utf-8"))
            body.extend(b"\r\n")
        for name, file_path in files:
            filename = os.path.basename(file_path)
            content_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"
            body.extend(("--" + boundary + "\r\n").encode("utf-8"))
            body.extend(('Content-Disposition: form-data; name="%s"; filename="%s"\r\n' % (name, filename)).encode("utf-8"))
            body.extend(("Content-Type: %s\r\n\r\n" % content_type).encode("utf-8"))
            with open(file_path, "rb") as stream:
                body.extend(stream.read())
            body.extend(b"\r\n")
        body.extend(("--" + boundary + "--\r\n").encode("utf-8"))
        return self.request(
            "POST",
            path,
            bytes(body),
            {"Content-Type": "multipart/form-data; boundary=" + boundary},
        )

    def request(self, method, path, data=None, headers=None):
        req = urllib.request.Request(self.base_url + path, data=data, method=method, headers=headers or {})
        try:
            with self.opener.open(req, timeout=30) as resp:
                return HttpResult(resp.status, resp.headers, resp.read().decode("utf-8", errors="replace"))
        except urllib.error.HTTPError as exc:
            return HttpResult(exc.code, exc.headers, exc.read().decode("utf-8", errors="replace"))


class Db:
    def __init__(self, args):
        self.enabled = bool(args.db_user)
        self.mysql = args.mysql
        self.user = args.db_user
        self.password = args.db_pass or ""
        self.host = args.db_host
        self.port = str(args.db_port)
        self.name = args.db_name

    def query(self, sql):
        if not self.enabled:
            return []
        env = os.environ.copy()
        env["MYSQL_PWD"] = self.password
        cmd = [
            self.mysql,
            "-h", self.host,
            "-P", self.port,
            "-u", self.user,
            "--default-character-set=utf8mb4",
            "-N",
            "-B",
            self.name,
            "-e",
            sql,
        ]
        proc = subprocess.run(
            cmd,
            env=env,
            text=True,
            encoding="utf-8",
            errors="replace",
            capture_output=True,
        )
        if proc.returncode != 0:
            raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
        return [line.split("\t") for line in proc.stdout.splitlines()]

    def scalar(self, sql, default=None):
        rows = self.query(sql)
        if not rows or not rows[0]:
            return default
        return rows[0][0]

    def execute(self, sql):
        self.query(sql)


def sql_str(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def ok_redirect(result, target):
    if result.status not in (301, 302, 303):
        return False
    if "?" in target:
        path, query = target.split("?", 1)
        return path in result.location and query in result.location
    return target in result.location


def login(client, username, password="123456"):
    client.get("/login")
    return client.post_form("/login", {"username": username, "password": password})


def json_loads(text):
    try:
        return json.loads(text)
    except Exception:
        return {"raw": text[:500]}


def first_id(db, sql):
    return db.scalar(sql, "")


def row_dict(rows, columns):
    if not rows:
        return {}
    return {name: rows[0][idx] if idx < len(rows[0]) else "" for idx, name in enumerate(columns)}


def add_case(cases, name, number, functions, input_data, precondition, expected, actual, assertion, passed=None):
    if passed is None:
        passed = assertion.startswith("通过")
    cases.append(
        {
            "测试名称": name,
            "测试编号": number,
            "测试函数（代码语言）": functions,
            "输入数据（代码语言）": input_data,
            "前置条件（自然语言）": precondition,
            "预期输出（自然语言）": expected,
            "实际输出（代码语言）": actual,
            "断言结果": assertion,
            "通过": passed,
        }
    )


def http_summary(result):
    return {"status": result.status, "location": result.location}


def create_course(client, db, prefix, stamp, status="active", allow_join="true"):
    name = prefix + "课程" + stamp
    code = prefix.upper()[:4] + stamp[-8:]
    res = client.post_form(
        "/teacher/course/create",
        {
            "courseName": name,
            "courseCode": code,
            "credit": "3",
            "subjectCategory": "软件测试",
            "hours": "32",
            "allowJoin": allow_join,
            "status": status,
            "description": prefix + " E2E 测试课程",
        },
    )
    course_id = first_id(db, "SELECT id FROM course WHERE code=" + sql_str(code)) if db.enabled else ""
    return {
        "name": name,
        "code": code,
        "id": course_id,
        "response": http_summary(res),
    }


def create_class(client, db, course_id, class_name, max_count):
    res = client.post_form(
        "/teacher/course/class/create",
        {"courseId": course_id, "className": class_name, "maxCount": str(max_count)},
    )
    class_id = first_id(
        db,
        "SELECT id FROM course_class WHERE course_id=%s AND name=%s ORDER BY id DESC LIMIT 1"
        % (course_id, sql_str(class_name)),
    ) if db.enabled else ""
    return {"id": class_id, "response": http_summary(res)}


def create_task(client, db, course_id, title, task_type, extra=None):
    data = {
        "courseId": course_id,
        "title": title,
        "taskType": task_type,
        "content": title + " 内容",
        "endTime": "2037-12-31T23:59",
        "status": "published",
        "fullScore": "100",
        "timeLimitMs": "10000",
        "memoryLimitMb": "128",
    }
    if extra:
        data.update(extra)
    res = client.post_form("/teacher/task/create", data)
    task_id = first_id(db, "SELECT id FROM task WHERE title=" + sql_str(title)) if db.enabled else ""
    return {"id": task_id, "title": title, "response": http_summary(res)}


def upload_resource(client, db, course_id, title, file_path):
    res = client.post_multipart(
        "/teacher/resource/upload",
        {"courseId": course_id, "title": title, "chapter": "E2E章节"},
        [("file", file_path)],
    )
    resource_id = first_id(
        db,
        "SELECT id FROM resource WHERE course_id=%s AND title=%s ORDER BY id DESC LIMIT 1"
        % (course_id, sql_str(title)),
    ) if db.enabled else ""
    return {"id": resource_id, "response": http_summary(res)}


def register_user(client, db, username, role="student", password="123456"):
    res = client.post_form("/register", {"username": username, "password": password, "role": role})
    user_id = first_id(db, "SELECT id FROM `user` WHERE username=" + sql_str(username)) if db.enabled else ""
    return {"id": user_id, "response": http_summary(res)}


def user_id(db, username):
    return first_id(db, "SELECT id FROM `user` WHERE username=" + sql_str(username))


def prepare_round_course(db, teacher, stamp, prefix, pdf_path, video_path):
    course = create_course(teacher, db, prefix, stamp)
    course_id = course["id"]
    pdf = upload_resource(teacher, db, course_id, prefix + "PDF" + stamp, pdf_path)
    video = upload_resource(teacher, db, course_id, prefix + "视频" + stamp, video_path)
    homework = create_task(teacher, db, course_id, prefix + "普通作业" + stamp, "homework")
    exam = create_task(teacher, db, course_id, prefix + "考试" + stamp, "exam", {"examAnswer": "main"})
    programming = create_task(
        teacher,
        db,
        course_id,
        prefix + "编程题" + stamp,
        "programming",
        {
            "allowedLanguage": "python",
            "testCases": "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n1\n---CASE---\n10 20\n---OUTPUT---\n30\n---WEIGHT---\n1",
        },
    )
    return {
        "course": course,
        "resources": {"pdf": pdf, "video": video},
        "tasks": {"homework": homework, "exam": exam, "programming": programming},
    }


def student_full_flow(args, db, teacher, stamp, pdf_path):
    student = Client(args.base_url)
    username = "s1_" + stamp[-10:]
    reg = register_user(student, db, username, "student")
    login_res = login(student, username)
    fixture = prepare_round_course(db, teacher, stamp, "S1", pdf_path, args.video_path)
    course_id = fixture["course"]["id"]
    pdf_id = fixture["resources"]["pdf"]["id"]
    video_id = fixture["resources"]["video"]["id"]
    homework_id = fixture["tasks"]["homework"]["id"]
    exam_id = fixture["tasks"]["exam"]["id"]
    programming_id = fixture["tasks"]["programming"]["id"]

    browse = student.get("/student/course/selection?search=" + urllib.parse.quote(fixture["course"]["code"]))
    select = student.post_form("/student/course/select", {"courseId": course_id})
    my_courses = student.get("/student/course/my")
    enrollment = db.scalar(
        "SELECT COUNT(*) FROM course_enrollment WHERE student_id=%s AND course_id=%s" % (reg["id"], course_id),
        "0",
    ) if db.enabled else ""
    class_count = db.scalar(
        "SELECT COALESCE(MAX(current_count),0) FROM course_class WHERE course_id=%s" % course_id,
        "0",
    ) if db.enabled else ""

    download = student.get("/student/resource/download/" + str(pdf_id))
    progress = student.post_json("/student/resource/progress", {"resourceId": video_id, "currentTime": 30, "duration": 120})
    progress_row = row_dict(
        db.query("SELECT progress,last_position,duration FROM resource_progress WHERE student_id=%s AND resource_id=%s" % (reg["id"], video_id)),
        ["progress", "last_position", "duration"],
    ) if db.enabled else {}

    title = "学生主线讨论" + stamp
    post = student.post_form(
        "/discussion/post",
        {"courseId": course_id, "title": title, "content": "学生主线发帖", "postType": "question", "targetRole": "teacher", "anonymous": "false"},
    )
    post_id = first_id(db, "SELECT id FROM discussion_post WHERE title=" + sql_str(title)) if db.enabled else ""

    homework_submit = student.post_multipart(
        "/student/task/submit",
        {"taskId": homework_id, "content": "学生主线普通作业提交"},
        [("file", pdf_path)],
    )
    homework_submission = first_id(
        db,
        "SELECT id FROM submission WHERE task_id=%s AND student_id=%s" % (homework_id, reg["id"]),
    ) if db.enabled else ""

    exam_begin = student.post_form("/student/exam/begin", {"taskId": exam_id})
    exam_save = student.post_form("/student/exam/save", {"taskId": exam_id, "content": "main"})
    exam_submit = student.post_form("/student/exam/submit", {"taskId": exam_id, "content": "main", "auto": "false", "uploadQuestionId": "1"})
    exam_row = row_dict(
        db.query("SELECT status,score,content FROM exam_record WHERE task_id=%s AND student_id=%s" % (exam_id, reg["id"])),
        ["status", "score", "content"],
    ) if db.enabled else {}

    judge = student.post_json(
        "/api/v2/judge/submit",
        {"taskId": programming_id, "language": "python", "code": "a,b=map(int,input().split())\nprint(a+b)"},
    )
    judge_json = json_loads(judge.body)
    scores = student.get("/student/scores")
    ai = student.post_json("/api/v2/ai/chat", {"courseId": course_id, "courseName": fixture["course"]["name"], "message": "请用一句话解释端到端测试"})
    ai_json = json_loads(ai.body)

    actual = {
        "register": reg,
        "login": http_summary(login_res),
        "browse_course_selection": {"status": browse.status, "contains_course_code": fixture["course"]["code"] in browse.body},
        "select_course": http_summary(select),
        "my_courses": {"status": my_courses.status, "contains_course_name": fixture["course"]["name"] in my_courses.body},
        "data_consistency": {"enrollment_count": enrollment, "class_current_count_max": class_count},
        "resource": {
            "download_status": download.status,
            "progress_response": json_loads(progress.body),
            "db_progress": progress_row,
        },
        "discussion": {"post": http_summary(post), "post_id": post_id},
        "homework": {"submit": http_summary(homework_submit), "submission_id": homework_submission},
        "exam": {"begin": http_summary(exam_begin), "save": json_loads(exam_save.body), "submit_status": exam_submit.status, "db_record": exam_row},
        "programming": {"http_status": judge.status, "response": judge_json},
        "scores": {"status": scores.status, "contains_course": fixture["course"]["name"] in scores.body},
        "ai": {"status": ai.status, "response": ai_json},
    }
    checks = [
        reg["response"]["status"] == 302,
        "registered=1" in reg["response"]["location"],
        ok_redirect(login_res, "/"),
        browse.status == 200,
        enrollment == "1",
        my_courses.status == 200 and fixture["course"]["name"] in my_courses.body,
        download.status == 200,
        progress_row.get("progress") in ("25.00", "25.0", "25"),
        bool(post_id),
        bool(homework_submission),
        exam_row.get("status") == "SUBMITTED",
        ((judge_json.get("data") or {}).get("status") == "AC"),
        scores.status == 200 and fixture["course"]["name"] in scores.body,
        ai.status == 200 and bool((ai_json.get("data") or {}).get("reply")),
    ]
    passed = all(checks)
    return fixture, actual, passed


def teacher_full_flow(args, db, stamp, pdf_path):
    teacher = Client(args.base_url)
    teacher_username = "t2_" + stamp[-10:]
    reg = register_user(teacher, db, teacher_username, "teacher")
    login_res = login(teacher, teacher_username)
    fixture = prepare_round_course(db, teacher, stamp, "T2", pdf_path, args.video_path)
    course_id = fixture["course"]["id"]
    class_info = create_class(teacher, db, course_id, "教师主线班级", 20)

    student = Client(args.base_url)
    student_login = login(student, "student_005")
    select = student.post_form("/student/course/select", {"courseId": course_id})
    hw_id = fixture["tasks"]["homework"]["id"]
    exam_id = fixture["tasks"]["exam"]["id"]
    programming_id = fixture["tasks"]["programming"]["id"]
    hw_submit = student.post_form("/student/task/submit", {"taskId": hw_id, "content": "教师主线学生作业"})
    judge = student.post_json(
        "/api/v2/judge/submit",
        {"taskId": programming_id, "language": "python", "code": "a,b=map(int,input().split())\nprint(a+b)"},
    )
    student.post_form("/student/exam/begin", {"taskId": exam_id})
    student.post_form("/student/exam/submit", {"taskId": exam_id, "content": "main", "auto": "false", "uploadQuestionId": "1"})

    hw_submission = first_id(
        db,
        "SELECT id FROM submission WHERE task_id=%s AND student_id=(SELECT id FROM `user` WHERE username='student_005')" % hw_id,
    ) if db.enabled else ""
    grade_hw = teacher.post_form("/teacher/task/grade", {"submissionId": hw_submission, "score": "92", "comment": "教师主线批改"})
    programming_submission = first_id(
        db,
        "SELECT id FROM submission WHERE task_id=%s AND student_id=(SELECT id FROM `user` WHERE username='student_005')" % programming_id,
    ) if db.enabled else ""
    review_programming = teacher.post_form("/teacher/task/grade", {"submissionId": programming_submission, "score": "90", "comment": "教师复核覆盖自动评分"})

    stats = teacher.get("/teacher/score/statistics")
    export = teacher.get("/teacher/score/export?courseId=" + str(course_id))
    archive = teacher.get("/teacher/course/archive/" + str(course_id))
    course_status = db.scalar("SELECT status FROM course WHERE id=" + str(course_id), "") if db.enabled else ""
    graded = row_dict(
        db.query("SELECT status,score,feedback FROM submission WHERE id=" + str(hw_submission)),
        ["status", "score", "feedback"],
    ) if db.enabled and hw_submission else {}
    reviewed = row_dict(
        db.query("SELECT status,score,feedback FROM submission WHERE id=" + str(programming_submission)),
        ["status", "score", "feedback"],
    ) if db.enabled and programming_submission else {}

    actual = {
        "register_teacher": reg,
        "login_teacher": http_summary(login_res),
        "course": fixture["course"],
        "class": class_info,
        "resources": fixture["resources"],
        "tasks": fixture["tasks"],
        "student_fixture": {"login": http_summary(student_login), "select": http_summary(select), "homework_submit": http_summary(hw_submit), "judge": json_loads(judge.body)},
        "grade_homework": {"response": http_summary(grade_hw), "db_submission": graded},
        "review_programming_score": {"response": http_summary(review_programming), "db_submission": reviewed},
        "statistics": {"page_status": stats.status, "contains_course": fixture["course"]["name"] in stats.body},
        "export": {"status": export.status, "content_type": export.headers.get("Content-Type", ""), "contains_student": "student_005" in export.body},
        "archive": {"response": http_summary(archive), "db_status": course_status},
    }
    passed = (
        ok_redirect(login_res, "/")
        and bool(course_id)
        and bool(class_info["id"])
        and bool(fixture["resources"]["pdf"]["id"])
        and bool(hw_submission)
        and graded.get("status") == "graded"
        and reviewed.get("score") in ("90.0", "90.00", "90")
        and stats.status == 200
        and export.status == 200
        and course_status == "archived"
    )
    return fixture, actual, passed


def admin_governance(args, db, stamp):
    admin = Client(args.base_url)
    admin_login = login(admin, "admin")
    temp = Client(args.base_url)
    username = "adm_" + stamp[-10:]
    reg = register_user(temp, db, username, "student")
    users_page = admin.get("/admin/users?role=student")
    update = admin.post_form(
        "/admin/users/update",
        {"userId": reg["id"], "name": "管理员主线用户", "email": "admin-mainline@example.com", "role": "teacher"},
    )
    reset = admin.post_form("/admin/users/reset-password", {"userId": reg["id"], "password": "654321"})
    changed = Client(args.base_url)
    changed_login = login(changed, username, "654321")
    self_delete = admin.post_form("/admin/users/delete", {"userId": user_id(db, "admin")})
    normal_access = changed.get("/admin/users")
    delete = admin.post_form("/admin/users/delete", {"userId": reg["id"]})
    remaining = db.scalar("SELECT COUNT(*) FROM `user` WHERE username=" + sql_str(username), "") if db.enabled else ""
    log_count = db.scalar("SELECT COUNT(*) FROM operation_log WHERE username='admin' AND action LIKE '管理员%'", "") if db.enabled else ""

    actual = {
        "admin_login": http_summary(admin_login),
        "temporary_user": reg,
        "query_users": {"status": users_page.status, "contains_temp_user": username in users_page.body},
        "update_role": http_summary(update),
        "reset_password": http_summary(reset),
        "login_with_new_password": http_summary(changed_login),
        "delete_unique_or_current_admin": http_summary(self_delete),
        "ordinary_user_access_admin": http_summary(normal_access),
        "delete_user": http_summary(delete),
        "db_after_delete": {"remaining_count": remaining},
        "operation_log_count": log_count,
        "unsupported_requirement": "当前代码未提供禁用用户入口；普通用户访问管理页实际重定向 /login，而不是需求中的 HTTP 403。",
    }
    passed = (
        ok_redirect(admin_login, "/")
        and users_page.status == 200
        and ok_redirect(update, "message=updated")
        and ok_redirect(reset, "message=passwordReset")
        and ok_redirect(changed_login, "/")
        and ok_redirect(self_delete, "message=selfDeleteBlocked")
        and ok_redirect(delete, "message=deleted")
        and remaining == "0"
    )
    return actual, passed


def fault_tolerance_flow(args, db, teacher, stamp):
    setup = create_course(teacher, db, "F4", stamp)
    course_id = setup["id"]
    default_class_id = first_id(db, "SELECT id FROM course_class WHERE course_id=%s ORDER BY id LIMIT 1" % course_id) if db.enabled else ""
    if db.enabled and default_class_id:
        db.execute("UPDATE course_class SET max_count=1,current_count=0 WHERE id=" + str(default_class_id))

    s1 = Client(args.base_url)
    s2 = Client(args.base_url)
    u1 = "f4a_" + stamp[-10:]
    u2 = "f4b_" + stamp[-10:]
    r1 = register_user(s1, db, u1, "student")
    r2 = register_user(s2, db, u2, "student")
    login(s1, u1)
    login(s2, u2)
    first_select = s1.post_form("/student/course/select", {"courseId": course_id})
    duplicate_select = s1.post_form("/student/course/select", {"courseId": course_id})
    duplicate_count = db.scalar(
        "SELECT COUNT(*) FROM course_enrollment WHERE student_id=%s AND course_id=%s" % (r1["id"], course_id),
        "0",
    ) if db.enabled else ""

    second_select = s2.post_form("/student/course/select", {"courseId": course_id})
    enrollment_total = db.scalar("SELECT COUNT(*) FROM course_enrollment WHERE course_id=" + str(course_id), "0") if db.enabled else ""
    class_row = row_dict(
        db.query("SELECT max_count,current_count FROM course_class WHERE id=" + str(default_class_id)),
        ["max_count", "current_count"],
    ) if db.enabled and default_class_id else {}

    no_join = create_course(teacher, db, "F4C", stamp, status="closed", allow_join="false")
    closed_select = s1.post_form("/student/course/select", {"courseId": no_join["id"]})
    closed_count = db.scalar(
        "SELECT COUNT(*) FROM course_enrollment WHERE student_id=%s AND course_id=%s" % (r1["id"], no_join["id"]),
        "0",
    ) if db.enabled else ""

    ai = s1.post_json("/api/v2/ai/chat", {"courseId": course_id, "courseName": setup["name"], "message": "触发AI降级检查"})
    ai_json = json_loads(ai.body)

    programming = create_task(
        teacher,
        db,
        course_id,
        "F4编程降级检查" + stamp,
        "programming",
        {
            "allowedLanguage": "python",
            "testCases": "---CASE---\n1 2\n---OUTPUT---\n3\n---WEIGHT---\n1",
        },
    )
    judge = s1.post_json(
        "/api/v2/judge/submit",
        {"taskId": programming["id"], "language": "python", "code": "a,b=map(int,input().split())\nprint(a+b)"},
    )
    judge_json = json_loads(judge.body)
    judge_data = judge_json.get("data") or {}

    actual = {
        "duplicate_enrollment": {
            "first_select": http_summary(first_select),
            "duplicate_select": http_summary(duplicate_select),
            "student_course_count": duplicate_count,
        },
        "capacity_limit": {
            "setup": {"course_id": course_id, "class_id": default_class_id, "max_count": class_row.get("max_count")},
            "second_student_select": http_summary(second_select),
            "total_enrollments": enrollment_total,
            "class_count": class_row,
            "requirement_expected": "仅 1 人成功",
            "actual_observation": "当前 CourseService.enroll 未检查 max_count；如果 total_enrollments > 1，则说明容量控制需求未实现。",
        },
        "closed_course_rejection": {
            "course": no_join,
            "select": http_summary(closed_select),
            "enrollment_count": closed_count,
        },
        "ai_degradation": {"status": ai.status, "response": ai_json},
        "judge0_fallback": {
            "status": judge.status,
            "response": judge_json,
            "note": "若 usedLocalJudge=false，说明当前环境 Judge0 可用；需以 JUDGE0_API_URL=http://127.0.0.1:9 重启应用后复跑，才能验证本地判题降级。",
        },
        "unsupported_fault_injection": [
            "当前项目不是拆分部署的 user-service，无法在不改代码/不加代理的情况下单独 Mock user-service 超时。",
            "批改通知失败降级当前代码未实现可控通知服务调用，无法直接故障注入。",
        ],
    }
    duplicate_ok = duplicate_count == "1"
    closed_ok = closed_count == "0"
    ai_ok = ai.status == 200 and bool((ai_json.get("data") or {}).get("reply"))
    judge_observed = judge.status == 200 and judge_json.get("code") == 200
    capacity_ok = enrollment_total == "1"
    passed = duplicate_ok and closed_ok and ai_ok and judge_observed and capacity_ok
    notes = []
    if duplicate_ok:
        notes.append("重复选课未产生重复记录")
    else:
        notes.append("重复选课产生了异常记录")
    if closed_ok:
        notes.append("已结课/禁止加入课程未产生选课记录")
    else:
        notes.append("关闭课程仍产生选课记录")
    if not capacity_ok:
        notes.append("容量上限控制不符合 UC17：最后 1 个名额场景下实际选课数为 %s" % enrollment_total)
    if not judge_data.get("usedLocalJudge"):
        notes.append("Judge0 不可用降级未被触发；需要用不可达 Judge0 地址重启后验证")
    return actual, passed, "；".join(notes)


def markdown_matrix(cases):
    lines = []
    lines.append("# 端到端测试文档")
    lines.append("")
    lines.append("[toc]")
    lines.append("")
    lines.append("## 测试用例编号方式及对照表")
    lines.append("")
    lines.append("首字母为测试用例所属端到端业务主线，后跟 3 位十进制序号。最后一位为 0 代表主成功流程，1-9 代表异常、降级或边界流程。")
    lines.append("")
    lines.append("| 字母 | 对应模块 |")
    lines.append("|---|---|")
    lines.append("| E | 端到端测试准备工作 |")
    lines.append("| S | 主线一_学生全流程 |")
    lines.append("| T | 主线二_教师全流程 |")
    lines.append("| A | 主线三_管理员治理 |")
    lines.append("| F | 主线四_跨服务异常与降级容错闭环 |")
    lines.append("")
    lines.append("## 端到端业务主线测试")
    lines.append("")
    for case in cases:
        lines.append("### " + case["测试编号"] + " " + case["测试名称"])
        lines.append("")
        lines.append("**测试函数（代码语言）**")
        lines.append("")
        lines.append("```text")
        lines.append(case["测试函数（代码语言）"])
        lines.append("```")
        lines.append("")
        lines.append("**输入数据（代码语言）**")
        lines.append("")
        lines.append("```json")
        lines.append(json.dumps(case["输入数据（代码语言）"], ensure_ascii=False, indent=2))
        lines.append("```")
        lines.append("")
        lines.append("**前置条件（自然语言）**")
        lines.append("")
        lines.append(case["前置条件（自然语言）"])
        lines.append("")
        lines.append("**预期输出（自然语言）**")
        lines.append("")
        lines.append(case["预期输出（自然语言）"])
        lines.append("")
        lines.append("**实际输出（代码语言）**")
        lines.append("")
        lines.append("```json")
        lines.append(json.dumps(case["实际输出（代码语言）"], ensure_ascii=False, indent=2))
        lines.append("```")
        lines.append("")
        lines.append("**断言结果**")
        lines.append("")
        lines.append(case["断言结果"])
        lines.append("")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Teaching Platform four-mainline E2E tests")
    parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost:8080"))
    parser.add_argument("--db-user", default=os.getenv("DB_USERNAME", ""))
    parser.add_argument("--db-pass", default=os.getenv("DB_PASSWORD", ""))
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "localhost"))
    parser.add_argument("--db-port", default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "teaching_platform"))
    parser.add_argument("--mysql", default=os.getenv("MYSQL_BIN", "mysql"))
    parser.add_argument("--format", choices=["markdown", "json"], default="markdown")
    parser.add_argument("--output", default="")
    parser.add_argument("--strict-exit", action="store_true")
    parser.add_argument("--pdf-path", default=os.path.join("docs", "测试文档_样例.pdf"))
    parser.add_argument("--video-path", default=os.path.join("uploads", "resources", "2", "1780801920737_bandicam 2026-06-07 10-15-00-438.mp4"))
    args = parser.parse_args()

    stamp = time.strftime("%Y%m%d%H%M%S")
    cases = []
    db = Db(args)

    prep_actual = {"base_url": args.base_url, "db_name": args.db_name, "seed_accounts": {}, "files": {}}
    prep_passed = True
    try:
        with urllib.request.urlopen(args.base_url.rstrip("/") + "/login", timeout=5) as resp:
            prep_actual["server"] = {"login_page_status": resp.status}
    except Exception as exc:
        prep_actual["server"] = {"error": str(exc)}
        prep_passed = False

    try:
        if db.enabled:
            db.scalar("SELECT 1")
            for account in ["admin", "teacher_demo", "student_006", "student_005"]:
                prep_actual["seed_accounts"][account] = db.scalar("SELECT COUNT(*) FROM `user` WHERE username=" + sql_str(account), "0")
    except Exception as exc:
        prep_actual["database"] = {"error": str(exc)}
        prep_passed = False

    pdf_path = os.path.abspath(args.pdf_path)
    video_path = os.path.abspath(args.video_path)
    prep_actual["files"] = {
        "pdf_path": pdf_path,
        "pdf_exists": os.path.exists(pdf_path),
        "video_path": video_path,
        "video_exists": os.path.exists(video_path),
    }
    if not os.path.exists(pdf_path) or not os.path.exists(video_path):
        prep_passed = False

    add_case(
        cases,
        "准备工作：部署所有服务 + 准备测试数据",
        "E000",
        "GET /login\nmysql SELECT 1\nSELECT COUNT(*) FROM user WHERE username IN (...)",
        {
            "baseUrl": args.base_url,
            "db": {"host": args.db_host, "port": args.db_port, "name": args.db_name, "user": args.db_user},
            "requiredAccounts": ["admin", "teacher_demo", "student_006", "student_005"],
            "requiredFiles": [args.pdf_path, args.video_path],
        },
        "平台已通过 run-dev.ps1 -SeedTestData 启动；MySQL 可连接；测试账号和上传样例文件存在。",
        "登录页返回 200；数据库连接正常；admin、teacher_demo、student_006、student_005 存在；PDF/视频文件存在。",
        prep_actual,
        "通过：端到端测试环境可用。" if prep_passed else "失败/阻塞：端到端测试环境不可用，需先启动平台或补齐数据库/文件。",
        prep_passed,
    )

    teacher = Client(args.base_url)
    teacher_login = login(teacher, "teacher_demo")

    if prep_passed and ok_redirect(teacher_login, "/"):
        student_fixture, student_actual, student_passed = student_full_flow(args, db, teacher, stamp, pdf_path)
    else:
        student_actual, student_passed = {"blocked": "准备工作或 teacher_demo 登录失败"}, False
    add_case(
        cases,
        "主线一_学生：注册 -> 登录 -> 选课 -> 学资源 -> 讨论 -> 作业 -> 考试 -> 编程 -> 成绩 -> AI",
        "S000",
        "UserController.registerUser/loginUser\nStudentController.selectCourse/taskSubmit/examBegin/examSave/examSubmit/scoreSummary\nTeachingResourceController.downloadPdf/updateProgress\nDiscussionController.createPost\nJudgeController.submitAndJudge\nAiController.chat",
        {
            "student": "s1_" + stamp[-10:],
            "courseFixture": "由 teacher_demo 在脚本准备阶段创建",
            "resourceProgress": {"currentTime": 30, "duration": 120},
            "homeworkContent": "学生主线普通作业提交",
            "examAnswer": "main",
            "programmingCode": "a,b=map(int,input().split())\\nprint(a+b)",
            "aiMessage": "请用一句话解释端到端测试",
        },
        "新学生账号未占用；教师测试课程、PDF/视频资源、普通作业、考试、编程题已准备；学生具备选课权限。",
        "学生可注册登录；选课记录、班级人数、我的课程列表一致；资源下载和视频进度保存成功；讨论帖保存；普通作业提交；考试暂存并提交；编程题 AC；成绩页可见；AI 返回内容或明确降级提示。",
        student_actual,
        "通过：学生端核心用户旅程闭环通过。" if student_passed else "失败/阻塞：学生端核心用户旅程存在未通过步骤，见实际输出。",
        student_passed,
    )

    if prep_passed:
        teacher_fixture, teacher_actual, teacher_passed = teacher_full_flow(args, db, stamp, pdf_path)
    else:
        teacher_actual, teacher_passed = {"blocked": "准备工作失败"}, False
    add_case(
        cases,
        "主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发资源/任务 -> 批改复核 -> 统计 -> 归档",
        "T000",
        "UserController.registerUser/loginUser\nTeacherController.createCourse/createClass/createTask/submitGrade/scoreStatistics/exportScores/archiveCourse\nTeachingResourceController.uploadResource\nJudgeController.submitAndJudge",
        {
            "teacher": "t2_" + stamp[-10:],
            "course": "T2课程" + stamp,
            "class": {"name": "教师主线班级", "maxCount": 20},
            "tasks": ["普通作业", "考试", "编程题"],
            "reviewScore": 90,
            "archiveAtEnd": True,
        },
        "教师账号可注册；学生 student_005 可作为提交数据；PDF/视频样例文件存在；数据库可校验最终状态。",
        "教师完成内容生产和管理闭环：课程/班级/资源/任务创建成功；学生提交后教师批改普通作业并复核覆盖编程评分；成绩统计和 CSV 导出可用；课程最终归档。",
        teacher_actual,
        "通过：教师端内容生产与管理闭环通过。" if teacher_passed else "失败/阻塞：教师端闭环存在未通过步骤，见实际输出。",
        teacher_passed,
    )

    if prep_passed:
        admin_actual, admin_passed = admin_governance(args, db, stamp)
    else:
        admin_actual, admin_passed = {"blocked": "准备工作失败"}, False
    add_case(
        cases,
        "主线三_管理员：登录 -> 查询用户 -> 修改角色 -> 重置密码 -> 删除用户 -> 验证日志",
        "A000",
        "AdminController.users/updateUser/resetPassword/deleteUser/logs\nUserController.loginUser",
        {
            "admin": "admin",
            "temporaryUser": "adm_" + stamp[-10:],
            "update": {"role": "teacher", "email": "admin-mainline@example.com"},
            "resetPassword": "654321",
            "selfDeleteProtection": True,
        },
        "管理员账号 admin 存在；临时学生用户可注册；数据库可查询 user 和 operation_log。",
        "管理员可查询用户；可修改资料和角色；重置密码后用户可用新密码登录；删除当前管理员被拒绝；删除普通用户成功；操作日志可查询。",
        admin_actual,
        "通过：管理员治理主线通过。补充观察：禁用用户和普通用户访问返回 HTTP 403 在当前代码中未实现。" if admin_passed else "失败/阻塞：管理员治理主线存在未通过步骤，见实际输出。",
        admin_passed,
    )

    if prep_passed and ok_redirect(teacher_login, "/"):
        fault_actual, fault_passed, fault_notes = fault_tolerance_flow(args, db, teacher, stamp)
    else:
        fault_actual, fault_passed, fault_notes = {"blocked": "准备工作或 teacher_demo 登录失败"}, False, "未执行"
    add_case(
        cases,
        "主线四_跨服务异常与降级容错闭环：选课/AI/Judge0/容量边界",
        "F010",
        "CourseService.enroll\nStudentController.selectCourse\nAiController.chat\nJudgeController.submitAndJudge",
        {
            "duplicateEnrollment": "同一学生连续两次 POST /student/course/select",
            "closedCourse": {"status": "closed", "allowJoin": False},
            "capacity": {"classMaxCount": 1, "studentsTryingToEnroll": 2},
            "ai": {"message": "触发AI降级检查"},
            "judge0Fallback": "当前启动配置下观察 usedLocalJudge；若需强制降级，重启时设置 JUDGE0_API_URL=http://127.0.0.1:9",
        },
        "平台运行；teacher_demo 可创建异常场景课程；DB 可设置容量边界；AI Key 可缺省；Judge0 降级需要特定启动配置才能强制触发。",
        "重复选课不产生重复记录；关闭课程不产生选课记录；AI 不可用时返回明确提示；Judge0 不可用时本地判题降级；并发/容量边界只允许容量范围内请求成功。",
        fault_actual,
        ("通过：异常与降级容错主线通过。%s" % fault_notes) if fault_passed else ("部分通过/发现缺口：%s" % fault_notes),
        fault_passed,
    )

    output = json.dumps(cases, ensure_ascii=False, indent=2) if args.format == "json" else markdown_matrix(cases)
    if args.output:
        with open(args.output, "w", encoding="utf-8") as stream:
            stream.write(output)
    print(output)

    if args.strict_exit and not all(case["通过"] for case in cases):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
