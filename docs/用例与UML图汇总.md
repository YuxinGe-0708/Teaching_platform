# 在线教学与实训平台 — 用例与UML图汇总

## 一、用例图

![系统用例图](../../软件需求规格说明书/系统用例图.png)

## 二、概念类图

![概念类图](../../软件需求规格说明书/概念类图.png)

## 三、类图

![类图](../../软件详细设计说明书/类图.png)

## 四、用例描述与UML图

### UC01 用户注册登录并按角色进入工作台


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 用户注册登录并按角色进入工作台 |
| 用例编号 | UC01 |
| 参与者 | 访客、学生、教师、管理员 |
| 触发器 | 访客访问 `GET /register` 并提交 `POST /register`；用户提交 `POST /login` |
| 前置条件 | 注册：当前未登录，用户名未被占用；登录：`user` 表中存在用户名和密码匹配的用户 |
| 后置条件 | 注册：创建 `User` 记录，`role` 为 `student` 或 `teacher`，页面跳转到 `/login?registered=1`。<br>登录：Session 中设置 `currentUser`，页面跳转 `/`。 |
| 关联函数 | `UserController.showRegisterForm()`<br>`UserController.registerUser(...)`<br>`UserController.showLoginForm(...)`<br>`UserController.loginUser(...)`<br>`UserService.register(...)`<br>`UserService.login(...)` |
| 基本事件流 | （注册）1、访客点击注册入口，访问 `GET /register`。<br>（系统）2、系统显示 `register.html` 注册页面。<br>（用户）3、访客填写 `username`、`password`、`role`。<br>（系统）4、系统校验用户名长度是否为 3-20 个字符、密码长度是否为 6-32 个字符、角色是否为 `student` 或 `teacher`。<br>（用户）5、访客提交注册表单。<br>（系统）6、系统调用 `UserService.register(...)` 创建用户。<br>（系统）7、注册成功后跳转到 `/login?registered=1`。<br>（登录）8、用户访问 `GET /login`。<br>（系统）9、系统显示 `login.html` 登录页面。<br>（用户）10、用户填写 `username` 和 `password` 并提交。<br>（系统）11、系统调用 `UserService.login(...)` 校验账号密码。<br>（系统）12、校验通过后，将用户对象写入 Session 的 `currentUser`。<br>（系统）13、系统跳转到 `/`。 |
| 扩展事件流 | （注册异常）4a、用户名长度不符合要求 → 返回 `register`，提示用户名长度错误。<br>4b、密码长度不符合要求 → 返回 `register`，提示密码长度错误。<br>4c、角色不是 `student` 或 `teacher` → 返回 `register`，提示身份选择无效。<br>6a、用户名已存在 → 返回 `register`，提示用户名已存在。<br>（登录异常）11a、用户名不存在或密码不匹配 → 返回 `login`，提示用户名或密码错误。 |




**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/系统顺序.jpg)

![](../../软件需求规格说明书/登录.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/B.png)

![](../../软件概要设计说明书/软件概要设计说明书/I.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC01用户注册登录并按角色进入工作台顺序图.png)

---


