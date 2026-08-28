<template>
  <div v-loading="store.loading">
    <h2 class="page-title">设置</h2>

    <el-card class="settings-card">
      <el-form label-width="140px">
        <el-form-item label="每批抽取数量">
          <el-input-number v-model="store.dailyCount" :min="1" :max="200" />
          <span class="tip">每批抽取供学习的单词数（默认 35）</span>
        </el-form-item>
        <el-form-item label="冷却天数">
          <el-input-number v-model="store.cooldownDays" :min="0" :max="90" />
          <span class="tip">最近 N 天内已抽取过的单词不再重复抽取（默认 7）</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="设置对下一次换一批生效：当前批次不受影响。"
        type="info"
        :closable="false"
        show-icon
      />
    </el-card>

    <el-card class="settings-card backup-card">
      <template #header>
        <span class="backup-title">词库备份</span>
      </template>
      <p class="backup-desc">
        把数据库中的全部单词（含手动编辑的释义、变位、例句）写回词库种子文件 vocab_data.json，
        之后随 git 提交保存。建议手动改动积累一段时间后点一次。
      </p>
      <el-button type="primary" plain :loading="exporting" @click="exportWords">导出词库备份</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useSettingsStore } from '../stores/settings'

const store = useSettingsStore()
const saving = ref(false)
const exporting = ref(false)

async function save() {
  saving.value = true
  try {
    await store.save()
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}

async function exportWords() {
  exporting.value = true
  try {
    const r = await api.exportWords()
    ElMessage.success(`已导出 ${r.total} 个单词到 vocab_data.json（记得 git 提交）`)
  } finally {
    exporting.value = false
  }
}

onMounted(() => store.load())
</script>

<style scoped>
.settings-card {
  max-width: 640px;
}

.tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.backup-card {
  margin-top: 20px;
}

.backup-title {
  font-weight: 600;
}

.backup-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: #55606a;
  line-height: 1.7;
}
</style>
