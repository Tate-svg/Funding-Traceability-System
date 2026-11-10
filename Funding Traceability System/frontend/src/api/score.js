import request from '@/utils/request'

export function scoreFundOptions(params) {
  return request({ url: '/scores/fund-options', method: 'get', params })
}

// 分页查询评分单
export function listScores(params) {
  return request({
    url: '/scores',
    method: 'get',
    params
  })
}

// 查询评分单详情（含 details）
export function getScore(id) {
  return request({
    url: '/scores/' + id,
    method: 'get'
  })
}

// 新增评分单（不传 totalScore，后端自动计算）
export function addScore(data) {
  return request({
    url: '/scores',
    method: 'post',
    data
  })
}

// 修改评分单
export function updateScore(id, data) {
  return request({
    url: '/scores/' + id,
    method: 'put',
    data
  })
}

// 删除评分单
export function deleteScore(id) {
  return request({
    url: '/scores/' + id,
    method: 'delete'
  })
}

// 导出评分单 Excel
export function exportScores() {
  return request({
    url: '/scores/export',
    method: 'get',
    responseType: 'blob'
  })
}
