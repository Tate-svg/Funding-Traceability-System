import request from '@/utils/request'

/**
 * 可信时间戳台账（仅 AUDIT_ADMIN / SUPERVISOR 可见）
 */
export function listTimestamps(params) {
  return request({
    url: '/timestamps',
    method: 'get',
    params
  })
}

/**
 * 哈希链核验：{valid, chainBrokenAt, anomalyCount, nodes:[{...record, hashValid}]}
 */
export function verifyTimestamps(params) {
  return request({
    url: '/timestamps/verify',
    method: 'get',
    params
  })
}

/**
 * 审计专用导出：PDF / 图片溯源单据（文件自带「账号 + 导出时间」水印）
 * @param {String} format pdf | png
 * @param {Object} params bizType / bizId / node / keyword
 */
export function exportAuditTrail(format, params) {
  return request({
    url: '/timestamps/export',
    method: 'get',
    params: Object.assign({}, params, { format }),
    responseType: 'blob'
  })
}
