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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '../stores/settings'

const store = useSettingsStore()
const saving = ref(false)

async function save() {
  saving.value = true
  try {
    await store.save()
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
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
</style>
