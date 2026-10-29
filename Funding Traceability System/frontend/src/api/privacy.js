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

// 当前登录管理员的敏感字段二次遮挡状态
export function getMaskStatus() {
  return request({
    url: '/privacy/mask/getMaskStatus',
    method: 'get'
  })
}

// 切换二次遮挡开关
export function switchMask(isSecondMask) {
  return request({
    url: '/privacy/mask/switchMask',
    method: 'post',
    params: { isSecondMask }
  })
}

// 创建敏感查看会话（限时查看，到期自动失效）
export function createViewSession(targetId) {
  return request({
    url: '/privacy/mask/createViewSession',
    method: 'post',
    params: { targetId }
  })
}

// 获取敏感详情（含二次遮挡处理与调取留痕）
export function getSensitiveDetail(sessionKey, targetId) {
  return request({
    url: '/privacy/mask/getSensitiveDetail',
    method: 'get',
    params: { sessionKey, targetId }
  })
}

// 关闭敏感查看会话
export function closeViewSession(sessionKey) {
  return request({
    url: '/privacy/mask/closeSession',
    method: 'post',
    params: { sessionKey }
  })
}
