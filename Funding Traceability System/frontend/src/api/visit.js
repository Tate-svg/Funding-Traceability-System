import request from '@/utils/request'

// 分页查询回访记录
export function listVisits(params) {
  return request({
    url: '/visits',
    method: 'get',
    params
  })
}

// 查询单条回访记录
export function getVisit(id) {
  return request({
    url: '/visits/' + id,
    method: 'get'
  })
}

// 新增回访记录
export function addVisit(data) {
  return request({
    url: '/visits',
    method: 'post',
    data
  })
}

// 修改回访记录
export function updateVisit(id, data) {
  return request({
    url: '/visits/' + id,
    method: 'put',
    data
  })
}

// 删除回访记录
export function deleteVisit(id) {
  return request({
    url: '/visits/' + id,
    method: 'delete'
  })
}

// 导出回访记录 Excel
export function exportVisits() {
  return request({
    url: '/visits/export',
    method: 'get',
    responseType: 'blob'
  })
}

// 上传回访凭证材料（文本类后端自动脱敏；图片/文档类需人工脱敏后传 manuallyMasked=true）
export function uploadVisitMaterial(id, file, manuallyMasked) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('manuallyMasked', manuallyMasked ? 'true' : 'false')
  return request({
    url: '/visits/' + id + '/material',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}

// 下载脱敏后的回访凭证材料
export function downloadVisitMaterial(id) {
  return request({
    url: '/visits/' + id + '/material',
    method: 'get',
    responseType: 'blob',
    timeout: 60000
  })
}
