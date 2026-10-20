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
