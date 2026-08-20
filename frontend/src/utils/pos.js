/** 词性 → 标签颜色：名词蓝 / 动词绿 / 形容词橙 / 其他灰 */
export function posTagType(pos) {
  if (!pos) return 'info'
  if (pos.startsWith('s.')) return 'primary'
  if (pos.startsWith('v.')) return 'success'
  if (pos.includes('agg.')) return 'warning'
  return 'info'
}
