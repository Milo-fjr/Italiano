# AGENTS.md — AI 协作须知

给接手本项目的 AI 助手：这里是 README 之外的**关键领域知识与踩坑记录**。改动前先读，能避免重蹈覆辙。

## 一句话概述

意大利语 A2 词汇学习系统（用户为马可波罗计划生，明年 11 月出国）。Spring Boot 3 + Vue 3，本地单机运行，MySQL 8，词库约 1084 词。
五种学习模式：学习 / 测验 / 拼写 / 听写 / 错题本。
启动：双击根目录 `start.bat`（后端 8080 + 前端 5173，会开两个 cmd 窗口 + 自动开浏览器）。数据库密码在 `backend/application-local.yml`（gitignored，不入库）。

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

- 验证 API：`Invoke-RestMethod -Uri "http://localhost:8080/api/..."`（中文输出会乱码，可写临时文件用 Read 看；临时文件命名 `tmp_*.txt` / `tmp_*.js`，已 gitignore）
- 表名是 `word`（不是 words）、`word_progress`、`daily_extract`、`setting`
- **PowerShell 不支持 bash 风格 heredoc**（`$(cat <<'EOF'` 会报错）；**不支持 `&&`/`||` 语句分隔**（用 `;` 串联）；`cmd /c` 被安全策略拦截（要跑 .bat 用 `Start-Process`）；git commit 多段信息用多个 `-m` 参数
- **Git 远程已切到 GitHub**（origin → github.com/Milo-fjr/Italiano，公开，作品集用）。本机访问 GitHub 走本地代理 `127.0.0.1:6450`（AtlasCore），出网慢；超时已固化进 git 全局配置（`http.https://github.com.timeout=120`、lowSpeedLimit=0、lowSpeedTime=120），直接 `git push` 即可，无需加 `-c` 参数。若报代理连不上，先确认 6450 端口有进程监听
- start.bat 固定在启动后 5 秒开浏览器——若后端还没就绪，那个标签页会一直转圈，**刷新即可**。但先看控制台有没有报错：渲染崩溃（TypeError）也会表现为"打不开"，两者别混淆
- **往 MySQL 写含重音/中文的值**（如变位 JSON 里的 è/ò/à）：PowerShell 直接内联会乱码，用 `FROM_BASE64('<base64>')` 传值最稳——`node -e` 读 JSON 算出 `Buffer.from(str).toString('base64')`，喂 `UPDATE ... SET col=FROM_BASE64('...')`，全程纯 ASCII 无编码问题。临时脚本/输出命名 tmp_*，用完即删

## 架构地图

| 文件                                                | 职责                                               |
| ------------------------------------------------- | ------------------------------------------------ |
| `backend/.../util/ItalianGrammarUtil.java`        | **语法引擎**：例外表（例外优先）+ 规则推导，所有变位/复数/冠词/不规则标签的单一事实来源 |
| `backend/.../service/ExtraFormService.java`       | **拼写/听写共用判分支撑**：附加题判定 + 输入归一化（重音/大小写/空格容错），两模式永远同一口径 |
| `backend/.../service/ExtractService.java`         | **学习模式**批次抽取：完成次数流转（零遍随机 > 完成次数升序+冷却）          |
| `backend/.../service/QuizService.java`            | **测验模式**：SRS 到期词查询（next_review_at <= 今天，随机排序）            |
| `backend/.../service/SpellService.java`          | **拼写模式**：中→意产出复习，独立 spell 盒子 + 防撞五条件队列                  |
| `backend/.../service/DictService.java`            | **听写模式**：听音→意拼写，两段式（先选释义后拼写），独立 dict 盒子 + 防撞队列      |
| `backend/.../service/WordService.java`           | 完成/撤销/编辑/测验答题，SRS 升盒降盒逻辑                              |
| `backend/src/main/resources/data/vocab_data.json` | 1084 词导入源（首启导入用）                                  |
| `frontend/src/views/TodayView.vue`                | **学习模式**卡片页（背新词：标记完成/撤销/换一批）                    |
| `frontend/src/views/QuizView.vue`                 | **测验模式**卡片页（SRS 到期：翻卡核对、认识/不认识）                 |
| `frontend/src/views/SpellView.vue`                | **拼写模式**单卡答题页（中→意拼写、不规则附加形式、自反动词提示、结果对照）    |
| `frontend/src/views/DictView.vue`                | **听写模式**两段式答题页（听音选释义 → 听音拼写单词）                   |
| `frontend/src/views/NotebookView.vue`             | **错题本**：测验/拼写/听写答错自动进本，学会移出                      |
| `frontend/src/components/WordDetailDialog.vue`    | 详情弹窗（变位表、单复数、朗读按钮）                               |
| `frontend/src/utils/tts.js`                       | Web Speech API 朗读（调 Windows 系统意语语音包 Elsa）        |

