import request from '@/utils/request'

// 随机抽检：{ minAmount, maxAmount, ratio } → { batchNo, total, drawn }
export function drawReviewTasks(data) {
  return request({
    url: '/review-tasks/draw',
    method: 'post',
    data
  })
}

// 复核任务分页
export function listReviewTasks(params) {
  return request({
    url: '/review-tasks',
    method: 'get',
    params
  })
}

// 复核确认：{ result: 'PASS' | 'FAIL', remark }
export function finishReviewTask(id, data) {
  return request({
    url: '/review-tasks/' + id + '/finish',
    method: 'post',
    data
  })
}
