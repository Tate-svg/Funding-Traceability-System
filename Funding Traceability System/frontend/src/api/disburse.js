import request from '@/utils/request'

// 待拨付单据分页（拨付管理员）
export function listPendingDisburse(params) {
  return request({
    url: '/funds/pending-disburse',
    method: 'get',
    params
  })
}

// 确认拨付
export function disburseFund(id) {
  return request({
    url: '/funds/' + id + '/disburse',
    method: 'post'
  })
}