## 领域逻辑陷阱（改前必读）

1. **语法引擎三层优先级**：例外表（代码内硬编码）> 规则推导 > 数据库手动编辑值（手动值最高，永不被覆盖）。改例外表只影响"无手动值"的词。
2. **`extract_count`** **是完成次数，不是抽取次数**。仅被抽进批次不计数，点"标记完成"才 +1，撤销 -1（可到 0）。
3. **双数据源**：`vocab_data.json` 是导入源，DB 是运行数据。改 JSON **不会**同步已导入的 DB 行，反向同步用「设置 → 词库备份」按钮（POST /api/export，DB 全量写回 JSON 含语法字段/例句；导入端 JSON 值优先，重灌为全保真恢复）。**删词要两处同步**：DB 先删 `daily_extract` → `word_progress` → `word`（有外键依赖顺序），再删 JSON 对应条目并用 node 验证 JSON 合法。
4. **irregularTag 标签哲学：红标 = 必须额外记，规则推导可得的一律不标**（标签通胀会让用户不再看红标）。已删除：「音变」（动词 -care/-gare/-iare、名词 -ca/-ga/-cia/-gia，拼写有规律）、「复数不变」（外来词/缩写词/月份，性质即规则）。保留：时态不规则（现在/近过去/未完成/将来）、不规则复数、阴阳性特殊、性别需记（-e 结尾）、冠词式变化（bello 型）、形容词不规则变化。名词多标签顿号叠加；月份不标性别。
   注意：附加题判定已不依赖被删标签——名词复数附加题（banca→banche 类拼写陷阱）由 `ItalianGrammarUtil.isPluralTrapNoun` 词形判断兜底，改标签逻辑时别把这条断了。
5. **五套独立体系**：学习模式按 extract_count 流转抽词（零遍随机覆盖全库 → 完成次数升序循环，不看盒子）；测验只认 box/next_review_at（**不筛 box**，答错归 0 的词明天到期也回来）；拼写只认 spell_box/spell_next_review_at；听写只认 dict_box/dict_next_review_at；错题本只认 in_notebook。判分规则：拼写/听写全对升盒、有错归 0 明天回，服务端归一化容错（大小写/重音/空格）。交汇点：学习「标记完成」= 次数 +1 且盒 +1；测验「认识」盒 +1 不动次数、「不认识」盒归 0；**拼写答题只动 spell 字段、听写只动 dict 字段**。
6. **防撞规则**（同一词一天只出现在一种产出模式）：拼写/听写队列排除——当日认识测验欠账的词（测验优先级更高）、当日测验答过的词（last_quiz_at）、当日学习完成的词（completed_at）、当日拼写/听写答过的词。**从未拼写/听写的词（对应 next_review_at 为 NULL）视为到期**，由防撞规则自然节流。撤销学习到 extract_count=0 会把词挡在产出池外（资格门槛 extract_count > 0）。
7. **题目 DTO 防泄题设计**：拼写模式的题目接口**不返回意语单词**（word 字段不存在，只有 wordId/meaning/pos/category/extraLabel），答案只在判分结果里返回；听写模式的题目**含 word**（TTS 要播放，听本身就是题面）。前端写 `current.xxx` 前先确认 DTO 里真有这个字段——2026-09-09 就是读了不存在的 `current.word` 导致渲染崩溃（见事故记录）。
8. **自动朗读**：五模式统一"标记过了就读一遍"——学习「标记完成」、错题本「学会了」、测验认识/不认识、拼写提交/不会、听写判分落库后调 `speakItalian(该词)`。批量操作（全部完成/全部学会）不播，避免音频叠加。
9. MyBatis-Plus 全局 `FieldStrategy.ALWAYS`——此前为 IGNORED 时 null 字段不更新，导致撤销操作清不掉 `completed_at`，留下过脏时间戳。
10. **释义边界化**：中文一词多义会造成拼写歧义，释义要拆开各归一词（sera=傍晚；晚上 / notte=夜里，"晚上"只归前者）。用户提出释义质疑时先查库对账再动手。
11. **双助动词有两处硬编码，必须同步改**：`ItalianGrammarUtil.DUAL_AUX_VERBS`（规则引擎）与 `ImportService.fixDualAuxV3` 内的动词列表（启动迁移）各自维护一份"双助动词"清单，改一处忘改另一处会导致启动迁移每次重复执行并打误导日志。camminare/nuotare 是"动作方式"动词（不表去向），只用 avere（ho camminato / ho nuotato，无 essere 形式、分词不变性数），永远别加回这两份清单；误加的回退逻辑在 `fixDualAuxV6`（幂等）。追加到双助动词清单前先确认该词真的是"avere 及物 / essere 不及物"两义都对（如 correre/vivere/volare），拿不准查权威词典。