### UC03 管理员查询、修改和删除用户


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 管理员查询、修改和删除用户 |
| 用例编号 | UC03 |
| 参与者 | 管理员 |
| 触发器 | 管理员访问 `/admin/dashboard`、`/admin/users`、`/admin/logs` |
| 前置条件 | 当前用户 `role=admin` |
| 后置条件 | 用户信息被修改或删除；通知被发布或删除；操作日志被记录 |
| 关联函数 | `AdminController.dashboard(...)`<br>`AdminController.users(...)`<br>`AdminController.updateUser(...)`<br>`AdminController.resetPassword(...)`<br>`AdminController.deleteUser(...)`<br>`AdminController.publishNotification(...)`<br>`AdminController.deleteNotification(...)`<br>`AdminController.logs(...)` |
| 基本事件流 | （用户）1、管理员访问 `/admin/dashboard`。<br>（系统）2、系统校验当前用户 `role=admin`。<br>（系统）3、系统显示用户数、学生数、教师数、管理员数、课程数、任务数和提交数。<br>（用户）4、管理员访问 `/admin/users` 并可按 `role` 筛选用户。<br>（用户）5、管理员修改用户姓名、邮箱或角色。<br>（系统）6、系统调用 `UserService.updateByAdmin(...)` 保存修改并写入操作日志。<br>（用户）7、管理员重置用户密码或删除用户。<br>（系统）8、系统调用 `UserService.resetPassword(...)` 或 `UserService.deleteUser(...)`。<br>（用户）9、管理员发布或删除公告。<br>（系统）10、系统写入或删除 `notification`，并写入操作日志。<br>（用户）11、管理员查看 `/admin/logs`。<br>（系统）12、系统显示最近操作日志。 |
| 扩展事件流 | （异常处理）2a、未登录或当前用户不是管理员 → 重定向到 `/login`。<br>7a、管理员删除当前登录的自己 → 重定向到 `/admin/users?message=selfDeleteBlocked`。<br>5a、用户更新、密码重置或删除失败 → 重定向到用户管理页，提示失败。 |


**系统级顺序图（需求文档）：**

（需求文档中无对应图）

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/G-1.jpg)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC03管理员查询、修改和删除用户.png)

---


### UC04 教师创建、修改和归档课程


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 教师创建、修改和归档课程 |
| 用例编号 | UC04 |
| 参与者 | 教师 |
| 触发器 | 访问 `/teacher/course/*` |
| 前置条件 | 教师已登录 |
| 后置条件 | `course` 数据被创建或更新 |
| 关联函数 | `TeacherController.createCourse(...)`<br>`TeacherController.updateCourse(...)`<br>`TeacherController.deleteCourse(...)`<br>`TeacherController.archiveCourse(...)`<br>`CourseService.createCourse(...)` |
| 基本事件流 | （用户）1、教师访问课程管理页面。<br>（系统）2、系统显示教师本人创建的课程列表。<br>（用户）3、教师填写课程名称、课程编号、学分、学科分类、学时、加入开关、状态和描述。<br>（系统）4、系统调用 `CourseService.createCourse(...)` 创建课程。<br>（系统）5、系统生成课程 `inviteCode`，并创建"默认班级"。<br>（用户）6、教师可搜索、编辑、归档或删除课程。 |
| 扩展事件流 | （异常处理）4a、当前登录用户不是教师 → 重定向到 `/login`。<br>6a、教师删除已有学生数据的课程 → 系统调用 `courseService.updateStatus(id,"archived")` 归档课程。<br>6b、教师删除无学生数据的课程 → 系统调用 `courseService.deleteCourse(id)` 删除课程。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/教师课程.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/教师管理课程-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC04教师创建、修改和归档课程.png)

**状态图（详细设计）：**

![](../../软件详细设计说明书/课程状态图.png)

---


### UC05 学生选择或退出课程


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生选择或退出课程 |
| 用例编号 | UC05 |
| 参与者 | 学生 |
| 触发器 | `POST /student/course/select` 或 `POST /student/course/drop` |
| 前置条件 | 学生已登录；选课时课程为 `active` 且 `allowJoin=true` |
| 后置条件 | `course_enrollment` 增加或删除记录，`course_class.current_count` 相应增减 |
| 关联函数 | `StudentController.showCourseSelection(...)`<br>`StudentController.selectCourse(...)`<br>`StudentController.dropCourse(...)`<br>`CourseService.enroll(...)`<br>`CourseService.unenroll(...)` |
| 基本事件流 | （用户）1、学生访问 `GET /student/course/selection`。<br>（系统）2、系统调用 `CourseService.getAllActiveCourses()` 查询可选课程。<br>（系统）3、系统显示选课中心，支持按 `search` 搜索课程名称或课程编号。<br>（用户）4、学生选择课程并提交 `POST /student/course/select`。<br>（系统）5、系统调用 `CourseService.enroll(...)` 创建 `CourseEnrollment`。<br>（系统）6、系统将学生默认加入课程下第一个 `CourseClass`，并增加 `current_count`。<br>（用户）7、学生在我的课程页面提交退课请求。<br>（系统）8、系统调用 `CourseService.unenroll(...)` 删除选课记录并减少班级人数。 |
| 扩展事件流 | （异常处理）5a、课程不存在、课程不是 `active` 或 `allowJoin=false` → `CourseService.enroll(...)` 返回 false。<br>5b、学生已选该课程 → `CourseService.enroll(...)` 返回 false。<br>8a、退课记录不存在 → `CourseService.unenroll(...)` 返回 false。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/选课.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/F.jpg)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC05学生选择或退出课程.png)

