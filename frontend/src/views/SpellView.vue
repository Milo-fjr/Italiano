<template>
  <div>
    <!-- 拼写进度 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          拼写模式
          <span class="date">中文 → 意大利语 · 全对升盒 / 有错归 0</span>
        </h2>
        <div class="progress-line">
          <el-progress
            class="progress"
            :percentage="total ? Math.round((answered * 100) / total) : 0"
            :stroke-width="18"
            :format="() => `${answered} / ${total}`"
          />
          <span class="progress-hint">已拼 / 到期总数 · 拼对 {{ rightCount }} · 拼错 {{ wrongCount }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button round :loading="loading" @click="load">重新加载</el-button>
      </div>
    </div>

    <!-- 空状态：本次拼完全部 -->
    <div v-if="!loading && !current && answered > 0" class="summary-card">
      <el-card>
        <template #header>
          <div class="summary-head">
            <span>今日拼写完成：拼对 {{ rightCount }} 个 · 拼错 {{ wrongCount }} 个（错的明天回来）</span>
            <el-button round @click="load">重新加载</el-button>
          </div>
        </template>
        <div v-if="wrongCount" class="wrong-note">拼错的 {{ wrongCount }} 个词已自动进错题本，去错题本复习吧</div>
        <div v-else class="all-right">全部拼对，没有一个错词 🎉</div>
      </el-card>
    </div>

    <!-- 空状态：没有到期词 -->
    <el-empty
      v-else-if="!loading && !current"
      :description="nextDueAt ? `暂无到期拼写的单词，下一次拼写：${nextDueAtText}` : '暂无到期拼写的单词'"
    >
      <el-button type="primary" round @click="$router.push('/')">去学习模式背新词</el-button>
    </el-empty>

    <!-- 答题卡：一次一题，中文释义 → 拼写意语单词 -->
    <div v-loading="loading" class="spell-stage">
      <el-card v-if="current" class="spell-card">
        <div class="prompt-tags">
          <span class="head-tags">
            <el-tag v-if="current.irregular" size="small" type="danger">{{ current.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(current.pos)">{{ current.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ current.category }}</el-tag>
          </span>
        </div>
        <div class="meaning-prompt">{{ current.meaning }}</div>
        <div v-if="isReflexiveVerb" class="reflexive-note">
          这是自反动词，请写出带 <b>si</b> 的完整形式（如 svegliarsi / sedersi）
        </div>
        <div class="hint-line">拼出对应的意大利语单词（不规则变化去加练模式专练）</div>

        <!-- 输入区 -->
        <div class="inputs">
          <div class="input-item">
            <label class="input-label">意大利语单词</label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入意大利语单词（重音符号可不带）"
              :disabled="!!result"
              @keydown.enter="submit(false)"
            />
          </div>
        </div>

        <!-- 结果对照 -->
        <div v-if="result" class="result">
          <div class="verdict" :class="result.passed ? 'ok' : 'bad'">
            {{ result.passed ? '✓ 拼对了' : gaveUp ? '✗ 不会（明天再拼）' : '✗ 拼错了（明天再拼）' }}
          </div>
          <div class="compare-row" :class="result.wordCorrect ? 'ok' : 'bad'">
            <span class="compare-label">单词</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">
              {{ result.word }}
              <SoundButton :text="result.word" small />
            </span>
          </div>
        </div>

        <div class="card-btns">
          <el-button v-if="!result" type="danger" plain round size="large" :loading="submitting" @click="giveUp">
            不会（0）
          </el-button>
          <el-button v-if="!result" type="primary" round size="large" :loading="submitting" @click="submit(false)">
            提交（Enter）
          </el-button>
          <el-button v-else type="primary" round size="large" @click="next">
            下一个（Enter）
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { posTagType } from '../utils/pos'
import { speakItalian } from '../utils/tts'
import SoundButton from '../components/SoundButton.vue'

const loading = ref(false)
/** 到期拼写队列（服务端随机顺序；逐题作答，答过的不再出现） */
const words = ref([])
/** 本次会话的到期总数（进度分母） */
const total = ref(0)
/** 最近一次未来拼写到期日（无到期词时的提示） */
const nextDueAt = ref(null)
const currentIndex = ref(0)
const rightCount = ref(0)
const wrongCount = ref(0)
const inputWord = ref('')
/** 答题结果（null=答题中） */
const result = ref(null)
/** 本题是否点了「不会」（结果页显示区别于拼错） */
const gaveUp = ref(false)
const submitting = ref(false)
const wordInputRef = ref(null)

const current = computed(() => words.value[currentIndex.value])
const answered = computed(() => rightCount.value + wrongCount.value)

/** 自反动词：词性 v.rifl.。题目 DTO 不含意语单词（防泄题），用词性判断；题面提示写完整形避免漏 si */
const isReflexiveVerb = computed(
  () => !!current.value && (current.value.pos || '').startsWith('v.rifl')
)

const nextDueAtText = computed(() => {
  if (!nextDueAt.value) return ''
  const d = new Date(nextDueAt.value)
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
})

async function load() {
  loading.value = true
  try {
    const r = await api.getSpellDue()
    words.value = r.words
    total.value = r.total
    nextDueAt.value = r.nextDueAt
    currentIndex.value = 0
    rightCount.value = 0
    wrongCount.value = 0
    result.value = null
    gaveUp.value = false
    inputWord.value = ''
    focusWord()
  } finally {
    loading.value = false
  }
}

/** 提交判分（服务端归一化比较：大小写/重音符号/多余空格容错）；gaveUp=true 为「不会」直接判错。
 * 判分后自动朗读正确答案，拼对时强化听觉记忆、拼错/不会时听到正确发音 */
async function submit(gaveUpFlag = false) {
  if (!current.value || result.value || submitting.value) return
  // 防手滑：正常提交必须输入了单词（「不会」按钮不受限）
  if (!gaveUpFlag && !inputWord.value.trim()) {
    ElMessage.warning('还没输入呢：写下答案再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    result.value = await api.spellAnswer(current.value.wordId, {
      word: inputWord.value
    })
    gaveUp.value = gaveUpFlag
    speakItalian(result.value.word)
    if (result.value.passed) {
      rightCount.value++
    } else {
      wrongCount.value++ // 错词由后端自动进错题本，这里只计数
    }
  } finally {
    submitting.value = false
  }
}

/** 不会：放弃作答判错（归 0 明天再拼），保留输入框内容仅作展示 */
function giveUp() {
  submit(true)
}

/** 下一题 */
function next() {
  if (!result.value) return
  result.value = null
  gaveUp.value = false
  inputWord.value = ''
  currentIndex.value++
  focusWord()
}

/** 全局键盘：出结果后 Enter 切题；答题阶段按 0 = 不会（数字键不跟意语打字冲突，选 0 避免 N 撞字母 n） */
function onKeydown(e) {
  if (e.key === 'Enter' && result.value && !loading.value) {
    e.preventDefault()
    next()
    return
  }
  if (e.key === '0' && !result.value && !loading.value && current.value) {
    e.preventDefault()
    giveUp()
  }
}

function focusWord() {
  nextTick(() => wordInputRef.value?.focus())
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.today-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 24px;
}

.today-info {
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.today-title {
  margin-bottom: 14px;
}

.date {
  font-size: 14px;
  font-weight: 400;
  color: #98a2ac;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress {
  max-width: 420px;
  flex: 1;
}

.progress-hint {
  font-size: 12px;
  color: #98a2ac;
  white-space: nowrap;
}

/* 单卡顺序流：打字需要焦点，一次只出一题 */
.spell-stage {
  display: flex;
  justify-content: center;
  min-height: 120px;
}

.spell-card {
  width: 100%;
  max-width: 620px;
  border: 1px solid #e8ecf0;
  padding: 6px 4px;
}

.head-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.meaning-prompt {
  margin-top: 14px;
  font-size: 26px;
  font-weight: 700;
  color: #1e3a2b;
  line-height: 1.4;
}

.reflexive-note {
  margin-top: 8px;
  padding: 6px 10px;
  background: #f3f0e8;
  border-radius: 6px;
  font-size: 13px;
  color: #8b6d1c;
  b {
    font-weight: 600;
  }
}

.hint-line {
  margin-top: 6px;
  font-size: 13px;
  color: #98a2ac;
}

.inputs {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #55606a;
}

/* 结果对照 */
.result {
  margin-top: 20px;
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.verdict {
  font-size: 16px;
  font-weight: 700;
}

.verdict.ok {
  color: #00934d;
}

.verdict.bad {
  color: #cd212a;
}

.compare-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  flex-wrap: wrap;
}

.compare-label {
  min-width: 92px;
  color: #98a2ac;
  font-size: 13px;
}

.compare-input {
  color: #55606a;
}

.compare-row.ok .compare-input {
  color: #00934d;
}

.compare-row.bad .compare-input {
  color: #cd212a;
  text-decoration: line-through;
}

.arrow {
  color: #c0c6cd;
}

.compare-answer {
  font-weight: 700;
  color: #1e3a2b;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.card-btns {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 结束汇总卡 */
.summary-card {
  margin-bottom: 24px;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.wrong-note {
  color: #55606a;
  font-size: 14px;
  padding: 8px 0;
}

.all-right {
  color: #00934d;
  font-size: 15px;
  padding: 8px 0;
}
</style>
