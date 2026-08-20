<template>
  <div>
    <!-- 批次进度 -->
    <div class="today-header">
      <div class="today-info">
        <h2 class="page-title today-title">
          学习批次
          <span class="date">抽取于 {{ store.date }}</span>
        </h2>
        <div class="progress-line">
          <el-progress
            class="progress"
            :percentage="store.total ? Math.round((store.completed * 100) / store.total) : 0"
            :stroke-width="18"
            :format="() => `${store.completed} / ${store.total}`"
          />
          <span class="progress-hint">已学 / 总数（未完成的词会一直保留到学完为止）</span>
        </div>
      </div>
      <el-button type="primary" round :loading="store.loading" @click="onRefreshBatch">换一批</el-button>
    </div>

    <el-empty v-if="!store.loading && store.words.length === 0" description="还没有学习批次">
      <el-button type="primary" round :loading="store.loading" @click="store.refreshBatch()">抽取第一批单词</el-button>
    </el-empty>

    <!-- 单词卡片 -->
    <div v-loading="store.loading" class="card-grid">
      <el-card
        v-for="w in store.words"
        :key="w.wordId"
        class="word-card"
        :class="{ done: w.dailyStatus === 1 }"
        shadow="hover"
        @click="openDetail(w.wordId)"
      >
        <div class="card-head">
          <span class="word">{{ w.word }}</span>
          <el-tag size="small" :type="posTagType(w.pos)">{{ w.pos || '-' }}</el-tag>
        </div>
        <div class="meaning">{{ w.meaning }}</div>
        <div class="card-foot">
          <el-tag size="small" type="info" effect="plain">{{ w.category }}</el-tag>
          <span class="count">已完成 {{ w.extractCount }} 次</span>
        </div>
        <div class="card-btns" @click.stop>
          <el-button v-if="w.dailyStatus === 0" type="primary" round size="small" @click="store.complete(w.wordId)">
            标记完成
          </el-button>
          <el-button v-else round size="small" @click="store.undo(w.wordId)">撤销完成</el-button>
        </div>
        <div v-if="w.dailyStatus === 1" class="done-badge">✓</div>
      </el-card>
    </div>

    <WordDetailDialog v-model="dialogVisible" :word-id="activeId" @updated="store.load()" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTodayStore } from '../stores/today'
import { posTagType } from '../utils/pos'
import WordDetailDialog from '../components/WordDetailDialog.vue'

const store = useTodayStore()
const dialogVisible = ref(false)
const activeId = ref(null)

function openDetail(id) {
  activeId.value = id
  dialogVisible.value = true
}

/** 换一批：还有未完成的词时先确认（未完成的会保留进新批次） */
async function onRefreshBatch() {
  const unfinished = store.total - store.completed
  if (unfinished > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unfinished} 个单词未完成，它们会保留到新批次继续学习。确定换一批吗？`,
        '换一批',
        { confirmButtonText: '换一批', cancelButtonText: '再学学', type: 'info' }
      )
    } catch {
      return
    }
  }
  await store.refreshBatch()
  ElMessage.success('已换一批')
}

onMounted(() => store.load())
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

/* 完成角标 */
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

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
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
