// 空白与真实0分必须分开；当前版本要求全部指标填写，暂不支持“不适用”。
export function scoreValidationError(details) {
  if (!Array.isArray(details) || !details.length) return '请先配置评分指标'
  const seen = new Set()
  for (const item of details) {
    if (!item || item.itemId === null || item.itemId === undefined || seen.has(String(item.itemId))) {
      return '评分指标不存在或重复，请刷新后重试'
    }
    seen.add(String(item.itemId))
    const weight = item.weight
    if (weight === null || weight === undefined || weight === '' || !Number.isFinite(Number(weight)) || Number(weight) < 0 || Number(weight) > 100) {
      return '指标权重须在0到100之间'
    }
    const score = item.itemScore
    if (score === null || score === undefined || score === '' || !Number.isFinite(Number(score)) || Number(score) < 0 || Number(score) > 100) {
      return '请填写全部指标得分（0-100），缺项不能生成综合评分'
    }
  }
  const weightTotal = details.reduce((sum, item) => sum + Number(item.weight), 0)
  if (Math.abs(weightTotal - 100) > 0.000001) return '指标权重合计须为100%，请先调整评分指标'
  return ''
}

export function scorePreview(details) {
  if (scoreValidationError(details)) return null
  // 与后端保持各分项先保留2位小数再汇总的顺序。
  return (details.reduce((sum, item) => sum + Math.round(Number(item.itemScore) * Number(item.weight)) / 100, 0)).toFixed(2)
}
