<template>
  <el-dialog
    v-model="visible"
    :title="detail ? detail.word : '单词详情'"
    width="660px"
    destroy-on-close
    class="detail-dialog"
    @open="load"
  >
    <div v-loading="loading">
      <template v-if="detail && !editing">
        <div class="basic">
          <div class="word-line">
            <span class="word">{{ detail.word }}</span>
            <SoundButton :text="detail.word" />
            <el-tag size="small" :type="posTagType(detail.pos)">{{ detail.pos || '-' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ detail.category }}</el-tag>
            <el-tag v-if="detail.progressStatus === 2" size="small" type="success">已完成</el-tag>
            <el-tag v-else-if="detail.progressStatus === 1" size="small" type="warning">已抽取未完成</el-tag>
            <el-tag v-else size="small" type="info">未学</el-tag>
          </div>
          <div class="meaning">{{ detail.meaning }}</div>
          <div class="meta">
            <span>累计完成：{{ detail.extractCount }} 次</span>
            <span v-if="detail.lastExtractedAt">最近抽取：{{ detail.lastExtractedAt }}</span>
            <span v-if="detail.completedAt">最近完成：{{ detail.completedAt.replace('T', ' ') }}</span>
            <span v-if="detail.box > 0">复习盒子：Box {{ detail.box }}<template v-if="detail.nextReviewAt">（下次 {{ detail.nextReviewAt }}）</template></span>
          </div>
        </div>

        <!-- 例句：意大利语例句 + 中文翻译 -->
        <div v-if="detail.example && (detail.example.it || detail.example.zh)" class="section">
          <h4>例句</h4>
          <div class="example">
            <div v-if="detail.example.it" class="example-it">{{ detail.example.it }}</div>
            <div v-if="detail.example.zh" class="example-zh">{{ detail.example.zh }}</div>
          </div>
        </div>

        <!-- 名词：定冠词/不定冠词与单复数、性别 -->
        <div v-if="isNoun" class="section">
          <h4>名词</h4>
          <div class="noun-forms">
            <div class="noun-form">
              <div class="noun-label">单数（定冠词）</div>
              <div class="noun-value">
                {{ detail.article ? detail.article + ' ' : '' }}{{ detail.word }}
              </div>
            </div>
            <div class="noun-arrow">→</div>
            <div class="noun-form">
              <div class="noun-label">复数（定冠词）</div>
              <div class="noun-value">
                {{ detail.articlePlural && detail.plural ? detail.articlePlural + ' ' + detail.plural : (detail.plural || detail.articlePlural || '—') }}
              </div>
            </div>
          </div>
          <div v-if="detail.gender || detail.articleIndefinite" class="noun-meta">
            <span v-if="detail.gender">
              性别：<b>{{ detail.gender === 'm' ? '阳性 (m)' : '阴性 (f)' }}</b>
            </span>
            <span v-if="detail.articleIndefinite">
              不定冠词：<b>{{ detail.articleIndefinite + ' ' + detail.word }}</b>
            </span>
          </div>
        </div>

        <!-- 形容词：性数变化四格 -->
        <div v-if="adjRows.length" class="section">
          <h4>形容词变化</h4>
          <el-table :data="adjRows" size="small" border>
            <el-table-column prop="label" label="性 / 数" width="120" align="center" />
            <el-table-column prop="form" label="形式" />
          </el-table>
        </div>

        <!-- 动词：四时态变位（现在时/近过去时/未完成过去时/简单将来时） -->
        <div v-if="hasConjugation" class="section">
          <h4>动词变位</h4>
          <el-tabs v-model="activeTense" type="card">
            <el-tab-pane
              v-for="(forms, tense) in detail.conjugation"
              :key="tense"
              :label="tenseLabel(tense)"
              :name="tense"
            >
              <el-table :data="conjugationRows(forms)" size="small" border>
                <el-table-column prop="person" label="人称" width="120" />
                <el-table-column prop="form" label="变位" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="dialog-actions">
          <el-button v-if="detail.progressStatus !== 2" type="primary" @click="onComplete">
            标记完成
          </el-button>
          <el-button v-else @click="onUndo">撤销完成</el-button>
          <el-button type="primary" plain @click="startEdit">编辑</el-button>
        </div>
      </template>

      <!-- 编辑模式 -->
      <template v-if="detail && editing">
        <el-form label-width="110px">
          <el-form-item label="释义">
            <el-input v-model="form.meaning" />
          </el-form-item>
          <el-form-item label="词性">
            <el-input v-model="form.pos" placeholder="如 s.m. / s.f. / v. / agg." />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="form.category" />
          </el-form-item>

          <el-divider content-position="left">例句（可空）</el-divider>
          <el-form-item label="意大利语例句">
            <el-input v-model="form.exampleIt" placeholder="如 Faccio una foto." />
          </el-form-item>
          <el-form-item label="中文翻译">
            <el-input v-model="form.exampleZh" placeholder="如 我拍张照。" />
          </el-form-item>

          <template v-if="form.pos && form.pos.startsWith('s.')">
            <el-form-item label="名词性别">
              <el-select v-model="form.gender" clearable placeholder="选择（可留空）">
                <el-option label="m（阳性）" value="m" />
                <el-option label="f（阴性）" value="f" />
              </el-select>
            </el-form-item>
            <el-form-item label="单数定冠词">
              <el-input v-model="form.article" placeholder="如 il / lo / la / l'" />
            </el-form-item>
            <el-form-item label="复数形式">
              <el-input v-model="form.plural" placeholder="如 libri" />
            </el-form-item>
          </template>

          <template v-if="form.pos && form.pos.includes('agg.')">
            <el-divider content-position="left">形容词变化</el-divider>
            <el-form-item label="阳性单数">
              <el-input v-model="form.adjForms.ms" placeholder="如 bello" />
            </el-form-item>
            <el-form-item label="阴性单数">
              <el-input v-model="form.adjForms.fs" placeholder="如 bella" />
            </el-form-item>
            <el-form-item label="阳性复数">
              <el-input v-model="form.adjForms.mp" placeholder="如 belli" />
            </el-form-item>
            <el-form-item label="阴性复数">
              <el-input v-model="form.adjForms.fp" placeholder="如 belle" />
            </el-form-item>
          </template>

          <template v-if="form.pos && form.pos.startsWith('v.')">
            <el-divider content-position="left">动词变位（四时态）</el-divider>
            <el-tabs v-model="editTense" type="card">
              <el-tab-pane
                v-for="t in tenses"
                :key="t.key"
                :label="t.label"
                :name="t.key"
              >
                <el-form-item v-for="p in persons" :key="p" :label="p">
                  <el-input v-model="form.conjugation[t.key][p]" :placeholder="`${t.label} · ${p}`" />
                </el-form-item>
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-form>
        <div class="dialog-actions">
          <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
          <el-button @click="editing = false">取消</el-button>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { posTagType } from '../utils/pos'
