<template>
  <div v-loading="store.loading">
    <h2 class="page-title">学习统计</h2>

    <!-- 数字卡片 -->
    <div v-if="store.stats" class="stat-cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.totalWords }}</div>
        <div class="stat-label">总词数</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.coveredWords }}</div>
        <div class="stat-label">已覆盖词数</div>
      </el-card>
      <el-card class="stat-card highlight">
        <div class="stat-value">{{ store.stats.coverageRate }}%</div>
        <div class="stat-label">覆盖率</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.todayCompleted }}/{{ store.stats.todayTotal }}</div>
        <div class="stat-label">本批已学</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.totalExtractCount }}</div>
        <div class="stat-label">累计完成次数</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.dueReviewCount }}</div>
        <div class="stat-label">今日到期复习</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ store.stats.spellDueCount }}</div>
        <div class="stat-label">今日到期拼写</div>
      </el-card>
    </div>

    <!-- SRS 盒子分布：简单数字 + 进度条 -->
    <el-card v-if="store.stats && store.stats.boxDistribution" class="box-card">
      <template #header>复习盒子分布（Leitner Box 0-5）</template>
      <div
        v-for="b in store.stats.boxDistribution"
        :key="b.label"
        class="box-row"
      >
        <span class="box-label">
          {{ b.label }}<template v-if="b.label === 'Box 0'">（未进入复习）</template>
        </span>
        <el-progress
          class="box-bar"
          :percentage="boxPercentage(b.count)"
          :format="() => `${b.count} 词`"
        />
      </div>
    </el-card>

    <!-- 图表 -->
    <div class="charts">
      <el-card>
        <template #header>各分类词数与完成情况</template>
        <div ref="categoryChartRef" class="chart"></div>
      </el-card>
      <el-card>
        <template #header>完成次数分布</template>
        <div ref="distChartRef" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useStatsStore } from '../stores/stats'

const store = useStatsStore()
const categoryChartRef = ref(null)
const distChartRef = ref(null)
let categoryChart = null
let distChart = null

/** 盒子词数占总词数的百分比（进度条宽度） */
function boxPercentage(count) {
  const total = store.stats?.totalWords || 0
  if (!total) return 0
  return Math.round((count * 1000) / total) / 10
}

function renderCharts() {
  const stats = store.stats
  if (!stats) return

  if (!categoryChart && categoryChartRef.value) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  if (categoryChart) {
    const categories = stats.categoryStats.map((c) => c.category)
    categoryChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['词数', '已完成'] },
      grid: { left: 50, right: 20, top: 40, bottom: 90 },
      xAxis: { type: 'category', data: categories, axisLabel: { rotate: 45, interval: 0, fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '词数', type: 'bar', data: stats.categoryStats.map((c) => c.total), itemStyle: { color: '#a0cfff' } },
        { name: '已完成', type: 'bar', data: stats.categoryStats.map((c) => c.completed), itemStyle: { color: '#67c23a' } }
      ]
    })
  }

  if (!distChart && distChartRef.value) {
    distChart = echarts.init(distChartRef.value)
  }
  if (distChart) {
    distChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 词 ({d}%)' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['35%', '65%'],
          label: { formatter: '{b}\n{c} 词' },
          data: stats.extractCountDistribution.map((d) => ({ name: d.label, value: d.count }))
        }
      ]
    })
  }
}

function resize() {
  categoryChart?.resize()
  distChart?.resize()
}

watch(
  () => store.stats,
  () => nextTick(renderCharts)
)

onMounted(() => {
  store.load().then(() => nextTick(renderCharts))
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  categoryChart?.dispose()
  distChart?.dispose()
})
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-card.highlight {
  background: linear-gradient(150deg, #e6f4ec 0%, #ffffff 80%);
  border-color: #b3dfcb;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #00934d;
}

.stat-card.highlight .stat-value {
  font-size: 34px;
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.charts {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
}

.chart {
  height: 420px;
}

/* SRS 盒子分布：每行标签 + 进度条 */
.box-card {
  margin-bottom: 20px;
}

.box-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
}

.box-label {
  width: 160px;
  font-size: 13px;
  color: #55606a;
  text-align: right;
  flex-shrink: 0;
}

.box-bar {
  flex: 1;
}
</style>
