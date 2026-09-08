<template>
  <el-container class="app">
    <el-header class="header">
      <div class="header-inner">
        <div class="brand">
          <span class="flag" aria-hidden="true"><i /><i /><i /></span>
          <span class="brand-text">意大利语 A2 词汇学习</span>
        </div>
        <el-menu mode="horizontal" router :default-active="activePath" class="menu" :ellipsis="false">
          <el-menu-item index="/">学习模式</el-menu-item>
          <el-menu-item index="/quiz">测验模式</el-menu-item>
          <el-menu-item index="/spell">拼写模式</el-menu-item>
          <el-menu-item index="/dict">听写模式</el-menu-item>
          <el-menu-item index="/notebook">错题本</el-menu-item>
          <el-menu-item index="/library">单词库</el-menu-item>
          <el-menu-item index="/stats">统计</el-menu-item>
          <el-menu-item index="/settings">设置</el-menu-item>
        </el-menu>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activePath = computed(() => route.path)
</script>

<style>
/* ===== 主题色（意大利国旗绿）覆盖 Element Plus ===== */
:root {
  --it-green: #00934d;
  --it-red: #cd212a;
  --it-ink: #1e3a2b;
  --el-color-primary: #00934d;
  --el-color-primary-light-3: #4cb482;
  --el-color-primary-light-5: #80c9a6;
  --el-color-primary-light-7: #b3dfcb;
  --el-color-primary-light-8: #cce9da;
  --el-color-primary-light-9: #e6f4ec;
  --el-color-primary-dark-2: #00763e;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
  color: #2c3540;
  background: linear-gradient(180deg, #f2f7f4 0%, #f5f7fa 240px);
}

.app {
  min-height: 100vh;
}

/* ===== 顶部导航 ===== */
.header {
  height: 64px;
  padding: 0;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 0 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

/* 意大利国旗小旗标 */
.flag {
  display: inline-flex;
  width: 30px;
  height: 20px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
}

.flag i {
  flex: 1;
}

.flag i:nth-child(1) {
  background: #00934d;
}

.flag i:nth-child(2) {
  background: #ffffff;
}

.flag i:nth-child(3) {
  background: #cd212a;
}

.brand-text {
  font-size: 19px;
  font-weight: 700;
  color: var(--it-ink);
}

.menu {
  border-bottom: none;
  flex: 1;
  height: 100%;
}

.menu .el-menu-item {
  font-size: 15px;
  height: 100%;
}

.main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

/* ===== 通用页面标题 ===== */
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--it-ink);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title::before {
  content: '';
  width: 6px;
  height: 22px;
  border-radius: 3px;
  background: linear-gradient(180deg, #00934d, #4cb482);
}

/* 弹窗标题加大 */
.el-dialog__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--it-ink);
}

/* 卡片圆角统一 */
.el-card {
  border-radius: 12px;
}

/* ===== 不规则标签：实心红底白字加粗，高可见性 =====
   所有 type="danger" 的 el-tag 都是不规则标记，覆盖 Element Plus 默认浅底红字 */
.el-tag--danger {
  --el-tag-bg-color: var(--it-red);
  --el-tag-border-color: var(--it-red);
  --el-tag-text-color: #ffffff;
  font-weight: 600;
}
</style>
