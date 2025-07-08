import request from '@/utils/request'

// 查询会议室信息列表
export function listRoom(query) {
  return request({
    url: '/system/room/list',
    method: 'get',
    params: query
  })
}

// 查询会议室信息详细
export function getRoom(roomId) {
  return request({
    url: '/system/room/' + roomId,
    method: 'get'
  })
}

// 新增会议室信息
export function addRoom(data) {
  return request({
    url: '/system/room',
    method: 'post',
    data: data
  })
}

// 修改会议室信息
export function updateRoom(data) {
  return request({
    url: '/system/room',
    method: 'put',
    data: data
  })
}


// 删除会议室信息
export function delRoom(roomId) {
  return request({
    url: '/system/room/' + roomId,
    method: 'delete'
  })
}
// 获取上传图片的token
export function getUploadToken() {
  return request({
    url: '/system/room/picture/upload-token',
    method: 'get'
  })
}

export function uploadRoomImage(formData) {
  return request({
    url: '/system/room/picture/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
     timeout: 30000 
  })
}