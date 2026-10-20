import request from '@/utils/request'

// 上传学生佐证材料（.txt，multipart）
export function uploadAttachment(studentId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/students/' + studentId + '/attachments',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 确认材料脱敏结果入库
export function confirmAttachment(studentId, attId, maskedText) {
  return request({
    url: '/students/' + studentId + '/attachments/' + attId + '/confirm',
    method: 'post',
    data: { maskedText }
  })
}

// 学生材料列表
export function listAttachments(studentId) {
  return request({
    url: '/students/' + studentId + '/attachments',
    method: 'get'
  })
}