---


### UC06 教师创建班级并管理班级成员


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 教师创建班级并管理班级成员 |
| 用例编号 | UC06 |
| 参与者 | 教师 |
| 触发器 | 访问 `/teacher/course/class/*` |
| 前置条件 | 教师已登录；课程属于当前教师 |
| 后置条件 | `course_class`、`course_enrollment` 数据被创建或更新 |
| 关联函数 | `TeacherController.createClass(...)`<br>`TeacherController.updateClass(...)`<br>`TeacherController.removeStudentFromClass(...)`<br>`TeacherController.deleteClass(...)` |
| 基本事件流 | （用户）1、教师进入班级管理页面。<br>（用户）2、教师新增班级或修改班级名称、人数上限。<br>（系统）3、系统写入或更新 `course_class`。<br>（用户）4、教师可移除课程学生或删除班级。 |
| 扩展事件流 | （异常处理）2a、课程不属于当前教师 → 重定向到 `/teacher/course/manage`。 |


**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/教师课程.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/教师管理课程-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC06教师创建班级并管理班级成员.png)

---


### UC07 教师发布资源，学生学习并记录进度


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 教师发布资源，学生学习并记录进度 |
| 用例编号 | UC07 |
| 参与者 | 教师、学生 |
| 触发器 | 教师上传资源或学生访问资源 |
| 前置条件 | 教师拥有课程权限；学生已选课程 |
| 后置条件 | `resource` 记录被创建、更新或删除；`resource_progress` 更新学习进度 |
| 关联函数 | `TeachingResourceController.uploadResource(...)`<br>`TeachingResourceController.editResource(...)`<br>`TeachingResourceController.deleteResource(...)`<br>`TeachingResourceController.viewResource(...)`<br>`TeachingResourceController.updateVideoProgress(...)`<br>`TeachingResourceService.saveResource(...)` |
| 基本事件流 | （用户）1、教师进入课程资源管理页。<br>（系统）2、系统显示该课程已有资源列表。<br>（用户）3、教师上传文件，并填写标题、章节和资源类型。<br>（系统）4、系统将文件保存到 `uploads/resources/{courseId}`，并在 `resource` 表中插入记录。<br>（用户）5、教师可编辑资源标题、章节信息或删除资源。<br>（用户）6、学生访问课程资源页面，查看资源列表。<br>（系统）7、系统显示资源，学生可下载 PDF 或播放视频。<br>（用户）8、学生观看视频时，前端定时上报播放进度。<br>（系统）9、系统调用 `updateVideoProgress(...)` 更新 `resource_progress`。 |
| 扩展事件流 | （异常处理）3a、文件大小超过限制 → 返回上传失败提示。<br>3b、资源类型不支持 → 返回类型不支持提示。<br>4a、课程不属于当前教师 → 重定向到课程管理页。<br>6a、学生未选课程 → 重定向到我的课程页面。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/教学资源.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/D.jpg)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC07教师发布资源，学生学习并记录进度.png)

---


