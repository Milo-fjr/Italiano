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
    <el-empty
      v-if="!loading && !current && answered > 0"
      :description="`今日拼写完成：拼对 ${rightCount} 个 · 拼错 ${wrongCount} 个（错的明天回来）`"
    >
      <el-button round @click="load">重新加载</el-button>
    </el-empty>

    <!-- 空状态：没有到期词 -->
    <el-empty
      v-else-if="!loading && !current"
      :description="nextDueAt ? `暂无到期拼写的单词，下一次拼写：${nextDueAtText}` : '暂无到期拼写的单词'"
    >
      <el-button type="primary" round @click="$router.push('/')">去学习模式背新词</el-button>
    </el-empty>

    <!-- 答题卡：一次一题，中文释义 → 拼写意语单词（+ 不规则附加形式） -->
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
        <div class="hint-line">
          拼出对应的意大利语单词<template v-if="current.extraLabel">，并填写{{ current.extraLabel }}</template>
        </div>

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
              @keyup.enter="submit"
            />
          </div>
          <div v-if="current.extraLabel" class="input-item">
            <label class="input-label">{{ current.extraLabel }}</label>
            <el-input
              v-model="inputExtra"
              size="large"
              :placeholder="current.extraLabel"
              :disabled="!!result"
              @keyup.enter="submit"
            />
          </div>
        </div>

        <!-- 结果对照 -->
        <div v-if="result" class="result">
          <div class="verdict" :class="result.passed ? 'ok' : 'bad'">
            {{ result.passed ? '✓ 全部拼对' : '✗ 有错误（明天再拼）' }}
          </div>
          <div class="compare-row" :class="result.wordCorrect ? 'ok' : 'bad'">
            <span class="compare-label">单词</span>
            <span class="compare-input">{{ inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">
              {{ result.word }}
              <SoundButton :text="result.word" small />
            </span>
          </div>
          <div v-if="result.extraLabel" class="compare-row" :class="result.extraCorrect ? 'ok' : 'bad'">
            <span class="compare-label">{{ result.extraLabel }}</span>
            <span class="compare-input">{{ inputExtra || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">{{ result.extraAnswer }}</span>
          </div>
        </div>

        <div class="card-btns">
          <el-button v-if="!result" type="primary" round size="large" :loading="submitting" @click="submit">
            提交（Enter）
          </el-button>
          <el-button v-else ref="nextBtnRef" type="primary" round size="large" @click="next">
            下一个（Enter）
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import api from '../api'
import { posTagType } from '../utils/pos'
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
const inputExtra = ref('')
/** 答题结果（null=答题中） */
const result = ref(null)
const submitting = ref(false)
const wordInputRef = ref(null)
const nextBtnRef = ref(null)

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
    const r = await api.getSpellDue()
    words.value = r.words
    total.value = r.total
    nextDueAt.value = r.nextDueAt
    currentIndex.value = 0
    rightCount.value = 0
    wrongCount.value = 0
    result.value = null
    inputWord.value = ''
    inputExtra.value = ''
    focusWord()
  } finally {
    loading.value = false
  }
}

/** 提交判分（服务端归一化比较：大小写/重音符号/多余空格容错） */
async function submit() {
  if (!current.value || result.value || submitting.value) return
  submitting.value = true
  try {
    result.value = await api.spellAnswer(current.value.wordId, {
      word: inputWord.value,
      extra: inputExtra.value
    })
    if (result.value.passed) {
      rightCount.value++
    } else {
      wrongCount.value++
    }
    await nextTick()
    nextBtnRef.value?.focus()
  } finally {
    submitting.value = false
  }
}

/** 下一题 */
function next() {
  result.value = null
  inputWord.value = ''
  inputExtra.value = ''
  currentIndex.value++
  focusWord()
}

function focusWord() {
  nextTick(() => wordInputRef.value?.focus())
}

onMounted(load)
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
</style>
