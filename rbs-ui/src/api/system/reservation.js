import request from '@/utils/request'

// 查询会议室预订列表
export function listReservation(query) {
  return request({
    url: '/system/reservation/list',
    method: 'get',
    params: query
  })
}

// 查询会议室预订详细
export function getReservation(reservationId) {
  return request({
    url: '/system/reservation/' + reservationId,
    method: 'get'
  })
}

export function getReservationByapplicantName(params) {
  return request({
    url: `/system/reservation/applicant/${params.applicantName}`,
    method: 'get',
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize
    }
  })
}
// 新增会议室预订
export function addReservation(data) {
  return request({
    url: '/system/reservation',
    method: 'post',
    data: data
  })
}

// 修改会议室预订
export function updateReservation(data) {
  return request({
    url: '/system/reservation',
    method: 'put',
    data: data
  })
}

// 删除会议室预订
export function delReservation(reservationId) {
  return request({
    url: '/system/reservation/' + reservationId,
    method: 'delete'
  })
}