### UC08 学生发帖提问，教师回复并收到通知


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生发帖提问，教师回复并收到通知 |
| 用例编号 | UC08 |
| 参与者 | 学生、教师 |
| 触发器 | 用户访问 `/discussion/*` 发帖或回复 |
| 前置条件 | 用户已登录；课程存在且用户有访问权限 |
| 后置条件 | `discussion_post` 或 `discussion_reply` 记录被创建；`notification` 被写入 |
| 关联函数 | `DiscussionController.postList(...)`<br>`DiscussionController.postDetail(...)`<br>`DiscussionController.createPost(...)`<br>`DiscussionController.reply(...)`<br>`DiscussionService.createPost(...)`<br>`DiscussionService.createReply(...)` |
| 基本事件流 | （用户）1、用户进入课程讨论区，查看帖子列表。<br>（系统）2、系统显示该课程已有帖子。<br>（用户）3、学生发帖，可匿名或定向提问。<br>（系统）4、系统调用 `DiscussionService.createPost(...)` 创建帖子。<br>（用户）5、教师或其他学生回复帖子。<br>（系统）6、系统调用 `DiscussionService.createReply(...)` 创建回复。<br>（系统）7、若帖子为定向提问且回复者为教师，系统写入通知。<br>（用户）8、用户查看通知，跳转到帖子详情。 |
| 扩展事件流 | （异常处理）3a、帖子内容为空 → 返回提示"内容不能为空"。<br>4a、课程不存在或用户无权限 → 返回错误提示。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/课程讨论.png)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/课程讨论-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC08学生发帖提问，教师回复并收到通知.png)

---


### UC09 学生使用 AI 进行答疑、总结和思维导图生成


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生使用 AI 进行答疑、总结和思维导图生成 |
| 用例编号 | UC09 |
| 参与者 | 学生、AI 服务 |
| 触发器 | 学生提交 `POST /api/v2/ai/chat` 或 `POST /api/v2/ai/study` |
| 前置条件 | 用户已登录；AI API Key 已配置 |
| 后置条件 | 返回 AI 生成内容；`study_note` 可被创建或更新 |
| 关联函数 | `AiController.chat(...)`<br>`AiController.study(...)`<br>`AiService.chat(...)`<br>`AiService.studyNote(...)`<br>`StudentController.createNote(...)`<br>`StudentController.updateNote(...)` |
| 基本事件流 | （用户）1、学生进入 AI 助手页面。<br>（系统）2、系统加载 AI 对话界面。<br>（用户）3、学生输入文本提问或上传视频截图进行图像讲解。<br>（系统）4、系统调用 `AiService.chat(...)` 发送请求到 AI 服务。<br>（系统）5、系统返回 AI 回答。<br>（用户）6、学生可在资源页面触发 AI 总结或思维导图生成。<br>（系统）7、系统调用 `AiService.studyNote(...)` 生成笔记内容。<br>（系统）8、学生可将 AI 生成结果保存为 `study_note`。 |
| 扩展事件流 | （异常处理）4a、AI 未配置 Key 或鉴权失败 → 返回可读提示，不回滚用户消息。<br>4b、AI 调用异常 → 回滚用户消息，提示"AI 调用失败"。<br>4c、视觉模型不支持图片 → 提示"模型可能不支持图片输入"。 |


**系统级顺序图（需求文档）：**

（需求文档中无对应图）

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/学习辅助-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC09学生使用AI进行答疑、总结和思维导图生成.png)

**活动图（详细设计）：**

![](../../软件详细设计说明书/AI多轮对话活动图.png)

---


