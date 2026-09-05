import { ElMessage } from 'element-plus'

/**
 * 意大利语朗读：调用浏览器 Web Speech API，使用系统（Windows）已安装的意语语音。
 * 零后端依赖、离线可用；语音包在 Windows 设置 → 时间和语言 → 语音 中安装。
 */

let italianVoice = null
let voicesLoaded = false

function pickItalianVoice() {
  const voices = speechSynthesis.getVoices()
  if (!voices.length) {
    return null
  }
  voicesLoaded = true
  const itVoices = voices.filter((v) => (v.lang || '').toLowerCase().startsWith('it'))
  // 优先本地服务语音（Windows 安装的 Elsa/Cosimo 等），其次任意意大利语音色
  italianVoice = itVoices.find((v) => v.localService) || itVoices[0] || null
  return italianVoice
}

// Chrome/Edge 的语音列表为异步加载，提前缓存避免首次点击读不出声
if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
  pickItalianVoice()
  speechSynthesis.addEventListener?.('voiceschanged', pickItalianVoice)
}

/** 朗读意大利语文本（单词/例句）；未找到意语语音时提示 */
export function speakItalian(text) {
  if (!('speechSynthesis' in window)) {
    ElMessage.warning('当前浏览器不支持语音朗读，请使用 Edge 或 Chrome')
    return
  }
  const voice = italianVoice || pickItalianVoice()
  if (!voice && voicesLoaded) {
    ElMessage.warning('未找到意大利语语音，请在 Windows 设置中安装意语语音包')
    return
  }
  speechSynthesis.cancel()
  const utter = new SpeechSynthesisUtterance(text)
  if (voice) {
    utter.voice = voice
    utter.lang = voice.lang
  } else {
    utter.lang = 'it-IT' // 语音列表尚未就绪时先按语言朗读
  }
  utter.rate = 1.0 // 常速：0.9 时尾音发拖（慢速合成拖长尾字母），1.0 干脆且仍清晰
  speechSynthesis.speak(utter)
}
