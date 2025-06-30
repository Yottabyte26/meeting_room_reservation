import request from '@/utils/request'

/**
 * 获取部门员工统计数据
 */
export function getDeptEmployeeStat() {
    return request({
        url: '/system/index/deptEmployeeStat',
        method: 'get'
    })
}

/**
 * 获取用户性别统计数据
 */
export function getGenderStat() {
    return request({
        url: '/system/index/genderStat',
        method: 'get'
    })
}

/**
 * 获取会议室预订次数统计数据
 */
export function getRoomReservationStat() {
    return request({
        url: '/system/index/roomReservationStat',
        method: 'get'
    })
}

/**
 * 获取当前用户的会议室预订统计数据
 * @returns {Promise} 返回统计数据对象
 */
export function getUserMeetingStats() {
    return request({
        url: '/system/reservation/userStats',
        method: 'get'
    })
}

// 获取总预订次数
export function getTotalReservations() {
    return request({
        url: '/system/room/totalReservations',
        method: 'get'
    })
}

// 获取会议室预订次数统计
export function getRoomReservations() {
    return request({
        url: '/system/room/roomReservations',
        method: 'get'
    })
}

