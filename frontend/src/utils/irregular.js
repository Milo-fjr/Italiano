// 不规则/变位模式标签的视觉分级：
// 文案含「不规则」= 真异常（红标 danger）；「-isc 型 / 普通型」= -ire 变位模式提示（浅绿 primary，弱于红标）
export function irregularTagType(text) {
  return text && text.includes('不规则') ? 'danger' : 'primary'
}
