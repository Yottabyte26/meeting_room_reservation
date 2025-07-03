import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

// 发送密码重置用的邮箱验证码
export function sendResetPwdEmailCode(email) {
  return request({
    url: '/system/user/sendResetPwdCode', // 后端发送重置密码验证码的接口
    headers: {
      isToken: false,
      repeatSubmit: true
    },
    method: 'post',
    data: { email }
  })
}

// 通过邮箱验证码重置密码
export function resetPasswordByEmail(data) {
  return request({
    url: '/system/user/resetPasswordByEmail',
    headers: {
      isToken: false, // 未登录状态下访问
      repeatSubmit: false
    },
    method: 'post',
    data: data // data包含：email、code、newPassword
  })
}