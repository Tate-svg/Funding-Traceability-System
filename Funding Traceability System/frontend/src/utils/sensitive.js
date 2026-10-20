// 敏感信息扫描与打码工具（规则与后端一致，按优先级、区间不重叠）

// 规则中文名
export const RULE_NAMES = {
  ID_CARD: '身份证号',
  PHONE: '手机号',
  BANK_CARD: '银行卡号',
  LONG_DIGIT: '长数字'
}

// 单片段打码
export function maskFragment(rule, text) {
  const len = text.length
  if (rule === 'PHONE') {
    return text.slice(0, 3) + '****' + text.slice(len - 4)
  }
  if (rule === 'ID_CARD') {
    return text.slice(0, 3) + '*'.repeat(len - 7) + text.slice(len - 4)
  }
  if (rule === 'BANK_CARD') {
    return text.slice(0, 4) + '*'.repeat(len - 8) + text.slice(len - 4)
  }
  // LONG_DIGIT
  return text.slice(0, 2) + '*'.repeat(len - 4) + text.slice(len - 2)
}

// 扫描文本，rules 形如 [{ rule, regex }]，按优先级顺序、区间不重叠命中
export function scanSensitive(text, rules) {
  const hits = []
  if (!text) return hits
  rules.forEach(({ rule, regex }) => {
    const re = new RegExp(regex.source, 'g')
    let m
    while ((m = re.exec(text)) !== null) {
      const start = m.index
      const end = start + m[0].length
      const overlap = hits.some(h => start < h.end && end > h.start)
      if (!overlap) {
        hits.push({ start, end, rule, text: m[0], masked: maskFragment(rule, m[0]) })
      }
      if (m[0].length === 0) re.lastIndex++
    }
  })
  hits.sort((a, b) => a.start - b.start)
  return hits
}

// 从后往前替换命中区间，返回打码后的全文
export function applyMasks(text, hits) {
  if (!text) return ''
  let result = text
  const sorted = (hits || []).slice().sort((a, b) => b.start - a.start)
  sorted.forEach(h => {
    result = result.slice(0, h.start) + h.masked + result.slice(h.end)
  })
  return result
}
