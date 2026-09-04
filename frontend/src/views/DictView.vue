<template>
  <div>
    <!-- 听写进度 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          听写模式
          <span class="date">听音 → 意大利语 · 全对升盒 / 有错归 0</span>
        </h2>
        <div class="progress-line">
          <el-progress
            class="progress"
            :percentage="total ? Math.round((answered * 100) / total) : 0"
            :stroke-width="18"
            :format="() => `${answered} / ${total}`"
          />
          <span class="progress-hint">已听 / 到期总数 · 听对 {{ rightCount }} · 听错 {{ wrongCount }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button round :loading="loading" @click="load">重新加载</el-button>
      </div>
    </div>

    <!-- 空状态：本次听完所有 -->
    <div v-if="!loading && !current && answered > 0" class="summary-card">
      <el-card>
        <template #header>
          <div class="summary-head">
            <span>今日听写完成：听对 {{ rightCount }} 个 · 听错 {{ wrongCount }} 个（错的明天回来）</span>
            <el-button round @click="load">重新加载</el-button>
          </div>
        </template>
        <div v-if="wrongCount" class="wrong-note">听错的 {{ wrongCount }} 个词已自动进错题本，去错题本复习吧</div>
        <div v-else class="all-right">全部听对，没有一个错词 🎉</div>
      </el-card>
    </div>

    <!-- 空状态：没有到期词 -->
    <el-empty
      v-else-if="!loading && !current"
      :description="nextDueAt ? `暂无到期听写的单词，下一次听写：${nextDueAtText}` : '暂无到期听写的单词'"
    >
      <el-button type="primary" round @click="$router.push('/')">去学习模式背新词</el-button>
    </el-empty>

    <!-- 答题卡：一次一题，听发音（不看词）→ 写出意语单词（+ 不规则附加形式） -->
    <div v-loading="loading" class="spell-stage">
      <el-card v-if="current" class="spell-card">
        <div class="prompt-tags">
          <span class="head-tags">
            <el-tag v-if="current.irregular" size="small" type="danger">{{ current.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(current.pos)">{{ current.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ current.category }}</el-tag>
          </span>
        </div>

        <!-- 听音区：大喇叭播放（单词原文仅进 TTS，页面不显示） -->
        <div class="audio-prompt">
          <button class="play-btn" type="button" title="播放单词发音" @click="play">
            <svg viewBox="0 0 24 24" width="34" height="34" fill="currentColor" aria-hidden="true">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
            </svg>
          </button>
          <span class="play-hint">点击喇叭听发音 · 可反复听</span>
        </div>

        <div class="hint-line">
          听音写出单词和中文释义<template v-if="current.extraLabel">，并填写{{ current.extraLabel }}</template>
        </div>

        <!-- 输入区 -->
        <div class="inputs">
          <div class="input-item">
            <label class="input-label">意大利语单词</label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入听到的单词（重音符号可不带）"
              :disabled="!!result"
              @keydown.enter="submit(false)"
            />
          </div>
          <div class="input-item">
            <label class="input-label">中文释义</label>
            <el-input
              v-model="inputMeaning"
              size="large"
              placeholder="这个词是什么意思？"
              :disabled="!!result"
              @keydown.enter="submit(false)"
            />
          </div>
          <div v-if="current.extraLabel" class="input-item">
            <label class="input-label">{{ current.extraLabel }}</label>
            <el-input
              v-model="inputExtra"
              size="large"
              :placeholder="current.extraLabel"
              :disabled="!!result"
              @keydown.enter="submit(false)"
            />
          </div>
        </div>

        <!-- 结果对照 -->
        <div v-if="result" class="result">
          <div class="verdict" :class="result.passed ? 'ok' : 'bad'">
            {{ result.passed ? '✓ 全部听对' : gaveUp ? '✗ 不会（明天再听）' : '✗ 有错误（明天再听）' }}
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
          <div class="compare-row" :class="result.meaningCorrect ? 'ok' : 'bad'">
            <span class="compare-label">中文释义</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : inputMeaning || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">{{ result.meaning }}</span>
          </div>
          <div v-if="result.extraLabel" class="compare-row" :class="result.extraCorrect ? 'ok' : 'bad'">
            <span class="compare-label">{{ result.extraLabel }}</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : inputExtra || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">{{ result.extraAnswer }}</span>
          </div>
        </div>

        <div class="card-btns">
          <el-button v-if="!result" type="danger" plain round size="large" :loading="submitting" @click="giveUp">
            不会
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
/** 到期听写队列（服务端随机顺序；逐题作答，答过的不再出现） */
const words = ref([])
/** 本次会话的到期总数（进度分母） */
const total = ref(0)
/** 最近一次未来听写到期日（无到期词时的提示） */
const nextDueAt = ref(null)
const currentIndex = ref(0)
const rightCount = ref(0)
const wrongCount = ref(0)
const inputWord = ref('')
const inputMeaning = ref('')
const inputExtra = ref('')
/** 答题结果（null=答题中） */
const result = ref(null)
/** 本题是否点了「不会」（结果页显示区别于听错） */
const gaveUp = ref(false)
const submitting = ref(false)
const wordInputRef = ref(null)

const current = computed(() => words.value[currentIndex.value])
const answered = computed(() => rightCount.value + wrongCount.value)

const nextDueAtText = computed(() => {
  if (!nextDueAt.value) return ''
  const d = new Date(nextDueAt.value)
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
})

async function load() {
  loading.value = true
  try {
    const r = await api.getDictDue()
    words.value = r.words
    total.value = r.total
    nextDueAt.value = r.nextDueAt
    currentIndex.value = 0
    rightCount.value = 0
    wrongCount.value = 0
    result.value = null
    gaveUp.value = false
    inputWord.value = ''
    inputMeaning.value = ''
    inputExtra.value = ''
    focusWord()
  } finally {
    loading.value = false
  }
}

/** 播放当前单词发音（Web Speech 意语语音包） */
function play() {
  if (current.value) {
    speakItalian(current.value.word)
  }
}

/** 提交判分（服务端判分：单词归一化容错、释义按「；」多答案括号剔除）；gaveUp=true 为「不会」直接判错 */
async function submit(gaveUpFlag = false) {
  if (!current.value || result.value || submitting.value) return
  // 防手滑：正常提交必须单词和释义都填了（「不会」按钮不受限）
  if (!gaveUpFlag && (!inputWord.value.trim() || !inputMeaning.value.trim())) {
    ElMessage.warning('还没填完呢：单词和中文释义都写下再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    result.value = await api.dictAnswer(current.value.wordId, {
      word: inputWord.value,
      meaning: inputMeaning.value,
      extra: inputExtra.value
    })
    gaveUp.value = gaveUpFlag
    if (result.value.passed) {
      rightCount.value++
    } else {
      wrongCount.value++ // 错词由后端自动进错题本，这里只计数
    }
  } finally {
    submitting.value = false
  }
}

/** 不会：放弃作答判错（归 0 明天再听），保留输入框内容仅作展示 */
function giveUp() {
  submit(true)
}

/** 下一题 */
function next() {
  if (!result.value) return
  result.value = null
  gaveUp.value = false
  inputWord.value = ''
  inputMeaning.value = ''
  inputExtra.value = ''
  currentIndex.value++
  focusWord()
}

/** 出结果后全局 Enter → 下一题（按钮 focus 不可靠：el-button ref 是组件实例非 DOM） */
function onKeydown(e) {
  if (e.key === 'Enter' && result.value && !loading.value) {
    e.preventDefault()
    next()
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

/* 听音区：大喇叭居中 */
.audio-prompt {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.play-btn {
  width: 76px;
  height: 76px;
  padding: 0;
  border: 2px solid #00934d;
  border-radius: 50%;
  background: #f0f9f3;
  color: #00934d;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.play-btn:hover {
  background: #d9f0e3;
}

.play-btn:active {
  transform: scale(0.94);
}

.play-hint {
  font-size: 13px;
  color: #98a2ac;
}

.hint-line {
  margin-top: 14px;
  font-size: 14px;
  color: #55606a;
  text-align: center;
  font-weight: 600;
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
