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

/**
 * 语音预定会议室（上传音频文件）
 * @param {File} file - 语音文件（wav格式）
 * @returns {Promise}
 */
export function voiceBookMeetingRoom(file) {
  const formData = new FormData()
  formData.append('file', file) // 后端要求的参数名是file
  return request({
    url: '/system/voice/book',
    method: 'post',
    data: formData,
    headers: {
      repeatSubmit: false},
    timeout: 30000 // 语音识别接口超时时间设为30秒
  })
}

/**
 * 解析语音指令（仅解析文本，不预定）
 * @param {String} voiceCommand - 语音指令文本
 * @returns {Promise}
 */
export function parseVoiceCommand(voiceCommand) {
  return request({
    url: '/system/voice/parseCommand',
    method: 'post',
    params: {
      voiceCommand: voiceCommand
    }
  })
}