import SoundButton from './SoundButton.vue'

const props = defineProps({
  modelValue: Boolean,
  wordId: {
    type: Number,
    default: null
  }
})
const emit = defineEmits(['update:modelValue', 'updated'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const persons = ['io', 'tu', 'lui/lei', 'noi', 'voi', 'loro']
const adjLabels = { ms: '阳性单数', fs: '阴性单数', mp: '阳性复数', fp: '阴性复数' }
const tenses = [
  { key: 'present', label: '现在时' },
  { key: 'passatoProssimo', label: '近过去时' },
  { key: 'imperfetto', label: '未完成过去时' },
  { key: 'futuro', label: '简单将来时' }
]
const tenseLabelMap = Object.fromEntries(tenses.map((t) => [t.key, t.label]))
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const detail = ref(null)
const activeTense = ref('present')
const editTense = ref('present')
const form = reactive({
  meaning: '',
  pos: '',
  category: '',
  gender: '',
  article: '',
  plural: '',
  exampleIt: '',
  exampleZh: '',
  conjugation: {},
  adjForms: {}
})

// 词性 → 标签颜色见 utils/pos.js

const isNoun = computed(() => {
  const d = detail.value
  return d && d.pos && d.pos.startsWith('s.') && (d.article || d.plural || d.articlePlural)
})

const hasConjugation = computed(() => {
  const c = detail.value && detail.value.conjugation
  return c && Object.keys(c).length > 0
})

function tenseLabel(key) {
  return tenseLabelMap[key] || key
}

const conjugationRows = (forms) => {
  if (!forms) return []
  return persons.map((p) => ({ person: p, form: forms[p] || '—' }))
}

const adjRows = computed(() => {
  if (!detail.value || !detail.value.adjForms) return []
  return Object.entries(detail.value.adjForms)
    .filter(([, form]) => form)
    .map(([key, form]) => ({ label: adjLabels[key] || key, form }))
})

async function load() {
  if (!props.wordId) return
  loading.value = true
  editing.value = false
  try {
    detail.value = await api.getWord(props.wordId)
    activeTense.value = 'present'
  } finally {
    loading.value = false
  }
}

function startEdit() {
  const d = detail.value
  form.meaning = d.meaning || ''
  form.pos = d.pos || ''
  form.category = d.category || ''
  form.gender = d.gender || ''
  form.article = d.article || ''
  form.plural = d.plural || ''
  form.exampleIt = d.example?.it || ''
  form.exampleZh = d.example?.zh || ''
  form.conjugation = {}
  tenses.forEach((t) => {
    form.conjugation[t.key] = {}
    persons.forEach((p) => {
      form.conjugation[t.key][p] = d.conjugation?.[t.key]?.[p] || ''
    })
  })
  form.adjForms = {}
  Object.keys(adjLabels).forEach((k) => {
    form.adjForms[k] = d.adjForms?.[k] || ''
  })
  editTense.value = 'present'
  editing.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    const body = {
      meaning: form.meaning,
      pos: form.pos,
      category: form.category,
      // 例句：全空则提交空对象（后端清空）
      example: { it: form.exampleIt.trim(), zh: form.exampleZh.trim() }
    }
    // 按词性提交对应语法字段
    if (form.pos && form.pos.startsWith('s.')) {
      body.gender = form.gender
      body.article = form.article
      body.plural = form.plural
    }
    if (form.pos && form.pos.includes('agg.')) {
      const adjForms = {}
      let hasAdj = false
      Object.keys(adjLabels).forEach((k) => {
        if (form.adjForms[k]) {
          adjForms[k] = form.adjForms[k]
          hasAdj = true
        }
      })
      body.adjForms = hasAdj ? adjForms : {}
    }
    if (form.pos && form.pos.startsWith('v.')) {
      const conjugation = {}
      tenses.forEach((t) => {
        const personsMap = {}
        let hasTense = false
        persons.forEach((p) => {
          const v = form.conjugation?.[t.key]?.[p]
          if (v) {
            personsMap[p] = v
            hasTense = true
          }
        })
        if (hasTense) {
          conjugation[t.key] = personsMap
        }
      })
      body.conjugation = conjugation
    }
    detail.value = await api.updateWord(props.wordId, body)
    editing.value = false
    ElMessage.success('保存成功')
    emit('updated')
  } finally {
    saving.value = false
  }
}

async function onComplete() {
  detail.value = await api.completeWord(props.wordId)
  ElMessage.success('已完成')
  emit('updated')
}

async function onUndo() {
  detail.value = await api.undoWord(props.wordId)
  ElMessage.success('已撤销')
  emit('updated')
}

defineExpose({ reload: load })
</script>

<style scoped>
.basic {
  padding-bottom: 4px;
}

.word-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.word {
  font-size: 28px;
  font-weight: 700;
  color: #1e3a2b;
  letter-spacing: 0.5px;
}

.meaning {
  margin-top: 10px;
  font-size: 15px;
  color: #55606a;
}

.meta {
  margin-top: 10px;
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #98a2ac;
}

.section {
  margin-top: 20px;
}

.section h4 {
  margin-bottom: 10px;
  color: #1e3a2b;
  font-size: 14px;
  padding-left: 10px;
  border-left: 3px solid #00934d;
  line-height: 1.4;
}

/* 名词单复数展示 */
.noun-forms {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #f4f9f6;
  border-radius: 10px;
  padding: 14px 20px;
}

.noun-form {
  flex: 1;
  text-align: center;
}

.noun-label {
  font-size: 12px;
  color: #98a2ac;
  margin-bottom: 6px;
}

.noun-value {
  font-size: 20px;
  font-weight: 600;
  color: #1e3a2b;
}

.noun-arrow {
  font-size: 22px;
  color: #00934d;
  font-weight: 700;
}

.noun-meta {
  margin-top: 10px;
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #55606a;
}

.noun-meta b {
  color: #1e3a2b;
}

/* 例句展示 */
.example {
  background: #f4f9f6;
  border-radius: 10px;
  padding: 14px 20px;
}

.example-it {
  font-size: 17px;
  font-weight: 600;
  color: #1e3a2b;
  letter-spacing: 0.3px;
}

.example-zh {
  margin-top: 6px;
  font-size: 13px;
  color: #55606a;
}

.dialog-actions {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
}
</style>
