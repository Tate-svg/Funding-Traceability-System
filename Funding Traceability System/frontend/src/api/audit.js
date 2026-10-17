import request from '@/utils/request'

// 待审核申请分页（审核管理员）
export function listPendingAudit(params) {
  return request({
    url: '/funds/pending-audit',
    method: 'get',
    params
  })
}

// 待审核数量（红色角标）
export function countPendingAudit() {
  return request({
    url: '/funds/pending-audit/count',
    method: 'get'
  })
}

// 审核：{ result: 'PASS' | 'REJECT', remark }
export function auditFund(id, data) {
  return request({
    url: '/funds/' + id + '/audit',
    method: 'post',
    data
  })
}
