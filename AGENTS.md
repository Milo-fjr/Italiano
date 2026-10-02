# AGENTS.md — AI 协作须知

给接手本项目的 AI 助手：README 之外的**关键领域知识与踩坑规则**，改动前先读。
**本文件纪律：规则为主干，案例只留一行防回归锚点，不复述事故经过与日期流水**（完整历史在 git，需要细节用 `git log`/`git show` 回查）。

## 一句话概述

意大利语 A2 词汇学习系统（用户为马可波罗计划生，明年 11 月出发）。Spring Boot 3 + Vue 3，本地单机，MySQL 8，词库 1083 词。六种学习模式：学习 / 测验 / 拼写 / 听写 / 加练（认识/拼写/听写/不规则变化四题型）/ 错题本。
启动：双击根目录 `start.bat`（后端 8080 + 前端 5173，5 秒后自动开浏览器——未就绪转圈刷新即可，但先看控制台有无渲染报错，别和真崩溃混淆）。数据库密码在 `backend/application-local.yml`（gitignored，不入库）。

## 高频操作

```bash
# 后端启动（改了 Java 必须重启，无热重载）
cd backend && mvn spring-boot:run

# 编译验证（不动服务）
cd backend && mvn compile -q

# 单元测试（纯逻辑回归网：语法引擎/判分归一化，秒级、不起 Spring 不连 DB；改语法引擎/例外表/判分前必跑）
cd backend && mvn test

# 前端（Vite 热更新，改 .vue 不用重启）
cd frontend && npm run dev

# 直连数据库（PowerShell）
mysql -u root -p<密码> italian_vocab -e "SQL..."
```

- **含中文的 API 验证用 node fetch（tmp_*.js），别用 Invoke-RestMethod**（后者按 Latin-1 解码 UTF-8，中文乱码判分必 false）；tmp_* 已 gitignore，用完即删
- **API 响应是 `{code, message, data}` 包装**：前端 axios 拦截器已自动拆包；裸 node fetch 必须取 `j.data.xxx`，否则全是 undefined，别误判接口坏了
- **PowerShell `>` 重定向产出 UTF-16 带 BOM**，喂 node 前必剥 BOM 并打印行数自证；**diff 为空这种"符合预期的坏结果"，先证明解析没坏再相信**
- 表名：`word`（不是 words）、`word_progress`、`daily_extract`、`setting`
- PowerShell：不支持 bash heredoc、不支持 `&&`/`||`（用 `;` 串联）、`cmd /c` 被安全策略拦截（跑 .bat 用 `Start-Process`）；git commit 多段信息用多个 `-m`
- **GitHub（origin = Milo-fjr/Italiano，公开）**：走本地代理 `127.0.0.1:6450`（端口可能变，失效先查系统代理设置/扫监听端口再配）；push 超时已固化全局配置。push 失败先看报错是否代理连不上，可试 `git -c http.https://github.com.proxy= push` 直连
- **往 MySQL 写重音/中文**（如变位 JSON 的 è/ò）：PowerShell 内联会乱码，用 `FROM_BASE64('<base64>')` 传值（node 算 base64，全程纯 ASCII）
- 排查"某行为/标记何时被改"：`git log -S "关键词" --oneline -- <文件>`（pickaxe）是第一步
- **word_progress.status 语义**：0=从未抽取、1=已抽取未完成（extract_count=0）、2=已完成。**测试残留排查**：`SELECT last_extracted_at, COUNT(*) FROM word_progress WHERE status=1 AND extract_count=0 AND completed_at IS NULL GROUP BY last_extracted_at`——历史日期分组 = 测试抽取没归还（还原：`SET status=0, last_extracted_at=NULL`），当天分组 = 用户真实批次（勿动）
- **白屏但标签页标题正常 + 控制台仅 `[Vue Router warn] ... : {}`**：HTTP 缓存投毒（node_modules/.vite/deps 的 immutable 缓存头在 Vite 未就绪时缓存了失败响应），**Ctrl+F5 即愈，别改代码**

## 架构地图

