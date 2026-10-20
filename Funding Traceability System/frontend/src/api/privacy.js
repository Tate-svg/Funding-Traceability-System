import request from '@/utils/request'

// 提交隐私明文调取申请
export function applyPrivacyAccess(data) {
  return request({
    url: '/privacy/access/apply',
    method: 'post',
    data
  })
}

// 审批隐私调取申请（初审/终审）
export function approvePrivacyAccess(id, data) {
  return request({
    url: '/privacy/access/' + id + '/approve',
    method: 'post',
    data
  })
}

// 我的申请列表
export function listMyApplies() {
  return request({
    url: '/privacy/access/mine',
    method: 'get'
  })
}

// 待办申请列表（按角色返回初审/终审）
export function listTodoApplies() {
  return request({
    url: '/privacy/access/todo',
    method: 'get'
  })
}

// 按虚拟ID查看学生隐私档案（无有效审批则返回脱敏数据）
export function getPrivacyProfile(virtualId) {
  return request({
    url: '/privacy/profile/' + virtualId,
    method: 'get'
  })
}
