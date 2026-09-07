<template>
  <div>
    <!-- 测验进度 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          测验模式
          <span class="date">到期复习 · 认识 +1 盒 / 不认识归 0</span>
        </h2>
        <div class="progress-line">
          <el-progress
            class="progress"
            :percentage="total ? Math.round(((total - words.length) * 100) / total) : 0"
            :stroke-width="18"
            :format="() => `${total - words.length} / ${total}`"
          />
          <span class="progress-hint">已测 / 到期总数 · 认识 {{ knowCount }} · 不认识 {{ forgotCount }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button round :loading="loading" @click="load">重新加载</el-button>
      </div>
    </div>

    <!-- 空状态：本次答完全部 -->
    <el-empty
      v-if="!loading && words.length === 0 && (knowCount > 0 || forgotCount > 0)"
      :description="`今日测验完成：认识 ${knowCount} 个 · 不认识 ${forgotCount} 个（不认识的明天回来）`"
    >
      <el-button round @click="load">重新加载</el-button>
    </el-empty>

    <!-- 空状态：没有到期词 -->
    <el-empty
      v-else-if="!loading && words.length === 0"
      :description="nextDueAt ? `暂无到期复习的单词，下一次复习：${nextDueAtText}` : '暂无到期复习的单词'"
    >
      <el-button type="primary" round @click="$router.push('/')">去学习模式背新词</el-button>
    </el-empty>

    <!-- 测验卡片：点击翻转核对释义 -->
    <div v-loading="loading" class="card-grid">
      <el-card
        v-for="w in words"
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { posTagType } from '../utils/pos'
import { speakItalian } from '../utils/tts'
import SoundButton from '../components/SoundButton.vue'

const loading = ref(false)
/** 到期词队列（服务端随机顺序；答过的移出队列，越测越少） */
const words = ref([])
/** 本次会话的到期总数（进度分母，重新加载后按剩余量重置） */
const total = ref(0)
/** 最近一次未来到期日（无到期词时的提示） */
const nextDueAt = ref(null)
const knowCount = ref(0)
const forgotCount = ref(0)
/** 已翻转（显示释义核对）的词 */
const flippedSet = reactive(new Set())

const nextDueAtText = computed(() => {
  if (!nextDueAt.value) return ''
  const d = new Date(nextDueAt.value)
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
})

async function load() {
  loading.value = true
  try {
    const r = await api.getQuizDue()
    words.value = r.words
    total.value = r.total
    nextDueAt.value = r.nextDueAt
    knowCount.value = 0
    forgotCount.value = 0
    flippedSet.clear()
  } finally {
    loading.value = false
  }
}

/** 点击卡片：翻转显示/收起释义（先回忆再核对） */
function toggleFlip(id) {
  if (flippedSet.has(id)) {
    flippedSet.delete(id)
  } else {
    flippedSet.add(id)
  }
}

/** 认识/不认识后自动朗读该词一遍，强化听力记忆（卡片随即移出队列，先取词再删） */
function speakAndRemove(id) {
  const w = words.value.find((x) => x.wordId === id)
  if (w) speakItalian(w.word)
  words.value = words.value.filter((x) => x.wordId !== id)
  flippedSet.delete(id)
}

/** 认识：盒 +1（不动完成次数），卡片移出队列 */
async function onKnow(id) {
  await api.quizKnow(id)
  speakAndRemove(id)
  knowCount.value++
}

/** 不认识：盒归 0 明天再测，卡片移出队列（刚看过释义，当场重测无意义） */
async function onForget(id) {
  await api.forgetWord(id)
  speakAndRemove(id)
  forgotCount.value++
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

.word-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 90, 45, 0.12);
}

/* 空间不足时标签组整体换行到单词下方，避免长标签挤压遮挡朗读按钮 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

/* 单词 + 朗读按钮的左簇 */
.word-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

/* 词性/不规则标签组：空间不足时换行，避免遮挡单词 */
.head-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

.word {
  font-size: 21px;
  font-weight: 700;
  color: #1e3a2b;
  letter-spacing: 0.3px;
}

.meaning {
  margin-top: 8px;
  font-size: 14px;
  color: #55606a;
  min-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-btns {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

/* 翻转：正面单词+词性，背面释义+分类 */
.flip-inner {
  position: relative;
  min-height: 56px;
  transition: transform 0.25s ease;
  transform-style: preserve-3d;
}

.flip-inner.flipped {
  transform: rotateY(180deg);
}

.flip-face {
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
}

.flip-back {
  position: absolute;
  inset: 0;
  transform: rotateY(180deg);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>
