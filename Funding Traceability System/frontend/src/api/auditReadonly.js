import request from '@/utils/request'

export function auditReadonlyList(resource, params) {
  return request({ url: '/audit-readonly/' + resource, method: 'get', params })
}

export function auditReadonlyScore(id) {
  return request({ url: '/audit-readonly/scores/' + id, method: 'get' })
}

export function auditReadonlyVerify(fundId) {
  return request({ url: '/audit-readonly/verify', method: 'get', params: { fundId } })
}