## 历史事故记录（血泪教训）

### lenzuolo 事件（2026-08，最重要）

外部 AI（豆包）断言 `lenzuolo`（床单，阳性）复数 `le lenzuola` 是错的、另有阴性词 `la lenzuola`=床罩。**这是幻觉**。实际（Accademia della Crusca 权威确认）：`i lenzuoli`（逐张）/ `le lenzuola`（成对）双重复数都正确，后者日常更常用；"la lenzuola 床罩"是不存在的词（床罩是 copriletto）。
**教训：任何 AI 给出的语法/数据断言，改库前必须先查 Treccani / Accademia della Crusca / 权威词典验证。** 数据曾经是对的，被错误"修复"过一次又回滚。

### 渲染崩溃 + 误诊（2026-09-09，AI 责任事故）

加自反动词提示时读了 `current.value.word`，但拼写题目 DTO 防泄题不含 word 字段 → 进拼写页即 TypeError，整站白屏转圈。更糟的是用户反馈"打不开"时，AI 只测了首页（正常的）就下结论"刷新一下就好"，测错了页面、给错了诊断，直到用户贴出控制台报错才定位。
**教训三条：①改哪个页面就实测哪个页面，不能拿别的页面正常当依据；②用没把握的字段先查 DTO/后端代码，别凭感觉写；③用户报障先复现到他说的那个场景，看控制台，别急着下结论。**

### 浏览器实测污染学习数据（2026-09-09，AI 责任事故）

验证防手滑功能时让浏览器 agent 实测拼写模式，它一路答题翻队列——**36 个词被作答（31 对 6 错），6 个词被误塞进错题本**，SRS 盒子全部错位。三个叠加失误：①测前没做数据库快照；②agent 自述"答对 18 个"严重失真（实际 36 个含 6 错），差点按它的口径去回滚；③测试用例设计成"连续答题翻队列"本身就是污染源。
**教训：①会写库的浏览器实测，测前必须先快照**（`mysqldump italian_vocab word_progress > tmp_xxx.sql` 或 SELECT 导出），测后还原并逐字段核对；能不答题就不答题（看渲染/控制台即可），必须答题时限定 1-2 个词并记录词 id。②子 agent 的自述不可信，回滚范围以 binlog/DB 取证为准。③本机 MySQL 开 binlog（ROW 格式），取证命令：`D:\Dev\MySQL\bin\mysqlbinlog.exe --no-defaults --base64-output=decode-rows -v D:\Dev\MySQL\data\binlog.0000XX`，解出的 `### WHERE` 镜像 = 改前值，配合事件头时间戳可精确圈定污染窗口并逐行还原（本次 36 行全部精确复原）。④mysqlbinlog 必须加 `--no-defaults`（my.ini 的 default-character-set 会让它报错）；PowerShell 不支持 `<` 输入重定向，喂 SQL 用 `Get-Content x.sql -Raw | mysql ...`。

### 端口占用（两次，惯犯）

调试时 AI 在后台启动的后端未清理，用户双击 `start.bat` 报 `Port 8080 already in use`。诊断：`netstat -ano | findstr ":8080"` 找 PID，`Stop-Process -Id <pid> -Force`。**调试用完的后台服务必须归还：StopCommand 停后台命令后，用 netstat 确认端口真释放了再走；没释放就按 PID 补刀。收工前必查。**

