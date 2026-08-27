<template>
  <div>
    <div class="toolbar">
      <h2 class="page-title">单词库</h2>
      <div class="filters">
        <el-select v-model="store.category" placeholder="全部分类" clearable style="width: 170px" @change="store.search()">
          <el-option v-for="c in store.categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="store.status" placeholder="全部状态" clearable style="width: 150px" @change="store.search()">
          <el-option label="未学" :value="0" />
          <el-option label="已抽取未完成" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
        <el-input
          v-model="store.keyword"
          placeholder="搜索单词 / 释义"
          clearable
          style="width: 220px"
          @keyup.enter="store.search()"
          @clear="store.search()"
        />
        <el-button type="primary" @click="store.search()">搜索</el-button>
        <el-button :loading="importing" @click="importWords">导入词库</el-button>
      </div>
    </div>

    <el-table :data="store.items" v-loading="store.loading" border stripe @row-click="(row) => openDetail(row.id)">
      <el-table-column prop="word" label="单词" width="150">
        <template #default="{ row }">
          <span class="table-word">{{ row.word }}</span>
          <el-tag v-if="row.irregular" size="small" type="warning" effect="plain" class="irr-tag">
            {{ row.irregular }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pos" label="词性" width="90" />
      <el-table-column prop="meaning" label="释义" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="完成次数" width="100" align="center">
        <template #default="{ row }">{{ row.extractCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.progressStatus === 2" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="row.progressStatus === 1" type="warning" size="small">已抽取未完成</el-tag>
          <el-tag v-else type="info" size="small">未学</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastExtractedAt" label="最近抽取" width="110" align="center" />
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="store.page"
        :page-size="store.size"
        :total="store.total"
        layout="total, prev, pager, next, jumper"
        @current-change="store.load()"
      />
    </div>

    <WordDetailDialog v-model="dialogVisible" :word-id="activeId" @updated="store.load()" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useLibraryStore } from '../stores/library'
import WordDetailDialog from '../components/WordDetailDialog.vue'

const store = useLibraryStore()
const dialogVisible = ref(false)
const activeId = ref(null)
const importing = ref(false)

function openDetail(id) {
  activeId.value = id
  dialogVisible.value = true
}

async function importWords() {
  importing.value = true
  try {
    const r = await api.importWords()
    ElMessage.success(`导入完成：新增 ${r.inserted} 个，跳过 ${r.skipped} 个`)
    await store.load()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  store.load()
  store.loadCategories()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.table-word {
  font-weight: 600;
  color: #1e3a2b;
  font-size: 15px;
}

/* 不规则标记小标签：跟在单词后 */
.irr-tag {
  margin-left: 6px;
  height: 20px;
  padding: 0 6px;
  line-height: 18px;
  font-size: 11px;
  vertical-align: middle;
}

/* 行 hover 提示可点击 */
:deep(.el-table__row) {
  cursor: pointer;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
