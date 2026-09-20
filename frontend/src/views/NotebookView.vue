<template>
  <div>
    <!-- 错题本头部 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          错题本
          <span class="date">答错自动进本 · 学会了移出 · SRS 明天照常回流</span>
        </h2>
        <div class="progress-line">
          <el-progress
            class="progress"
            :percentage="total ? Math.round((learnedCount * 100) / total) : 0"
            :stroke-width="18"
            :format="() => `${learnedCount} / ${total}`"
          />
          <span class="progress-hint">已学会 / 本内总数</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button v-if="total > 0" type="danger" plain round :loading="loading" @click="onLearnAll">
          全部学会
        </el-button>
        <el-button round :loading="loading" @click="load">重新加载</el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && words.length === 0"
      description="没有错词——测验/拼写答错的词会自动出现在这里"
    >
      <el-button round :loading="loading" @click="load">重新加载</el-button>
    </el-empty>

    <!-- 错词卡片：学会后置灰留卡可撤销，重新加载后消失 -->
    <div v-loading="loading" class="card-grid">
      <el-card
        v-for="w in words"
        :key="w.wordId"
        class="word-card"
        :class="{ done: learnedIds.has(w.wordId) }"
        shadow="hover"
        @click="openDetail(w.wordId)"
      >
        <div class="card-head">
          <div class="word-line">
            <span class="word">{{ w.word }}</span>
            <SoundButton :text="w.word" />
          </div>
          <span class="head-tags">
            <el-tag v-if="w.irregular" size="small" :type="irregularTagType(w.irregular)">{{ w.irregular }}</el-tag>
            <el-tag size="small" :type="posTagType(w.pos)">{{ w.pos || '-' }}</el-tag>
          </span>
        </div>
        <div class="meaning">{{ w.meaning }}</div>
        <div class="card-foot">
          <el-tag size="small" type="info" effect="plain">{{ w.category }}</el-tag>
          <span class="count">已完成 {{ w.extractCount ?? 0 }} 次</span>
        </div>

        <div class="card-btns" @click.stop>
          <el-button v-if="!learnedIds.has(w.wordId)" type="primary" round size="small" @click="onLearn(w.wordId)">
            学会了
          </el-button>
          <el-button v-else round size="small" @click="onUndo(w.wordId)">放回去</el-button>
        </div>
        <div v-if="learnedIds.has(w.wordId)" class="done-badge">✓</div>
      </el-card>
    </div>

    <WordDetailDialog v-model="dialogVisible" :word-id="activeId" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { posTagType } from '../utils/pos'
import { irregularTagType } from '../utils/irregular'
import { speakItalian } from '../utils/tts'
import WordDetailDialog from '../components/WordDetailDialog.vue'
import SoundButton from '../components/SoundButton.vue'

const loading = ref(false)
/** 本内词（服务端按进本先后稳定排序；学会的先置灰留卡防手滑，重新加载后消失） */
const words = ref([])
const total = ref(0)
/** 本次会话已标记学会的词（撤销用） */
const learnedIds = reactive(new Set())
const dialogVisible = ref(false)
const activeId = ref(null)

const learnedCount = computed(() => learnedIds.size)

async function load() {
  loading.value = true
  try {
    const r = await api.getNotebook()
    words.value = r.words
    total.value = r.total
    learnedIds.clear()
  } finally {
    loading.value = false
  }
}

/** 学会了：朗读该词强化记忆后移出错题本（卡片置灰留卡，重新加载后消失） */
async function onLearn(id) {
  const w = words.value.find((x) => x.wordId === id)
  if (w) speakItalian(w.word)
  await api.notebookLearn(id)
  learnedIds.add(id)
}

/** 放回去：撤销手滑（恢复进本） */
async function onUndo(id) {
  await api.notebookUndo(id)
  learnedIds.delete(id)
}

/** 全部学会：一键清空（防误触二次确认） */
async function onLearnAll() {
  const remaining = total.value - learnedIds.size
  try {
    await ElMessageBox.confirm(
      `将本内剩余 ${remaining} 个错词全部标记为学会并清空错题本。确定吗？`,
      '全部学会',
      { confirmButtonText: '全部学会', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const count = await api.notebookLearnAll()
  ElMessage.success(`已清空错题本（${count} 个）`)
  await load()
}

function openDetail(id) {
  activeId.value = id
  dialogVisible.value = true
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

.word-card.done {
  background: linear-gradient(160deg, #f0f9f3 0%, #ffffff 70%);
  border-color: #c9e8d5;
}

.word-card.done .word,
.word-card.done .meaning {
  color: #9aa8a0;
}

.word-card.done .word {
  text-decoration: line-through;
  text-decoration-thickness: 1.5px;
  text-decoration-color: #4cb482;
}

/* 已学会角标 */
.done-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #00934d;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 3px 8px rgba(0, 118, 62, 0.35);
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

.count {
  font-size: 12px;
  color: #98a2ac;
}

.card-btns {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