| 文件 | 职责 |
| --- | --- |
| `backend/.../util/ItalianGrammarUtil.java` | **语法引擎**：例外表 + 规则推导，变位/复数/冠词/不规则标签的单一事实来源 |
| `backend/.../service/ExtraFormService.java` | 各产出型模式共用归一化与中文释义判分（静态方法，单测锁定） |
| `backend/src/test/java/.../` | 单元测试回归网：语法引擎已查证结论 + 判分归一化 + 多形式判分 |
| `backend/.../service/ExtractService.java` | 学习模式批次抽取（零遍随机 → 完成次数升序 + 冷却） |
| `backend/.../service/QuizService.java` | 测验模式：SRS 到期词查询 |
| `backend/.../service/SpellService.java` | 拼写模式：中→意产出复习，独立 spell 盒子 + 防撞队列 |
| `backend/.../service/DictService.java` | 听写模式：两段式，独立 dict 盒子 |
| `backend/.../service/PracticeService.java` | 加练模式：四题型，零 SRS（不规则专考见陷阱 14） |
| `backend/.../service/WordService.java` | 完成/撤销/编辑/答题，SRS 升降盒 |
| `backend/.../service/NotebookService.java` | 错题本双本制：词本 in_notebook / 变位本 in_conj_notebook |
| `backend/src/main/resources/data/vocab_data.json` | 词库导入源（首启导入用） |
| `frontend/src/views/TodayView.vue` | 学习模式卡片页（背新词） |
| `frontend/src/views/QuizView.vue` | 测验模式卡片页（SRS 到期） |
| `frontend/src/views/SpellView.vue` | 拼写模式单卡答题页 |
| `frontend/src/views/DictView.vue` | 听写模式两段式答题页 |
| `frontend/src/views/PracticeView.vue` | 加练模式四题型页（irregular 题卡见陷阱 14） |
| `frontend/src/views/NotebookView.vue` | 错题本双本 tab：卡片/详情/学会了/放回去/全部学会，按 book 参数复用 |
| `frontend/src/components/WordDetailDialog.vue` | 详情弹窗（变位表、单复数、朗读） |
| `frontend/src/utils/tts.js` | Web Speech API 朗读（Windows 意语语音包 Elsa） |

## 领域逻辑陷阱（改前必读）