### SRS 上线前的历史数据

SRS 部署前完成的 11 个词曾滞留 box 0，已回填 box 1（`UPDATE ... SET box=1, next_review_at=DATE(completed_at)+INTERVAL 1 DAY`）。注意判据用 `extract_count > 0` 而非 `completed_at IS NOT NULL`（后者含撤销遗留的脏时间戳）。

### GitHub 迁移：连接器不能建仓 + 国内直连不通（2026-09-12）

- **GitHub 连接器（TRAE 插件 MCP）无法建仓**：`create_repository` 持续 403 `Resource not accessible by integration`——GitHub App 签发的 token 权限里没有"建仓"这一项，重新授权/重启 TRAE 都补不上（Gitee 连接器默认就有建仓权限，所以 Gitee 一直正常）。**建仓只能网页手动**（github.com/new，Public、不勾 README）。
- **国内直连 github.com 不通**（`Connection was reset` / 连不上 443）。**遇到 github.com 连接/push 失败先检测本机有没有可用代理，别信写死的端口**：①查系统代理设置 `HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings` 的 ProxyServer；②或 `Get-NetTCPConnection -State Listen` 扫常见代理端口；③本机加速器（AtlasCore）的本地端口可能变化，以实际检测为准，测通后配 `git config --global http.https://github.com.proxy http://<host>:<port>`（只对 github.com 生效，Gitee 仍直连）。代理客户端没开或端口失效时 push 会失败，按此流程重配。
- **GCM 曾用错账号**：本地存有旧账号 fjr101 的 GitHub 凭据，push 到 Milo-fjr 的仓库被 403 拒。清凭据命令：`"protocol=https`nhost=github.com`n" | git credential reject`，然后重推，GCM 弹浏览器以 **Milo-fjr** 登录。
- 现状：origin = https://github.com/Milo-fjr/Italiano.git（master 已推送，公开），Milo-fjr 凭据已由 GCM 保存。

## AI 工作守则

0. **踩坑即沉淀**：每次操作后，凡是踩过的坑、遇到的容易再次犯错的问题、发现的领域新知识，都要**主动、及时**补进本文件对应章节（事故记录/领域逻辑陷阱/高频操作）——不要等用户提醒。本文件是接手的 AI 避坑的唯一文档，越完整越少重蹈覆辙。
1. **改完必实测，测改动的那个页面本身**——看渲染、看功能、看控制台有无报错，再提交推送。这是用户的固定工作流（改完 → 浏览器实测 → commit + push），不能跳。
2. **浏览器实测不污染数据**：会写库的实测先快照、测后还原核对；能只看渲染就不答题；必须答题就限 1-2 个词并记录 id。子 agent 的事后汇报必须用 DB/binlog 验证。
3. **不拿别的页面的正常当依据**去否掉用户报告的故障。
4. **写字段前核对数据来源**：DTO/接口/表结构没把握就读代码确认，不凭记忆和感觉。
5. **AI 的语法断言必须查权威词典验证后才能改库**（lenzuolo 事件）。
6. **收工归还资源**：后台服务、临时进程、端口、临时文件，全部清干净（netstat 验证）再结束。**调试用的临时脚本/输出（tmp_*）用完即删**——它们是一次性工具，不留着占目录、污染 git status；`.gitignore` 已有 `tmp_*.txt`/`tmp_*.js` 规则，但删除比 ignore 更彻底。
7. 用户对数据不一致的质疑**往往是对的**——先认真查库对账，不要急着解释。

## 用户协作偏好

- 中文交流
- 每次改动：改完 → 浏览器实测 → git commit + push（origin 已改指 GitHub `Milo-fjr/Italiano`，Gitee 停推；GitHub 推送需本机代理开着，见事故记录）
- 用户学习目标：每天 10-15 词精背（含变位变形），A2 全覆盖后加 B1；明年 6 月毕业、11 月出发意大利
- 词汇取舍标准是**用户的认知实用性**：中文里都不知道是什么的东西（如西葫芦 zucchina、甜椒 peperone）直接删；中国常见的（茄子、豆子）保留——判断权在用户，AI 别拿"意大利高频"反驳
- 技术审美：YAGNI，最小实现，反对过度设计；红标/UI 提示同理——什么都强调等于什么都不强调
