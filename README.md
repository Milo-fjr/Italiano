# 意大利语 A2 词汇学习系统（Web 版）

供马可波罗计划生意大利语 A2 学习使用的本地词汇学习系统。

核心流程：每天自动抽取一批单词（默认 35 个，可配置）→ 学习后手动**标记完成**（完成后抽取次数才 +1）→ 抽取算法优先覆盖**从未学过 / 抽取次数少**的单词，尽量覆盖全部 1087 个 A2 词汇。

## 功能

- **今日单词**：每日自动抽取，卡片式学习，支持标记完成 / 撤销完成，顶部进度条
- **单词详情**：
  - 名词：单/复数定冠词（按词尾规则自动推断，含不规则复数如 uomo→uomini）、不定冠词（un / uno / una / un'）、性别
  - 动词：四时态变位表（现在时 / 近过去时 / 未完成过去时 / 简单将来时，各六人称；不规则动词内置表 + 规则模板 + 反身动词自动加代词）
  - 形容词：性数四格变化（-o 结尾四式、-e 结尾二式）
  - 所有语法字段支持手动编辑补充
- **单词库**：1087 词，按分类（23 类）/ 状态 / 关键词筛选搜索，分页浏览
- **统计**：总词数、覆盖率、今日进度、累计抽取次数，分类分布与抽取次数分布图表
- **设置**：每日抽取数量（默认 35）、冷却天数（默认 7）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4 + MyBatis-Plus 3.5 + MySQL 8（JDK 21、Maven） |
| 前端 | Vue 3 + Vite 6 + Element Plus + Pinia + Vue Router + Axios + ECharts（意大利国旗绿主题） |

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 18+（npm）
- MySQL 8（本机服务已启动）

## 数据库初始化

**无需手动建库建表**。数据库连接配置在 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)：

- JDBC URL 带 `createDatabaseIfNotExist=true`：首次启动自动创建数据库 `italian_vocab`（utf8mb4）
- `schema.sql`（`CREATE TABLE IF NOT EXISTS`，幂等）自动建 4 张表：`word`、`word_progress`、`daily_extract`、`setting`
- 首次启动若 `word` 表为空，自动从 `backend/src/main/resources/data/vocab_data.json` 导入 1087 个单词（含自动预填的定冠词与动词变位）

如数据库账号密码不同，请在 `backend/` 下创建 `application-local.yml`（已被 .gitignore 排除，不会提交）：

```yaml
spring:
  datasource:
    password: 你的密码
```

或设置环境变量 `MYSQL_USERNAME` / `MYSQL_PASSWORD`。

## 启动方式

### 1. 启动后端（端口 8080）

```bash
cd backend
mvn spring-boot:run
```

启动日志出现 `自动导入完成：新增 1087 个` 即词库就绪。

### 2. 启动前端（端口 5173）

```bash
cd frontend
npm install        # 首次运行
npm run dev
```

### 3. 访问

浏览器打开 http://localhost:5173 （前端通过 Vite 代理转发 `/api` 到后端 8080，无需额外配置跨域）。

## 使用说明

1. 打开首页即自动抽取今日单词（每天首次访问时抽取，当天内不会重复抽取）
2. 学习后点卡片上的"标记完成"；误点可"撤销完成"
3. 点击单词卡片查看详情：名词定冠词、动词变位；点"编辑"可补充/修改变位、定冠词、性别、释义
4. "设置"页可调整每日抽取数量与冷却天数（对下一次抽取生效）

## 抽取算法说明

每天首次访问"今日单词"时抽取 N 个词（N=每日数量）：

1. 第一优先抽**从未抽取过**的词（按词库顺序）
2. 不足则抽**累计完成次数少**的词（次数升序 → 最久未抽优先）
3. **冷却期**：最近 `冷却天数` 内抽过的词不参与（自动保证同一天不重复）
4. 候选不足 N 个时放宽冷却限制补足

只有**标记完成后**该词的完成次数才 +1，撤销则 -1。未完成的词保持高优先级，冷却期后会被再次抽到复习。

说明：`extract_count` 字段的语义为**完成次数**（界面显示"完成次数"）——单词仅被抽取而未标记完成时不计入，这样未掌握的词在冷却期后仍会被优先抽出，起到强制复习的作用；"最近抽取日期"则记录该词最近一次进入每日列表的时间。

## 项目结构

```
backend/    Spring Boot 后端（controller / service / mapper / entity / util）
frontend/   Vue 3 前端（views / components / stores / api）
```

- 词库数据副本：`backend/src/main/resources/data/vocab_data.json`
- 定冠词推断与动词变位生成规则：`backend/src/main/java/com/italiano/vocab/util/ItalianGrammarUtil.java`
- 每日抽取算法：`backend/src/main/java/com/italiano/vocab/service/ExtractService.java`