1. **语法引擎三层优先级**：例外表（代码硬编码）> 规则推导 > DB 手动编辑值（最高，永不被覆盖）。改例外表只影响无手动值的词。
2. **`extract_count` 是完成次数不是抽取次数**：标记完成 +1、撤销 -1（可到 0），仅被抽进批次不计数。
3. **双数据源**：JSON 是导入源、DB 是运行数据，改 JSON 不同步已导入行；DB→JSON 用「设置 → 词库备份」（全量含语法字段/例句，导入端 JSON 值优先）。**删词两处同步**：DB 按外键顺序 `daily_extract → word_progress → word` 删，再删 JSON 条目并用 node 验证 JSON 合法。
4. **红标哲学：红标 = 必须额外记，规则可推导的一律不标**（标签通胀 = 用户不再看红标）。已删：音变（-care/-gare/-iare、-ca/-ga/-cia/-gia）、复数不变、-isc 型红标（-isc 是 -ire 变位的规则子模式，regular 口径）。-ire 动词改标「-isc 型/普通型」模式 tag（归属原形推不出，需连 io 形式记），真不规则 -ire 不叠加；前端经 `utils/irregular.js` 的 `irregularTagType()` 分级 danger 红/primary 浅绿。**IRREGULAR_PP 只存真不规则**：后缀恰合规则的（avuto、stato(stare)、dato）已移除；perdere/vedere 双形式分词取常用形（见待决备忘）。
5. **六套体系字段独立**：学习按 extract_count 流转（零遍随机 → 完成次数升序+冷却，不看盒子）；测验只认 box/next_review_at（**不筛 box**，答错归 0 明天到期即回）；拼写只认 spell_box/spell_next_review_at；听写只认 dict_box/dict_next_review_at；错题本双本：in_notebook（测验不认识/拼写/听写/加练 quiz-spell-dict 答错进）/ in_conj_notebook（加练 irregular 答错进），独立可并存，**进本不记来源——拆分前旧条目不可按词性批量搬（venire 等是测验"不认识"进的），只搬用户点名的词**。判分：全对升盒、有错归 0，服务端归一化容错（大小写/重音/空格/撇号——弯引号 ‘’‘´` 一律归一为直撇号，**替换必须在 NFD 之前**，´ 会被 NFD 分解成空格+组合符）。**每日配额按自然日计**：setting 表 quiz/spell/dict_daily_limit 三列（0=不限），今日已答 = `last_X_at=今天` 的行数，答满返回空队列（quotaReached）、到期词保持到期明天继续，未答满按最欠账优先（到期日 ASC + RAND，NULL 池垫底）——**绝不能按单次抽题 LIMIT 计**（挂在页面加载上，切走再切回会多抽）。导航红点 = clampToQuota 后"今天还能做几题"；**错题本红点不同口径** = 在册数（账本语义，清完才灭），NotebookView 页签常显双本计数，进本出本后调 statsStore.load() 即时刷新。交汇点：学习「标记完成」= 次数+1 且盒+1；测验「认识」盒+1 不动次数、「不认识」盒归 0；**拼写答题只动 spell 字段、听写只动 dict 字段**。
6. **防撞规则**（同一词一天只进一种产出模式）：拼写/听写队列排除——当日测验欠账词、当日测验答过（last_quiz_at）、当日学习完成（completed_at）、当日拼写/听写答过；NULL next_review_at 视为到期由防撞自然节流；撤销到 extract_count=0 挡在产出池外（资格门槛 >0）。
7. **DTO 防泄题**：拼写题目接口**不含意语单词**（答案只在判分结果里）；听写/加练 irregular 含 word（题面即形式）；irregular 人称点的 label 含答案人称 → **UI 不渲染**（防泄题口径演进：DTO 不含答案，或含但 UI 不渲染）。前端写 `current.xxx` 前先确认 DTO 真有该字段（曾读不存在的 current.word 白屏，见事故记录）。
8. **自动朗读**：五模式统一"标记过了就读一遍"（学习标记完成、错题本学会了、测验认识/不认识、拼写提交/不会、听写判分落库）；批量操作不播（防音频叠加）。
9. **MyBatis-Plus 全局 FieldStrategy.ALWAYS**：IGNORED 会让 null 更新失效（曾致撤销清不掉 completed_at，留脏时间戳）。
10. **释义边界化**：一词多义造成拼写提示歧义 → 释义拆开各归一词（sera=傍晚；晚上 / notte=夜里，"晚上"只归前者）。措辞由用户定，AI 提议被否属常态；用户质疑先查库对账再动手。已裁决：tranquillo=平静的 / silenzioso=安静的（心理/环境）；spedire=邮寄 / mandare=发送；派遣；allegro/felice/contento 按性格/深沉/当下切；**denaro 已整词删除（soldi 独占日常"钱"；ItalianGrammarUtil 不变形名词单里的残留属有意保留，别"修复"）**。
11. **双助动词两处硬编码必须同步改**：`ItalianGrammarUtil.DUAL_AUX_VERBS` 与 `ImportService.fixDualAuxV3` 内的清单（漏改 = 启动迁移每次重复执行打误导日志）；误加的幂等回退在 fixDualAuxV6。camminare/nuotare 是动作方式动词，只用 avere，别加回；追加前确认该词真是 avere/essere 两义都对（如 correre/vivere/volare），拿不准查权威词典。
12. **错题本排序稳定**（按 word_progress.id 升序 = 进本先后，别加回 shuffle）；测验 SRS 随机防位置记忆——两者别混淆。
13. **加练模式零 SRS**：从 extract_count>0 随机抽，答错只进错题本，中途退出零持久化；听写释义选错立即判错（预检接口只判断、不落库、不泄答案）。**新 UI 必须对齐同类型现有模式的交互惯例**；`frontend/src/styles/answer-card.css` 是答题卡样式单一来源（拼写/听写/加练三视图 @import 引入；新增题型引它、改样式改它；本地同名规则写在后面即覆盖；仅布局语义真不同的类留各视图，别硬抽）。浏览器验证 CSS 用 getComputedStyle 查具体值（快照证明不了样式表挂载）；Vite dev 代理在后端未启动时把 API 报成 500，别误诊为后端 bug。
14. **不规则专考（加练第 4 题型 irregular）**：拼写/听写/加练的附加题已全部撤下，不规则统一由本题型专考。考点引擎枚举：现在/将来时逐人称与规则推导比对（相同不考）+ 裸分词（念裸分词，避 ho/sono 性数歧义）+ 名词不规则复数（"/"双形式任答一）+ bello 型（固定语境推导唯一定语形式）+ -co/-go 软音 + 不变形容词复数=原词。变位考点三段式（听形式→选释义→选人称时态→拼写，同词性优先取干扰项，任一关错整题判错）；pp/名词/形容词考点无人称关。规则形式 = 纯听辨点（选对人称即过、不拼）；同形歧义降级不出人称关；**答案现场推导、题目 DTO 不含答案**（请求回传考点 + listenOnly/personChoice，服务端无状态判分）；DB 手动值优先，引擎推导兜底；全规则动词也入队（每词 1 听辨点）。
15. **拼写陷阱复数判则**：-ca/-ga → 加 h（banca→banche，锁硬音）；-cia/-gia → 看紧邻 c/g 前一字母（辅音前去 i / 元音后保 i，实现 = `buildPlural` 的 `charAt(len-4)`）；-co/-go：piana 加 h / sdrucciola 不加 h / amico→amici 是著名例外（保留红标）。**IRREGULAR_PLURAL 表照存但红标判定解耦**（复数以 -chi/-ghi 结尾视为规则加 h 不标红；改表别动红标判定）；软音型（amico/medico/stomaco/traffico 等）与强不规则（braccio→braccia，复数性别漂移）标红。数据疑点必须查权威词典修订后改库（succo 先例：piana 加 h，succi 系三处误写）。
16. **误触改判 + 复盘拦截**：打字题判错后结果区有「手滑了，改判对」——服务端**先快照后变更**、响应带 boxBefore/notebookBefore 原样回传（`/typo-fix`，零新表零字段），盒子按答错前等级+1、错题本还原到答错前、last_X_at 保持今天；「不会」主动放弃不提供改判、听写选错释义不提供、加练 irregular 拼写关暂无（见待决备忘）。**复盘拦截**：判错后（「不会」/答对不拦）首次回车只拦截不切题、第二次放行，防惯性回车跳过对照区；提醒 = 结果区内嵌琥珀脉冲横幅 `.review-hint`（answer-card.css 三视图统一），**不用 toast**（不醒目）；`reviewReminded` 重置点必须齐全（next/fixTypo/load/practice 的 switchType 清空块），漏一处 = 下个错词首次回车被静默放行。

## 待决备忘（TODO）

- **未完成时（imperfetto）未考察**：用户学到后进加练 irregular 考点枚举，**同步加 TENSE_LABELS 选项池 + 听辨点候选池，两处一起改**（IRREGULAR_IMPERFETTO 现有 essere/fare/dire/bere 四词）。
- **拼写陷阱类复数（-ca/-ga/-cia/-gia）当前无任何模式考察**（附加题撤下后的空档），是否纳入 irregular 题型待用户发话（`isPluralTrapNoun` 纯语法判定保留，可直接复用）。
- **perdere/vedere 双形式分词**：pp 考点答另一合法形式（perso/perduto、visto/veduto）会判错，是否支持 "/" 双形式待用户发话。
- **加练 irregular 拼写关暂无误触改判**（用户拍板范围是四场景；想加随时说）。

## 历史事故记录（一行版）

- **AI 语法断言必须查权威词典验证后才能改库**——外部 AI 曾幻觉"le lenzuola 是错的"，把正确数据错误"修复"过一次（lenzuolo 事件）。
- **改哪个页面测哪个页面；没把握的字段先查 DTO；用户报障先复现到他说的场景看控制台**——曾读不存在的 `current.word` 致整站白屏，又只测首页误诊"刷新就好"。
- **会写库的浏览器实测先快照后还原核对；子 agent 自述不可信，回滚范围以 DB/binlog 取证为准**——曾 36 词被污染靠 binlog 精确复原（`D:\Dev\MySQL\bin\mysqlbinlog.exe --no-defaults --base64-output=decode-rows -v <文件>`；PowerShell 喂 SQL 用 `Get-Content x.sql -Raw | mysql`）。
- **后台服务收工必归还**：停掉后 netstat 确认端口释放，没释放按 PID 补刀（8080 惯犯）。
- **SRS 数据回填判据用 `extract_count>0`**，别用 `completed_at IS NOT NULL`（含撤销遗留脏时间戳）。
- **用户报"元素/行为消失"先 `git log -S` 拉时间线 + 确认用户看的页面 + last_X_at 时间戳还原出处**；用户记忆通常有实物对应，只是可能指向另一元素——别急着说"你记错了"。
- **GitHub 连接器（MCP）不能建仓**（403，只能网页手动建）；GCM 曾用旧账号 403 → 清凭据重登 Milo-fjr。

## AI 工作守则

0. **踩坑即沉淀，按本文件纪律沉淀**：主动、及时把坑改写为"规则 + 机制 + 至多一行锚点"补进对应章节，不复述事故经过与日期流水——文件越精，越会被真正读完和遵守。
1. **改完必实测，测改动的那个页面本身**（渲染/功能/控制台），再 commit + push。
2. **浏览器实测不污染数据**：写库前快照、测后还原核对；能只看渲染就不答题；必须答题限 1-2 个词并记录 id。
3. **不拿别的页面的正常当依据**去否掉用户报告的故障。
4. **写字段前核对数据来源**：DTO/接口/表结构没把握就读代码确认，不凭感觉。
5. **AI 的语法断言查权威词典验证后才能改库**。
6. **收工归还资源**：后台服务/端口（netstat 验证）/临时文件（tmp_* 用完即删）全部清干净。
7. **用户对数据不一致的质疑往往是对的**——先查库对账，不要急着解释。

## 用户协作偏好

- 中文交流
- 每次改动：改完 → 浏览器实测 → git commit + push（origin = GitHub Milo-fjr/Italiano，Gitee 停推；代理见高频操作）
- 学习目标：每天 10-15 词精背，A2 全覆盖后加 B1；明年 6 月毕业、11 月出发意大利
- 词汇取舍标准 = **用户的认知实用性**（判断权在用户，别拿"意大利高频"反驳）
- 技术审美：YAGNI，最小实现；红标/UI 提示同理——什么都强调等于什么都不强调
