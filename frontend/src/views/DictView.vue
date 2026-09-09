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
          <span class="play-hint">点击喇叭或按空格播放 · 可反复听</span>
        </div>

        <div class="hint-line">
          <template v-if="stage === 1">先听发音，选出正确的中文释义</template>
          <template v-else>释义选对了！听音写出单词<template v-if="current.extraLabel">，并填写{{ current.extraLabel }}</template></template>
        </div>

        <!-- 第一关：释义 4 选 1（听懂了才能进拼写关；出结果后收起） -->
        <div v-if="stage === 1 && !result" class="inputs">
          <div class="input-item">
            <label class="input-label">这个单词是什么意思？ <span class="label-hint">（1-4 选释义 · 0 不会 · Enter 确认）</span></label>
            <div class="meaning-options">
              <button
                v-for="(opt, idx) in current.meaningOptions"
                :key="opt"
                type="button"
                class="option-btn"
                :class="{ selected: selectedMeaning === opt }"
                @click="selectedMeaning = opt"
              ><span class="option-idx">{{ idx + 1 }}</span>{{ opt }}</button>
            </div>
          </div>
        </div>

        <!-- 第二关：拼写（空格可反复听） -->
        <div v-else class="inputs">
          <div class="input-item">
            <label class="input-label">中文释义（已选对）</label>
            <div class="meaning-confirmed">{{ selectedMeaning }}</div>
          </div>
          <div v-if="isReflexiveVerb" class="reflexive-note">
            这是自反动词，请写出带 <b>si</b> 的完整形式（如 svegliarsi / sedersi）
          </div>
          <div class="input-item">
            <label class="input-label">意大利语单词 <span class="label-hint">（空格重听 · 0 不会 · Enter 提交）</span></label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入听到的单词（重音符号可不带）"
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
            <span class="compare-input">{{ gaveUp ? '（不会）' : stage === 1 ? '（未到拼写）' : inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">
              {{ result.word }}
              <SoundButton :text="result.word" small />
            </span>
          </div>
          <div class="compare-row" :class="result.meaningCorrect ? 'ok' : 'bad'">
            <span class="compare-label">中文释义</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : selectedMeaning || '（未选）' }}</span>
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
            不会（0）
          </el-button>
          <el-button v-if="!result && stage === 1" type="primary" round size="large" :loading="submitting" @click="confirmMeaning">
            确认释义（Enter）
          </el-button>
          <el-button v-else-if="!result" type="primary" round size="large" :loading="submitting" @click="submit(false)">
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
/** 选中的中文释义选项（4 选 1，点选而非手打，避免错别字/格式误判） */
const selectedMeaning = ref('')
const inputExtra = ref('')
/** 答题阶段：1=释义关（听音选义）、2=拼写关（听音写词）——释义选对才进 2 */
const stage = ref(1)
/** 答题结果（null=答题中） */
const result = ref(null)
/** 本题是否点了「不会」（结果页显示区别于听错） */
const gaveUp = ref(false)
const submitting = ref(false)
const wordInputRef = ref(null)

const current = computed(() => words.value[currentIndex.value])
const answered = computed(() => rightCount.value + wrongCount.value)

/** 自反动词：不定式以 -si 结尾。题面需提示写完整形，否则易漏 si 写成原形 */
const isReflexiveVerb = computed(
  () => !!current.value && current.value.word.toLowerCase().endsWith('si')
)

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
    stage.value = 1
    inputWord.value = ''
    selectedMeaning.value = ''
    inputExtra.value = ''
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

/** 第一关：确认释义选项——选对进拼写关，选错整题判错（服务端预检不动 SRS，判错走正式 answer） */
async function confirmMeaning() {
  if (stage.value !== 1 || !current.value || result.value || submitting.value) return
  if (!selectedMeaning.value) {
    ElMessage.warning('先按 1-4 选一个释义')
    return
  }
  submitting.value = true
  try {
    const r = await api.dictCheckMeaning(current.value.wordId, { meaning: selectedMeaning.value })
    if (r.correct) {
      stage.value = 2
      focusWord()
    } else {
      await finalize(false)
    }
  } finally {
    submitting.value = false
  }
}

