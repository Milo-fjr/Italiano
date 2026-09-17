<template>
  <div>
    <!-- 头部 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          加练模式
          <span class="date">从已学词里随机抽 · 错了进错题本 · 不记盒子</span>
        </h2>
        <div v-if="started" class="progress-line">
          <el-progress
            class="progress"
            :percentage="total ? Math.round(answered * 100 / total) : 0"
            :stroke-width="18"
            :format="() => `${answered} / ${total}`"
          />
          <span class="progress-hint">已答 / 总题数 · 对 {{ rightCount }} · 错 {{ wrongCount }}</span>
        </div>
        <div v-else class="progress-line">
          <el-progress
            class="progress"
            :percentage="0"
            :stroke-width="18"
            :format="() => '准备开始'"
          />
          <span class="progress-hint">从已学过的词里随机选，不影响 SRS 盒子</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button v-if="started" round @click="restart">退出练习</el-button>
      </div>
    </div>

    <!-- 选题型 + 数量 -->
    <div v-if="!started" class="setup-card">
      <el-card>
        <template #header>
          <div class="setup-title">选题型与题量</div>
        </template>
        <div class="type-grid">
          <div
            v-for="t in typeOptions"
            :key="t.value"
            class="type-card"
            :class="{ active: selectedType === t.value }"
            @click="selectedType = t.value"
          >
            <div class="type-icon">{{ t.icon }}</div>
            <div class="type-name">{{ t.label }}</div>
            <div class="type-desc">{{ t.desc }}</div>
          </div>
        </div>
        <div class="count-row">
          <span class="count-label">{{ selectedType === 'irregular' ? '词量（每词多个考点）' : '题量' }}</span>
          <el-radio-group v-model="selectedCount" size="large">
            <el-radio-button v-for="n in [10, 20, 30, 50]" :key="n" :value="n">
              {{ n }}{{ selectedType === 'irregular' ? ' 词' : ' 题' }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="start-row">
          <el-button type="primary" round size="large" :loading="loading" @click="start">
            开始加练
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 认识（quiz）：网格翻卡 -->
    <div v-else-if="selectedType === 'quiz'" v-loading="loading" class="card-grid">
      <el-card
        v-for="w in remainingQuiz"
        :key="w.wordId"
        class="word-card"
        shadow="hover"
        @click="toggleFlip(w.wordId)"
      >
        <div class="flip-inner" :class="{ flipped: flippedSet.has(w.wordId) }">
          <div class="flip-face flip-front">
            <div class="card-head">
              <div class="word-line">
                <span class="word">{{ w.word }}</span>
                <SoundButton :text="w.word" />
              </div>
              <span class="head-tags">
                <el-tag v-if="w.irregular" size="small" type="danger">{{ w.irregular }}</el-tag>
                <el-tag size="small" :type="posTagType(w.pos)">{{ w.pos || '-' }}</el-tag>
              </span>
            </div>
          </div>
          <div class="flip-face flip-back">
            <div class="meaning">{{ w.meaning }}</div>
            <div class="card-foot">
              <el-tag size="small" type="info" effect="plain">{{ w.category }}</el-tag>
            </div>
          </div>
        </div>
        <div class="card-btns" @click.stop>
          <el-button type="primary" round size="small" @click="onKnow(w.wordId)">认识</el-button>
          <el-button type="danger" plain round size="small" @click="onForget(w.wordId)">不认识</el-button>
        </div>
      </el-card>
    </div>

    <!-- 拼写（spell）：单卡输入 -->
    <div v-else-if="selectedType === 'spell' && spellCurrent" v-loading="loading" class="spell-stage">
      <el-card class="spell-card">
        <div class="prompt-tags">
          <span class="head-tags">
            <el-tag v-if="spellCurrent.irregular" size="small" type="danger">{{ spellCurrent.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(spellCurrent.pos)">{{ spellCurrent.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ spellCurrent.category }}</el-tag>
          </span>
        </div>
        <div class="meaning-prompt">{{ spellCurrent.meaning }}</div>
        <div v-if="spellIsReflexive" class="reflexive-note">
          这是自反动词，请写出带 <b>si</b> 的完整形式（如 svegliarsi / sedersi）
        </div>
        <div class="hint-line">拼出对应的意大利语单词（不规则变化去「不规则变化」题型专练）</div>
        <div class="inputs">
          <div class="input-item">
            <label class="input-label">意大利语单词</label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入意大利语单词（重音符号可不带）"
              :disabled="!!spellResult"
              @keydown.enter="spellSubmit(false)"
            />
          </div>
        </div>
        <div v-if="spellResult" class="result">
          <div class="verdict" :class="spellResult.passed ? 'ok' : 'bad'">
            {{ spellResult.passed ? '✓ 拼对了' : gaveUp ? '✗ 不会' : '✗ 拼错了' }}
          </div>
          <div class="compare-row" :class="spellResult.wordCorrect ? 'ok' : 'bad'">
            <span class="compare-label">单词</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">{{ spellResult.word }}<SoundButton :text="spellResult.word" small /></span>
          </div>
        </div>
        <div class="card-btns">
          <el-button v-if="!spellResult" type="danger" plain round size="large" :loading="submitting" @click="spellGiveUp">
            不会（0）
          </el-button>
          <el-button v-if="!spellResult" type="primary" round size="large" :loading="submitting" @click="spellSubmit(false)">
            提交（Enter）
          </el-button>
          <el-button v-else type="primary" round size="large" @click="spellNext">
            下一个（Enter）
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 听写（dict）：单卡两段式，UI 与普通听写模式一致 -->
    <div v-else-if="selectedType === 'dict' && dictCurrent" v-loading="loading" class="spell-stage">
      <el-card class="spell-card">
        <div class="prompt-tags">
          <span class="head-tags">
            <el-tag v-if="dictCurrent.irregular" size="small" type="danger">{{ dictCurrent.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(dictCurrent.pos)">{{ dictCurrent.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ dictCurrent.category }}</el-tag>
          </span>
        </div>

        <!-- 听音区：大喇叭播放（单词原文仅进 TTS，页面不显示） -->
        <div class="audio-prompt">
          <button class="play-btn" type="button" title="播放单词发音" @click="playDictWord">
            <svg viewBox="0 0 24 24" width="34" height="34" fill="currentColor" aria-hidden="true">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
            </svg>
          </button>
          <span class="play-hint">点击喇叭或按空格播放 · 可反复听</span>
        </div>

        <div class="stage-hint">
          <template v-if="dictStage === 1">先听发音，选出正确的中文释义</template>
          <template v-else>释义选对了！听音写出单词</template>
        </div>

        <!-- 第一关：释义 4 选 1（听懂了才能进拼写关） -->
        <div v-if="dictStage === 1 && !dictResult" class="inputs">
          <div class="input-item">
            <label class="input-label">这个单词是什么意思？ <span class="label-hint">（1-4 选释义 · 0 不会 · Enter 确认）</span></label>
            <div class="meaning-options">
              <button
                v-for="(opt, idx) in dictCurrent.meaningOptions"
                :key="opt"
                type="button"
                class="option-btn"
                :class="{ selected: pickedMeaning === opt }"
                @click="pickedMeaning = opt"
              ><span class="option-idx">{{ idx + 1 }}</span>{{ opt }}</button>
            </div>
          </div>
        </div>

        <!-- 第二关：拼写（空格可反复听） -->
        <div v-else-if="!dictResult" class="inputs">
          <div class="input-item">
            <label class="input-label">中文释义（已选对）</label>
            <div class="meaning-confirmed">{{ pickedMeaning }}</div>
          </div>
          <div v-if="dictIsReflexive" class="reflexive-note">
            这是自反动词，请写出带 <b>si</b> 的完整形式（如 svegliarsi / sedersi）
          </div>
          <div class="input-item">
            <label class="input-label">意大利语单词 <span class="label-hint">（空格重听 · 0 不会 · Enter 提交）</span></label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入听到的单词（重音符号可不带）"
              :disabled="!!dictResult"
              @keydown.enter="dictSubmit(false)"
            />
          </div>
        </div>

        <!-- 结果对照：与普通听写同顺序（单词 → 释义） -->
        <div v-if="dictResult" class="result">
          <div class="verdict" :class="dictResult.passed ? 'ok' : 'bad'">
            {{ dictResult.passed ? '✓ 全部听对' : gaveUp ? '✗ 不会' : '✗ 有错误' }}
          </div>
          <div class="compare-row" :class="dictResult.wordCorrect ? 'ok' : 'bad'">
            <span class="compare-label">单词</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : dictStage === 1 ? '（未到拼写）' : inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">
              {{ dictResult.word }}
              <SoundButton :text="dictResult.word" small />
            </span>
          </div>
          <div class="compare-row" :class="dictResult.meaningCorrect ? 'ok' : 'bad'">
            <span class="compare-label">中文释义</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : pickedMeaning || '（未选）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">{{ dictResult.meaning }}</span>
          </div>
        </div>

        <div class="card-btns">
          <el-button v-if="!dictResult" type="danger" plain round size="large" :loading="submitting" @click="dictStage === 1 ? dictGiveUpAtMeaning() : dictSubmit(true)">
            不会（0）
          </el-button>
          <el-button v-if="!dictResult && dictStage === 1" type="primary" round size="large" :loading="submitting" @click="confirmDictMeaning">
            确认释义（Enter）
          </el-button>
          <el-button v-else-if="!dictResult" type="primary" round size="large" :loading="submitting" @click="dictSubmit(false)">
            提交（Enter）
          </el-button>
          <el-button v-else type="primary" round size="large" @click="dictNext">
            下一个（Enter）
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 不规则变化（irregular）：单卡逐考点，单词即题面 -->
    <div v-else-if="selectedType === 'irregular' && irrCurrent" v-loading="loading" class="spell-stage">
      <el-card class="spell-card">
        <div class="prompt-tags">
          <span class="head-tags">
            <el-tag v-if="irrCurrent.irregular" size="small" type="danger">{{ irrCurrent.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(irrCurrent.pos)">{{ irrCurrent.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ irrCurrent.category }}</el-tag>
          </span>
        </div>
        <div class="word-line">
          <span class="word">{{ irrCurrent.word }}</span>
          <SoundButton :text="irrCurrent.word" />
        </div>
        <div class="meaning-prompt">{{ irrCurrent.meaning }}</div>
        <div class="point-label">{{ irrCurrent.point.label }}</div>
        <div v-if="irrCurrent.point.type === 'bello'" class="bello-context">
          ___ {{ irrCurrent.point.contextNoun }}（{{ irrCurrent.point.contextMeaning }}{{ irrCurrent.point.contextPlural ? '，复数' : '' }}）
        </div>
        <div class="hint-line">写出上方考点的正确形式（重音符号可不带；多形式任写其一）</div>
        <div class="inputs">
          <div class="input-item">
            <label class="input-label">{{ irrCurrent.point.label }} <span class="label-hint">（0 不会 · Enter 提交）</span></label>
            <el-input
              ref="wordInputRef"
              v-model="inputWord"
              size="large"
              placeholder="输入正确形式（重音符号可不带）"
              :disabled="!!irrResult"
              @keydown.enter="irrSubmit(false)"
            />
          </div>
        </div>
        <div v-if="irrResult" class="result">
          <div class="verdict" :class="irrResult.passed ? 'ok' : 'bad'">
            {{ irrResult.passed ? '✓ 答对了' : gaveUp ? '✗ 不会' : '✗ 答错了' }}
          </div>
          <div class="compare-row" :class="irrResult.passed ? 'ok' : 'bad'">
            <span class="compare-label">{{ irrCurrent.point.label }}</span>
            <span class="compare-input">{{ gaveUp ? '（不会）' : inputWord || '（未输入）' }}</span>
            <span class="arrow">→</span>
            <span class="compare-answer">
              {{ irrResult.answer }}
              <SoundButton :text="(irrResult.answer || '').split('/')[0]" small />
            </span>
          </div>
        </div>
        <div class="card-btns">
          <el-button v-if="!irrResult" type="danger" plain round size="large" :loading="submitting" @click="irrGiveUp">
            不会（0）
          </el-button>
          <el-button v-if="!irrResult" type="primary" round size="large" :loading="submitting" @click="irrSubmit(false)">
            提交（Enter）
          </el-button>
          <el-button v-else type="primary" round size="large" @click="irrNext">
            下一个（Enter）
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 完成汇总 -->
    <div v-if="started && finished" class="summary-card">
      <el-card>
        <template #header>
          <div class="summary-head">
            <span>加练完成：对 {{ rightCount }} · 错 {{ wrongCount }} 个</span>
            <el-button round @click="restart">再来一组</el-button>
          </div>
        </template>
        <div v-if="wrongCount > 0" class="wrong-note">
          答错的 {{ wrongCount }} 个词已自动进错题本，去错题本复习吧
        </div>
        <div v-else class="all-right">全部答对，一个都没错 🎉</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { posTagType } from '../utils/pos'
import { speakItalian } from '../utils/tts'
import SoundButton from '../components/SoundButton.vue'

const typeOptions = [
  { value: 'quiz', label: '认识翻卡', icon: '🔄', desc: '意→中翻卡核对，练眼熟' },
  { value: 'spell', label: '中→意拼写', icon: '✍️', desc: '给中文拼意语，练手速' },
  { value: 'dict', label: '听音写词', icon: '🎧', desc: '先选释义再拼写，练听力' },
  { value: 'irregular', label: '不规则变化', icon: '⚡', desc: '专练变位/复数/变形，一词多考点' }
]

const loading = ref(false)
const started = ref(false)
const selectedType = ref('spell')
const selectedCount = ref(20)
const total = ref(0)
const words = ref([]) // 全部题目
const answeredIds = reactive(new Set())
const rightCount = ref(0)
const wrongCount = ref(0)
const answered = computed(() => rightCount.value + wrongCount.value)
const finished = computed(() => started.value && answered.value >= total.value)

// quiz 专属
const flippedSet = reactive(new Set())
const remainingQuiz = computed(() => words.value.filter((w) => !answeredIds.has(w.wordId)))

// spell/dict/irregular 共享
const currentIndex = ref(0)
const inputWord = ref('')
const gaveUp = ref(false)
const submitting = ref(false)
const wordInputRef = ref(null)

// spell 专属
const spellResult = ref(null)
const spellCurrent = computed(() => {
  if (selectedType.value !== 'spell') return null
  return words.value[currentIndex.value] || null
})
const spellIsReflexive = computed(
  () => !!spellCurrent.value && (spellCurrent.value.pos || '').startsWith('v.rifl')
)

// dict 专属
const dictStage = ref(1) // 1=选释义 2=拼写
const pickedMeaning = ref('')
const dictResult = ref(null)
const dictCurrent = computed(() => {
  if (selectedType.value !== 'dict') return null
  return words.value[currentIndex.value] || null
})
const dictIsReflexive = computed(
  () => !!dictCurrent.value && (dictCurrent.value.pos || '').startsWith('v.rifl')
)

// irregular 专属：整词入队后按考点摊平成逐题队列（一个词的不规则点一次练全）
const irregularQueue = ref([])
const irrIndex = ref(0)
const irrResult = ref(null)
const irrCurrent = computed(() => {
  if (selectedType.value !== 'irregular') return null
  return irregularQueue.value[irrIndex.value] || null
})

async function start() {
  loading.value = true
  try {
    const r = await api.getPractice(selectedType.value, selectedCount.value)
    words.value = r.words
    if (selectedType.value === 'irregular') {
      // r.total 是词数；进度按考点题数计（每词多个考点）
      irregularQueue.value = r.words.flatMap((w) =>
        (w.points || []).map((p) => ({ ...w, point: p }))
      )
      total.value = irregularQueue.value.length
    } else {
      total.value = r.total
    }
    if (!total.value) {
      ElMessage.info(r.message || '没有可加练的词')
      return
    }
    started.value = true
    answeredIds.clear()
    flippedSet.clear()
    rightCount.value = 0
    wrongCount.value = 0
    currentIndex.value = 0
    irrIndex.value = 0
    inputWord.value = ''
    spellResult.value = null
    dictResult.value = null
    dictStage.value = 1
    pickedMeaning.value = ''
    irrResult.value = null
    gaveUp.value = false
    if (selectedType.value === 'spell' || selectedType.value === 'irregular') {
      focusWord()
    }
  } finally {
    loading.value = false
  }
}

function restart() {
  started.value = false
}

// ===== Quiz =====
function toggleFlip(id) {
  if (flippedSet.has(id)) flippedSet.delete(id)
  else flippedSet.add(id)
}

async function onKnow(id) {
  const w = words.value.find((x) => x.wordId === id)
  await api.practiceKnow(id, true)
  if (w) speakItalian(w.word)
  answeredIds.add(id)
  rightCount.value++
  flippedSet.delete(id)
}

async function onForget(id) {
  const w = words.value.find((x) => x.wordId === id)
  await api.practiceKnow(id, false)
  if (w) speakItalian(w.word)
  answeredIds.add(id)
  wrongCount.value++
  flippedSet.delete(id)
}

// ===== Spell =====
async function spellSubmit(gaveUpFlag = false) {
  if (!spellCurrent.value || spellResult.value || submitting.value) return
  if (!gaveUpFlag && !inputWord.value.trim()) {
    ElMessage.warning('还没输入呢：写下答案再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    spellResult.value = await api.practiceSpellAnswer(spellCurrent.value.wordId, {
      word: inputWord.value
    })
    gaveUp.value = gaveUpFlag
    speakItalian(spellResult.value.word)
    if (spellResult.value.passed) rightCount.value++
    else wrongCount.value++
    answeredIds.add(spellCurrent.value.wordId)
  } finally {
    submitting.value = false
  }
}

function spellGiveUp() {
  spellSubmit(true)
}

function spellNext() {
  if (!spellResult.value) return
  spellResult.value = null
  gaveUp.value = false
  inputWord.value = ''
  currentIndex.value++
  focusWord()
}

// ===== Irregular（不规则变化专考）=====
async function irrSubmit(gaveUpFlag = false) {
  if (!irrCurrent.value || irrResult.value || submitting.value) return
  if (!gaveUpFlag && !inputWord.value.trim()) {
    ElMessage.warning('还没输入呢：写下答案再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    const p = irrCurrent.value.point
    irrResult.value = await api.practiceIrregularAnswer(irrCurrent.value.wordId, {
      type: p.type,
      person: p.person,
      contextNoun: p.contextNoun,
      contextGender: p.contextGender,
      contextPlural: p.contextPlural,
      input: inputWord.value
    })
    gaveUp.value = gaveUpFlag
    // 多形式（colleghi/colleghe）读第一个
    speakItalian((irrResult.value.answer || '').split('/')[0])
    if (irrResult.value.passed) rightCount.value++
    else wrongCount.value++
  } finally {
    submitting.value = false
  }
}

function irrGiveUp() {
  irrSubmit(true)
}

function irrNext() {
  if (!irrResult.value) return
  irrResult.value = null
  gaveUp.value = false
  inputWord.value = ''
  irrIndex.value++
  focusWord()
}

// ===== Dict =====
function playDictWord() {
  if (dictCurrent.value) speakItalian(dictCurrent.value.word)
}

/** 第一关：确认释义选项——选对进拼写关，选错整题判错（服务端预检不动任何记录，判错走正式 dict-answer） */
async function confirmDictMeaning() {
  if (dictStage.value !== 1 || !dictCurrent.value || dictResult.value || submitting.value) return
  if (!pickedMeaning.value) {
    ElMessage.warning('先按 1-4 选一个释义')
    return
  }
  submitting.value = true
  try {
    const r = await api.practiceDictCheckMeaning(dictCurrent.value.wordId, { meaning: pickedMeaning.value })
    if (r.correct) {
      dictStage.value = 2
      focusWord()
    } else {
      await dictFinalize(false)
    }
  } finally {
    submitting.value = false
  }
}

function dictGiveUpAtMeaning() {
  // 释义阶段就放弃：直接判错
  dictFinalize(true)
}

async function dictSubmit(gaveUpFlag = false) {
  if (!dictCurrent.value || dictResult.value || submitting.value) return
  if (!gaveUpFlag && !inputWord.value.trim()) {
    ElMessage.warning('还没写呢：写下听到的单词再按 Enter，或点「不会」')
    return
  }
  submitting.value = true
  try {
    dictResult.value = await api.practiceDictAnswer(dictCurrent.value.wordId, {
      word: inputWord.value,
      meaning: pickedMeaning.value
    })
    gaveUp.value = gaveUpFlag
    speakItalian(dictResult.value.word)
    if (dictResult.value.passed) rightCount.value++
    else wrongCount.value++
    answeredIds.add(dictCurrent.value.wordId)
  } finally {
    submitting.value = false
  }
}

// 释义阶段放弃/选错时的正式判分（没填单词，服务端会判错；不切阶段，结果里单词显示「未到拼写」）
async function dictFinalize(gaveUpFlag) {
  if (!dictCurrent.value) return
  submitting.value = true
  try {
    dictResult.value = await api.practiceDictAnswer(dictCurrent.value.wordId, {
      word: '',
      meaning: pickedMeaning.value || ''
    })
    gaveUp.value = gaveUpFlag
    speakItalian(dictResult.value.word)
    wrongCount.value++
    answeredIds.add(dictCurrent.value.wordId)
  } finally {
    submitting.value = false
  }
}

function dictNext() {
  if (!dictResult.value) return
  dictResult.value = null
  gaveUp.value = false
  inputWord.value = ''
  pickedMeaning.value = ''
  dictStage.value = 1
  currentIndex.value++
}

// ===== 通用 =====
function focusWord() {
  nextTick(() => wordInputRef.value?.focus())
}

// 全局键盘：与普通拼写/听写同口径——出结果后 Enter 切题；0 = 不会；
// 听写：空格播放（两关通用，输入框已有内容时放行避免打断打字）、阶段1 1-4 选释义 + Enter 确认
function onKeydown(e) {
  if (!started.value || loading.value) return

  // 出结果后 Enter 下一题（spell/dict/irregular）
  if (e.key === 'Enter') {
    if (selectedType.value === 'spell' && spellResult.value) {
      e.preventDefault()
      spellNext()
      return
    }
    if (selectedType.value === 'dict' && dictResult.value) {
      e.preventDefault()
      dictNext()
      return
    }
    if (selectedType.value === 'irregular' && irrResult.value) {
      e.preventDefault()
      irrNext()
      return
    }
  }

  if (selectedType.value === 'dict' && dictCurrent.value && !dictResult.value) {
    // 空格 = 播放发音（两关通用）：焦点在输入框且已有内容时放行（短语要打空格）
    if (e.key === ' ') {
      const el = document.activeElement
      const typing = el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && el.value.trim().length > 0
      if (typing) return
      e.preventDefault()
      playDictWord()
      return
    }
    if (e.key === '0') {
      e.preventDefault()
      if (dictStage.value === 1) dictGiveUpAtMeaning()
      else dictSubmit(true)
      return
    }
    // 第一关：1-4 选释义，Enter 确认
    if (dictStage.value === 1) {
      if (e.key === 'Enter') {
        e.preventDefault()
        confirmDictMeaning()
        return
      }
      const idx = ['1', '2', '3', '4'].indexOf(e.key)
      if (idx >= 0 && dictCurrent.value.meaningOptions && idx < dictCurrent.value.meaningOptions.length) {
        e.preventDefault()
        pickedMeaning.value = dictCurrent.value.meaningOptions[idx]
      }
    }
    return
  }

  // 拼写：0 = 不会（Enter 提交由输入框 @keydown.enter 处理）
  if (e.key === '0' && selectedType.value === 'spell' && spellCurrent.value && !spellResult.value) {
    e.preventDefault()
    spellGiveUp()
  }

  // 不规则：0 = 不会（Enter 提交由输入框 @keydown.enter 处理）
  if (e.key === '0' && selectedType.value === 'irregular' && irrCurrent.value && !irrResult.value) {
    e.preventDefault()
    irrGiveUp()
  }
}

onMounted(() => {
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
.today-info { flex: 1; }
.header-actions { display: flex; align-items: center; gap: 16px; }
.today-title { margin-bottom: 14px; }
.date { font-size: 14px; font-weight: 400; color: #98a2ac; }
.progress-line { display: flex; align-items: center; gap: 12px; }
.progress { max-width: 420px; flex: 1; }
.progress-hint { font-size: 12px; color: #98a2ac; white-space: nowrap; }

/* 选题型 */
.setup-card { max-width: 820px; margin: 0 auto; }
.setup-title { font-size: 16px; font-weight: 600; color: #1e3a2b; }
.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.type-card {
  border: 2px solid #e8ecf0;
  border-radius: 12px;
  padding: 20px 16px;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s ease;
  background: #fff;
}
.type-card:hover {
  border-color: #80c9a6;
  transform: translateY(-2px);
}
.type-card.active {
  border-color: #00934d;
  background: #e6f4ec;
}
.type-icon { font-size: 32px; margin-bottom: 8px; }
.type-name { font-size: 16px; font-weight: 600; color: #1e3a2b; margin-bottom: 4px; }
.type-desc { font-size: 12px; color: #98a2ac; }

.count-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.count-label { font-size: 14px; color: #55606a; flex-shrink: 0; }
.start-row { display: flex; justify-content: center; }

/* 网格卡片（quiz）——与测验模式一致 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
  min-height: 120px;
}
.word-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  position: relative;
  overflow: visible;
  border: 1px solid #e8ecf0;
}
.word-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0, 90, 45, 0.12); }
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px; flex-wrap: wrap;
}
.word-line { display: flex; align-items: center; gap: 8px; min-width: 0; }
.head-tags {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  justify-content: flex-end; flex-shrink: 0;
}
.word { font-size: 21px; font-weight: 700; color: #1e3a2b; letter-spacing: 0.3px; }
.meaning { margin-top: 8px; font-size: 14px; color: #55606a; min-height: 20px; }
.card-foot { margin-top: 10px; display: flex; align-items: center; justify-content: space-between; }
.card-btns { margin-top: 12px; display: flex; justify-content: flex-end; }

.flip-inner {
  position: relative; min-height: 56px;
  transition: transform 0.25s ease; transform-style: preserve-3d;
}
.flip-inner.flipped { transform: rotateY(180deg); }
.flip-face { backface-visibility: hidden; -webkit-backface-visibility: hidden; }
.flip-back {
  position: absolute; inset: 0;
  transform: rotateY(180deg);
  display: flex; flex-direction: column; justify-content: center;
}

/* 单卡答题（spell/dict）——与拼写模式一致 */
.spell-stage { display: flex; justify-content: center; min-height: 120px; }
.spell-card { width: 100%; max-width: 620px; border: 1px solid #e8ecf0; padding: 6px 4px; }
.prompt-tags { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.meaning-prompt {
  margin-top: 14px; font-size: 26px; font-weight: 700;
  color: #1e3a2b; line-height: 1.4;
}
.reflexive-note {
  margin-top: 8px; padding: 6px 10px; background: #f3f0e8;
  border-radius: 6px; font-size: 13px; color: #8b6d1c;
}
.reflexive-note b { font-weight: 600; }
/* 听音区：大喇叭居中（与普通听写一致） */
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
.play-btn:hover { background: #d9f0e3; }
.play-btn:active { transform: scale(0.94); }
.play-hint { font-size: 13px; color: #98a2ac; }

.inputs {
  margin-top: 20px; display: flex; flex-direction: column; gap: 14px;
}
.input-label {
  display: block; margin-bottom: 6px; font-size: 13px; color: #55606a;
}

/* 拼写区小字提示（与普通拼写一致） */
.hint-line { margin-top: 6px; font-size: 13px; color: #98a2ac; }

/* 不规则题型：考点标签与 bello 型填空语境 */
.point-label {
  margin-top: 14px;
  display: inline-block;
  padding: 4px 12px;
  border: 1px solid #00934d;
  border-radius: 999px;
  background: #eef8f2;
  color: #00934d;
  font-size: 14px;
  font-weight: 600;
}
.bello-context {
  margin-top: 10px;
  font-size: 17px;
  font-weight: 600;
  color: #1e3a2b;
}

/* 听写区阶段提示（与普通听写一致：居中加粗） */
.stage-hint {
  margin-top: 14px; font-size: 14px; font-weight: 600;
  color: #55606a; text-align: center;
}
.label-hint { color: #98a2ac; font-weight: 400; font-size: 12px; }

/* 第二关顶部：已选对的释义（绿色确认条，与普通听写一致） */
.meaning-confirmed {
  padding: 10px 12px;
  border: 1px solid #00934d;
  border-radius: 8px;
  background: #eef8f2;
  color: #00934d;
  font-size: 14px;
  font-weight: 600;
}

/* 听写选项：两列卡片，点选高亮，序号角标对应键盘 1-4（与普通听写一致） */
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
.option-btn.selected .option-idx { background: #00934d; color: #fff; }
.option-btn:hover { border-color: #00934d; }
.option-btn.selected {
  border-color: #00934d;
  background: #eef8f2;
  color: #00934d;
  font-weight: 600;
}

/* 结果对照 */
.result {
  margin-top: 20px; border-top: 1px solid #e4e7ed;
  padding-top: 16px; display: flex; flex-direction: column; gap: 10px;
}
.verdict { font-size: 16px; font-weight: 700; }
.verdict.ok { color: #00934d; }
.verdict.bad { color: #cd212a; }
.compare-row {
  display: flex; align-items: center; gap: 10px;
  font-size: 15px; flex-wrap: wrap;
}
.compare-label { min-width: 92px; color: #98a2ac; font-size: 13px; }
.compare-input { color: #55606a; }
.compare-row.ok .compare-input { color: #00934d; }
.compare-row.bad .compare-input { color: #cd212a; text-decoration: line-through; }
.arrow { color: #c0c6cd; }
.compare-answer {
  font-weight: 700; color: #1e3a2b;
  display: inline-flex; align-items: center; gap: 6px;
}

/* 结束汇总 */
.summary-card { margin-top: 24px; }
.summary-head {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
}
.wrong-note { color: #55606a; font-size: 14px; padding: 8px 0; }
.all-right { color: #00934d; font-size: 15px; padding: 8px 0; }
</style>
