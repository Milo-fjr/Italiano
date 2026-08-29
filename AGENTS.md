# AGENTS.md — AI 协作须知

给接手本项目的 AI 助手：这里是 README 之外的**关键领域知识与踩坑记录**。改动前先读，能避免重蹈覆辙。

## 一句话概述

意大利语 A2 词汇学习系统（用户为马可波罗计划生，明年 11 月出国）。Spring Boot 3 + Vue 3，本地单机运行，MySQL 8。
启动：双击根目录 `start.bat`（后端 8080 + 前端 5173）。数据库密码在 `backend/application-local.yml`（gitignored，不入库）。

## 高频操作

```bash
# 后端启动（改了 Java 代码必须重启才生效，无热重载）
cd backend && mvn spring-boot:run

# 编译验证（不动服务）
cd backend && mvn compile -q

# 前端（Vite 热更新，改 .vue 不用重启）
cd frontend && npm run dev

# 直连数据库（PowerShell）
mysql -u root -p<密码> italian_vocab -e "SQL..."
```

- 验证 API：`Invoke-RestMethod -Uri "http://localhost:8080/api/..."`（中文输出会乱码，可写临时文件用 Read 看）
- 表名是 `word`（不是 words）、`word_progress`、`daily_extract`、`setting`
- **PowerShell 不支持 bash 风格 heredoc**（`$(cat <<'EOF'` 会报错），git commit 多段信息用多个 `-m` 参数

## 架构地图

| 文件                                                | 职责                                               |
| ------------------------------------------------- | ------------------------------------------------ |
| `backend/.../util/ItalianGrammarUtil.java`        | **语法引擎**：例外表（例外优先）+ 规则推导，所有变位/复数/冠词/不规则标签的单一事实来源 |
| `backend/.../service/ExtractService.java`         | **学习模式**批次抽取：完成次数流转（零遍随机 > 完成次数升序+冷却）          |
| `backend/.../service/QuizService.java`            | **测验模式**：SRS 到期词查询（next_review_at <= 今天，随机排序）            |
| `backend/.../service/WordService.java`            | 完成/撤销/编辑/测验答题，SRS 升盒降盒逻辑                              |
| `backend/src/main/resources/data/vocab_data.json` | 1087 词导入源（首启导入用）                                 |
| `frontend/src/views/TodayView.vue`                | **学习模式**卡片页（背新词：标记完成/撤销/换一批）                    |
| `frontend/src/views/QuizView.vue`                 | **测验模式**卡片页（SRS 到期：翻卡核对、认识/不认识）                 |
| `frontend/src/components/WordDetailDialog.vue`    | 详情弹窗（变位表、单复数、朗读按钮）                               |
| `frontend/src/utils/tts.js`                       | Web Speech API 朗读（调 Windows 系统意语语音包 Elsa）        |

## 领域逻辑陷阱（改前必读）

1. **语法引擎三层优先级**：例外表（代码内硬编码）> 规则推导 > 数据库手动编辑值（手动值最高，永不被覆盖）。改例外表只影响"无手动值"的词。
2. **`extract_count`** **是完成次数，不是抽取次数**。仅被抽进批次不计数，点"标记完成"才 +1，撤销 -1（可到 0）。
3. **双数据源**：`vocab_data.json` 是导入源，DB 是运行数据。改 JSON **不会**同步已导入的 DB 行，反向同步用「设置 → 词库备份」按钮（POST /api/export，DB 全量写回 JSON 含语法字段/例句；导入端 JSON 值优先，重灌为全保真恢复）。
4. **irregularTag 标签系统**（卡片红色标签）：名词多标签叠加（顿号连接），如 foto「复数不变、阴阳性特殊」；月份排除在「性别需记」外（统一阳性无记忆价值）。
5. **学习/测验双体系（两套独立）**：学习模式按 extract_count 流转抽词（零遍随机覆盖全库 → 完成次数升序循环，不看盒子）；测验模式只认盒子——next_review_at <= 今天即测（**不筛 box**，答错归 0 的词明天到期也能回来）。唯一交汇点：学习「标记完成」= 次数 +1 且盒 +1（词次日进测验）；测验「认识」盒 +1 **不动次数**（WordService.reviewKnow）、「不认识」盒归 0 明天到期。到期复习词**不进批次**；统计页到期数口径 = next_review_at <= 今天。
6. MyBatis-Plus 全局 `FieldStrategy.ALWAYS`——此前为 IGNORED 时 null 字段不更新，导致撤销操作清不掉 `completed_at`，留下过脏时间戳。

## 历史事故记录（血泪教训）

### lenzuolo 事件（2026-08，最重要）

外部 AI（豆包）断言 `lenzuolo`（床单，阳性）复数 `le lenzuola` 是错的、另有阴性词 `la lenzuola`=床罩。**这是幻觉**。实际（Accademia della Crusca 权威确认）：`i lenzuoli`（逐张）/ `le lenzuola`（成对）双重复数都正确，后者日常更常用；"la lenzuola 床罩"是不存在的词（床罩是 copriletto）。
**教训：任何 AI 给出的语法/数据断言，改库前必须先查 Treccani / Accademia della Crusca / 权威词典验证。** 数据曾经是对的，被错误"修复"过一次又回滚。

### SRS 上线前的历史数据

SRS 部署前完成的 11 个词曾滞留 box 0，已回填 box 1（`UPDATE ... SET box=1, next_review_at=DATE(completed_at)+INTERVAL 1 DAY`）。注意判据用 `extract_count > 0` 而非 `completed_at IS NOT NULL`（后者含撤销遗留的脏时间戳）。

### 端口占用

调试时 AI 在后台启动的后端未清理，用户双击 `start.bat` 报 `Port 8080 already in use` 但网页仍可用（旧实例在服务）。诊断：`netstat -ano | findstr ":8080"` 找 PID，`Stop-Process -Id <pid> -Force`。**调试用完的后台服务必须归还。**

## 用户协作偏好

- 中文交流
- 每次改动：改完 → 浏览器实测 → git commit + push 到 Gitee（习惯性推送，直接推）
- 用户学习目标：每天 10-15 词精背（含变位变形），A2 全覆盖后加 B1；明年 6 月毕业、11 月出发意大利
- 用户会对数据不一致提出质疑且**往往是对的**（lenzuolo 案除外）——认真查库对账，不要敷衍
- 技术审美：YAGNI，最小实现，反对过度设计

