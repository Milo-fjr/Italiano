<template>
  <button
    class="sound-btn"
    :class="{ small }"
    type="button"
    :title="`朗读 ${text}`"
    :aria-label="`朗读 ${text}`"
    @click.stop="speak"
  >
    <svg viewBox="0 0 24 24" :width="small ? 12 : 15" :height="small ? 12 : 15" fill="currentColor" aria-hidden="true">
      <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
    </svg>
  </button>
</template>

<script setup>
import { speakItalian } from '../utils/tts'

const props = defineProps({
  /** 要朗读的文本（单词或例句） */
  text: { type: String, required: true },
  /** 小尺寸：用于表格/行内场景 */
  small: { type: Boolean, default: false }
})

function speak() {
  speakItalian(props.text)
}
</script>

<style scoped>
.sound-btn {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  background: transparent;
  color: #00934d;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sound-btn:hover {
  background: #f0f9f3;
  border-color: #c9e8d5;
}

/* 表格/行内小尺寸 */
.sound-btn.small {
  width: 20px;
  height: 20px;
}

.sound-btn:active {
  transform: scale(0.92);
}
</style>
