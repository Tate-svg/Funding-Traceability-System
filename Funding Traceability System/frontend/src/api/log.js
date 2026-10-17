import request from '@/utils/request'

// 操作日志分页（只读存证）
export function listOperationLogs(params) {
  return request({
    url: '/operation-logs',
    method: 'get',
    params
  })
}