/** 第二关提交判分（服务端判分：单词归一化容错 + 附加形式） */
async function submit(gaveUpFlag = false) {
  if (stage.value !== 2 || !current.value || result.value || submitting.value) return
  // 防手滑：正常提交必须写了单词（「不会」不受限）
  if (!gaveUpFlag && !inputWord.value.trim()) {
    ElMessage.warning('还没写呢：写下听到的单词再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    await finalize(gaveUpFlag)
  } finally {
    submitting.value = false
  }
}

/** 正式判分落库（SRS 推进 + 错题本），两关殊途同归：拼写提交 / 释义选错 / 不会；判分后自动朗读单词强化听力记忆 */
async function finalize(gaveUpFlag) {
  result.value = await api.dictAnswer(current.value.wordId, {
    word: inputWord.value,
    meaning: selectedMeaning.value,
    extra: inputExtra.value
  })
  gaveUp.value = gaveUpFlag
  speakItalian(result.value.word)
  if (result.value.passed) {
    rightCount.value++
  } else {
    wrongCount.value++ // 错词由后端自动进错题本，这里只计数
  }
}

/** 不会：两关任一阶段放弃作答，判错归 0 明天再听 */
async function giveUp() {
  if (!current.value || result.value || submitting.value) return
  submitting.value = true
  try {
    await finalize(true)
  } finally {
    submitting.value = false
  }
}

/** 下一题 */
function next() {
  if (!result.value) return
  result.value = null
  gaveUp.value = false
  stage.value = 1
  inputWord.value = ''
  selectedMeaning.value = ''
  inputExtra.value = ''
  currentIndex.value++
}

/** 全局键盘：出结果后 Enter 切题；第一关 1-4 选释义 + Enter 确认；0 = 不会（两关通用）；空格 = 播放发音
 * 空格不跟打字冲突：焦点在任一输入框且已有内容时放行（a presto / mi siedo 需打空格），输入框空着时按空格播放 */
function onKeydown(e) {
  if (e.key === 'Enter' && result.value && !loading.value) {
    e.preventDefault()
    next()
    return
  }
  if (!result.value && !loading.value && current.value) {
    if (e.key === ' ') {
      const el = document.activeElement
      const typing = el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && el.value.trim().length > 0
      if (typing) return
      e.preventDefault()
      play()
      return
    }
    if (e.key === '0') {
      e.preventDefault()
      giveUp()
      return
    }
    if (stage.value === 1) {
      if (e.key === 'Enter') {
        e.preventDefault()
        confirmMeaning()
        return
      }
      const idx = ['1', '2', '3', '4'].indexOf(e.key)
      if (idx >= 0 && idx < current.value.meaningOptions.length) {
        e.preventDefault()
        selectedMeaning.value = current.value.meaningOptions[idx]
      }
    }
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

.label-hint {
  color: #98a2ac;
  font-weight: 400;
  font-size: 12px;
}

/* 第二关顶部：已选对的释义（绿色确认条，拼写时知道自己在写什么） */
.meaning-confirmed {
  padding: 10px 12px;
  border: 1px solid #00934d;
  border-radius: 8px;
  background: #eef8f2;
  color: #00934d;
  font-size: 14px;
  font-weight: 600;
}

/* 自反动词提示：避免漏 si 写成原形 */
.reflexive-note {
  padding: 6px 10px;
  background: #f3f0e8;
  border-radius: 6px;
  font-size: 13px;
  color: #8b6d1c;
  b {
    font-weight: 600;
  }
}

/* 释义 4 选 1 选项：两列卡片，点选高亮，序号角标对应键盘 1-4 */
.meaning-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #d9dee3;
  border-radius: 8px;
  background: #fff;
  color: #1e3a2b;
  font-size: 14px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}

.option-idx {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #eef0f2;
  color: #55606a;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.option-btn.selected .option-idx {
  background: #00934d;
  color: #fff;
}

.option-btn:hover:not(:disabled) {
  border-color: #00934d;
}

.option-btn.selected {
  border-color: #00934d;
  background: #eef8f2;
  color: #00934d;
  font-weight: 600;
}

.option-btn:disabled {
  cursor: default;
  opacity: 1;
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