### UC10 教师发布、修改和撤回作业或考试


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 教师发布、修改和撤回作业或考试 |
| 用例编号 | UC10 |
| 参与者 | 教师 |
| 触发器 | 教师访问 `/teacher/task/*` 创建或管理任务 |
| 前置条件 | 教师已登录；教师拥有课程权限 |
| 后置条件 | `task` 记录被创建、更新或状态变更 |
| 关联函数 | `TeacherController.createTask(...)`<br>`TeacherController.updateTask(...)`<br>`TeacherController.deleteTask(...)`<br>`TaskService.createTask(...)`<br>`TaskService.updateTaskStatus(...)` |
| 基本事件流 | （用户）1、教师进入课程任务管理页面。<br>（系统）2、系统显示该课程已有任务列表。<br>（用户）3、教师创建任务，填写标题、描述、类型（`homework`/`exam`/`programming`）、满分、截止时间。<br>（系统）4、系统调用 `TaskService.createTask(...)` 创建任务。<br>（用户）5、教师可为考试配置标准答案，为编程题配置测试用例和允许语言。<br>（用户）6、教师可修改任务信息或发布任务。<br>（系统）7、系统更新 `task` 状态为 `published`。<br>（用户）8、教师可撤回已发布任务。<br>（系统）9、系统更新 `task` 状态为 `retracted`。 |
| 扩展事件流 | （异常处理）3a、课程不属于当前教师 → 重定向到课程管理页。<br>4a、任务创建失败 → 返回错误提示。 |


**系统级顺序图（需求文档）：**

（需求文档中无对应图）

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/作业提交-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC10教师发布、修改和撤回作业或考试.png)

**状态图（详细设计）：**

![](../../软件详细设计说明书/任务状态图.png)

---


### UC11 学生提交或补交普通作业


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生提交或补交普通作业 |
| 用例编号 | UC11 |
| 参与者 | 学生 |
| 触发器 | `POST /student/task/submit` |
| 前置条件 | 学生已选任务所属课程；任务不是 `exam` |
| 后置条件 | `submission` 插入或更新 |
| 关联函数 | `StudentController.taskLibrary(...)`<br>`StudentController.taskDetail(...)`<br>`StudentController.taskSubmit(...)`<br>`TaskService.getSubmission(...)`<br>`SubmissionMapper.insert(...)`<br>`SubmissionMapper.updateContent(...)` |
| 基本事件流 | （用户）1、学生访问 `GET /student/tasks` 查看已发布任务。<br>（系统）2、系统筛选学生已选课程下 `status=published` 的任务。<br>（用户）3、学生进入 `GET /student/task/detail?taskId=...`。<br>（系统）4、系统显示任务说明、历史提交和附件提交入口。<br>（用户）5、学生填写文本答案或上传附件。<br>（系统）6、系统保存附件到 `uploads`。<br>（系统）7、系统查询是否已有 `Submission`。<br>（系统）8、无提交记录时插入 `submission`，已有记录时更新内容或附件路径。 |
| 扩展事件流 | （异常处理）3a、任务不存在或学生未选任务所属课程 → 重定向到 `/student/course/my`。<br>5a、文本和附件均为空 → 返回任务详情页面，提示"提交内容或附件不能为空"。<br>5b、任务类型为 `exam` → 重定向到 `/student/exam/start?taskId=...`。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/作业提交.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/作业提交-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC11学生提交或补交普通作业.png)

**状态图（详细设计）：**

![](../../软件详细设计说明书/提交对象状态图.png)

---


### UC12 学生开始考试、暂存答案并提交试卷


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生开始考试、暂存答案并提交试卷 |
| 用例编号 | UC12 |
| 参与者 | 学生、教师 |
| 触发器 | 学生访问 `/student/exam/start`、`/student/exam/take`，教师批改考试提交 |
| 前置条件 | 任务 `type=exam`；学生已选课程 |
| 后置条件 | `exam_record` 和 `submission` 记录考试状态、内容和成绩 |
| 关联函数 | `StudentController.examStart(...)`<br>`StudentController.examBegin(...)`<br>`StudentController.examTake(...)`<br>`StudentController.examSave(...)`<br>`StudentController.examUpload(...)`<br>`StudentController.examSubmit(...)`<br>`ExamService.beginExam(...)`<br>`ExamService.saveProgress(...)`<br>`ExamService.submitExam(...)`<br>`ExamService.autoSubmitExam(...)`<br>`ExamService.createSubmissionFromExam(...)`<br>`TeacherController.submitGrade(...)` |
| 基本事件流 | （用户）1、学生访问 `GET /student/exam/start?taskId=...`。<br>（系统）2、系统校验任务类型为 `exam`，且学生已选对应课程。<br>（系统）3、系统显示考试说明页。<br>（用户）4、学生点击开始考试，提交 `POST /student/exam/begin`。<br>（系统）5、系统调用 `ExamService.beginExam(...)` 创建或更新 `ExamRecord`。<br>（系统）6、系统跳转到 `/student/exam/take?taskId=...`。<br>（用户）7、学生在答题页填写答案，可提交 `POST /student/exam/save` 暂存。<br>（系统）8、系统调用 `ExamService.saveProgress(...)` 保存答案。<br>（用户）9、学生可通过 `POST /student/exam/upload` 上传题目附件。<br>（系统）10、系统将附件信息写入考试内容。<br>（用户）11、学生提交试卷。<br>（系统）12、系统调用 `ExamService.submitExam(...)` 或 `autoSubmitExam(...)`。<br>（系统）13、系统调用 `ExamService.createSubmissionFromExam(...)` 同步生成或更新 `Submission`。<br>（系统）14、若任务配置标准答案，系统自动判分；教师仍可进入批改页保存分数和评语。 |
| 扩展事件流 | （异常处理）2a、任务不存在、任务不是 `exam` 或学生未选课程 → 重定向到任务列表或我的课程页面。<br>3a、学生已提交考试 → 显示已完成提示，阻止重复参加。<br>7a、考试未开始或已提交时暂存答案 → 返回 `code=400`。<br>9a、考试未开始或已提交时上传附件 → 返回 `code=400`。<br>12a、剩余时间小于等于 0 且状态为 `IN_PROGRESS` → 调用 `ExamService.autoSubmitExam(...)` 自动交卷。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/考试.jpg)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/参加考试-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC12学生开始考试、暂存答案并提交试卷.png)

**状态图（详细设计）：**

![](../../软件详细设计说明书/考试记录状态图.png)

---


### UC13 学生提交编程作业并获得评测结果


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生提交编程作业并获得评测结果 |
| 用例编号 | UC13 |
| 参与者 | 学生、评测服务 |
| 触发器 | `POST /api/v2/judge/submit` |
| 前置条件 | 用户已登录；提交代码非空；若传入 `taskId`，任务必须存在且 `type=programming` |
| 后置条件 | 返回评测结果；若传入 `taskId`，写入并批改 `submission` |
| 关联函数 | `JudgeController.submitAndJudge(...)`<br>`JudgeService.judge(...)`<br>`LocalJudgeService.judge(...)`<br>`TaskService.submit(...)`<br>`SubmissionMapper.grade(...)` |
| 基本事件流 | （用户）1、学生在实训页面提交 `code`、`language` 和可选 `taskId`。<br>（系统）2、系统校验登录状态和代码非空。<br>（系统）3、若传入 `taskId`，系统查询 `Task` 并校验 `type=programming`。<br>（系统）4、系统读取任务中的服务器端测试用例。<br>（系统）5、系统检查提交语言是否符合 `allowedLanguage`。<br>（系统）6、系统调用 `JudgeService.judge(...)`。<br>（系统）7、`JudgeService` 优先调用 Judge0 云端评测。<br>（系统）8、云端不可用且允许本地兜底时，调用 `LocalJudgeService.judge(...)`。<br>（系统）9、若包含 `taskId`，系统创建或更新 `Submission` 并保存评分结果。<br>（系统）10、系统返回 `status`、`score`、`passedCases`、`totalCases`、`timeUsedMs`、`memoryUsedKb`、`diagnosis`、`errorMessage`、`usedLocalJudge`。 |
| 扩展事件流 | （异常处理）2a、用户未登录 → 返回 `code=401`。<br>2b、代码为空 → 返回"代码不能为空"。<br>3a、任务不存在或不是 `programming` → 返回 `code=404`。<br>5a、提交语言不符合 `allowedLanguage` → 返回 `code=400`。<br>6a、测试用例为空或语言不受支持 → 返回 `IE` 状态。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/编程编程.png)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/E.jpg)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC13学生提交编程作业并获得评测结果.png)

**活动图（详细设计）：**

![](../../软件详细设计说明书/编程题评测活动图.png)

---


### UC14 教师批改、复核成绩并通知学生


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 教师批改、复核成绩并通知学生 |
| 用例编号 | UC14 |
| 参与者 | 教师、学生 |
| 主责服务 | assessment-service |
| 协作服务 | user-service |
| 触发器 | 教师打开提交记录并提交分数和评语 |
| 前置条件 | 教师拥有任务所属课程权限；提交记录存在 |
| 后置条件 | 学生可以查看最新分数、评语和批改状态 |
| 关联函数 | `TeacherController.taskDetail`<br>`TeacherController.submitGrade`<br>`TaskService.getSubmission`<br>`SubmissionMapper.grade`<br>`UserService.notify`（或 `NotificationMapper.insert`） |
| 基本事件流 | （用户）1、教师访问 `GET /teacher/task/detail/{taskId}` 查询指定任务的提交列表。<br>（系统）2、系统显示学生提交记录。<br>（用户）3、教师打开某个提交，填写分数和评语。<br>（系统）4、系统保存批改结果到 `submission`。<br>（系统）5、系统允许教师复核或覆盖自动评测分数。<br>（系统）6、系统调用 user-service 向学生发送通知。<br>（用户）7、学生收到通知，查看最新分数和评语。 |
| 扩展事件流 | （异常处理）4a、教师无权限 → 返回无权限提示。<br>4b、分数超出范围 → 返回"分数超出范围"提示。<br>4c、提交不存在 → 返回"提交不存在"。<br>6a、通知服务失败 → 返回提示"成绩已保存，但通知发送失败"，不回滚已保存成绩。 |
| 验收条件 | 批改、分数范围、复核、覆盖自动分数和通知失败测试通过。 |



**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/批改作业-1.png)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/教师批改-1.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC14教师批改、复核成绩并通知学生.png)

---


### UC15 学生查询成绩，教师查看成绩统计


| 项目 | 内容 |
| --- | --- |
| 用例名称 | 学生查询成绩，教师查看成绩统计 |
| 用例编号 | UC15 |
| 参与者 | 学生、教师 |
| 主责服务 | assessment-service |
| 协作服务 | learning-service、user-service |
| 触发器 | 学生查看个人成绩，或教师进入成绩统计页面 |
| 前置条件 | 用户已登录；学生只能查看自己的成绩，教师只能查看所授课程成绩 |
| 后置条件 | 成绩明细和统计结果与提交记录一致 |
| 关联函数 | `StudentController.scoreSummary`<br>`TeacherController.scoreStatistics`<br>`ScoreService.studentScoreSummary`<br>`ScoreService.teacherCourseStatistics` |
| 基本事件流 | （用户）1、学生访问 `GET /student/scores`，或教师访问 `GET /teacher/score/statistics`。<br>（系统）2、系统调用 `ScoreService` 查询任务和提交成绩。<br>（系统）3、学生查看个人成绩明细。<br>（系统）4、教师查看平均分、最高分、最低分和分数段统计。<br>（用户）5、用户按课程、任务或班级筛选统计结果。 |
| 扩展事件流 | （异常处理）2a、无权限访问 → 返回明确的无权限提示。<br>2b、课程不存在或任务不存在 → 返回"课程不存在"或"任务不存在"。<br>2c、统计数据为空 → 返回空列表并提示"暂无成绩数据"。 |
| 验收条件 | 学生越权查询、教师课程权限、成绩明细、统计计算和空数据测试通过。 |




**系统级顺序图（需求文档）：**

![](../../软件需求规格说明书/成绩查询-1.png)

**组件级顺序图（概要设计）：**

![](../../软件概要设计说明书/软件概要设计说明书/成绩顺序图.png)

**类级顺序图（详细设计）：**

![](../../软件详细设计说明书/UC15学生查询成绩，教师查看成绩统计.png)

---

