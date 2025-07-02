<template>
  <div class="login">
    <div class="login-container">
      <div class="login-image-container">
        <img src="../assets/images/huiyi.jpg" alt="Login Background" class="login-image">
        <h1 class="system-title">会议室预定系统</h1>
      </div>
      <div class="login-form-container">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
          <div class="login-title-container">
            <h3 class="title">登录</h3>
          </div>
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="用户名/手机号/邮箱">
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" :type="showPassword ? 'text' : 'password'" auto-complete="off"
              placeholder="请输入您的密码（6-16位字母加数字）" @keyup.enter="handleLogin">
              <template #suffix>
                <i :class="showPassword ? 'fas fa-eye' : 'fas fa-eye-slash'" style="margin-top:8px;font-size:18px;"
                  @click="togglePasswordVisibility" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input v-model="loginForm.code" size="large" auto-complete="off" placeholder="验证码" style="width: 63%"
              @keyup.enter="handleLogin">
              <template #prefix>
                <svg-icon icon-class="validCode" class="el-input__icon input-icon" />
              </template>
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img" />
            </div>
          </el-form-item>

          <el-form-item style="width:100%;">
            <el-button :loading="loading" size="medium" type="primary"
              style="width:100%;background:rgb(111, 113, 113);border-radius: 35px" @click.prevent="handleLogin">
              <span v-if="!loading" style="font-size: 12px">登 录</span>
              <span v-else style="font-size: 12px">登 录 中...</span>
            </el-button>

            <div class="forgot-password">
              <span class="link-type" @click="forgotPasswordVisible = true">已有账号，忘记密码？</span>
            </div>

            <div class="wechat-login-container">
              <div class="login-divider">
                <span class="divider-line"></span>
                <span class="divider-text">使用其他账号登录</span>
                <span class="divider-line"></span>
              </div>
              
              <div class="wechat-login-btn" @click="wechatLoginVisible = true">
                <svg-icon icon-class="wechat" class="wechat-icon" />
                <span>微信登录</span>
              </div>
            </div>
          </el-form-item>
          
          <div class="goregister">
            <div style="float: left;" v-if="register">
              <span style="color: gray">没有账号，去</span>
              <router-link class="link-type" :to="'/register'">注册</router-link>
            </div>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <el-dialog 
      v-model="forgotPasswordVisible" 
      title="找回密码" 
      width="400px"
      :close-on-click-modal="false">
      <el-form :model="forgotForm" :rules="forgotRules" ref="forgotFormRef">
        <el-form-item prop="email">
          <el-input v-model="forgotForm.email" placeholder="请输入注册邮箱">
            <template #prefix>
              <svg-icon icon-class="email" />
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="code">
          <div class="code-input">
            <el-input v-model="forgotForm.code" placeholder="验证码" />
            <el-button 
              :disabled="codeDisabled" 
              @click="sendCode"
              class="code-button">
              {{ codeButtonText }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="submitForgotForm"
            class="submit-button">
            提交
          </el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 微信登录弹窗 -->
    <el-dialog 
      v-model="wechatLoginVisible" 
      title="微信登录" 
      width="350px"
      :show-close="false"
      :close-on-click-modal="false">
      <div class="wechat-login-content">
        <div class="qrcode-container" v-if="!isMobile">
          <div class="qrcode-placeholder">
            <svg-icon icon-class="wechat" class="qrcode-icon" />
            <p>微信扫码登录</p>
          </div>
          <p class="tip">请使用微信扫描二维码</p>
        </div>
        
        <div class="mobile-tip" v-else>
          <svg-icon icon-class="wechat" class="wechat-icon" />
          <p>请在微信客户端打开链接</p>
        </div>
        
        <el-button 
          type="text" 
          @click="wechatLoginVisible = false"
          class="close-button">
          返回其他登录方式
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 登录表单数据
const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

// 忘记密码表单数据
const forgotForm = ref({
  email: '',
  code: ''
})

// 表单验证规则
const loginRules = {
  username: [{ required: true, trigger: "blur", message: "用户名不得为空！" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 响应式数据
const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(true)
const redirect = ref(undefined)
const showPassword = ref(false)
const eyeIconVisible = ref(true)
const forgotPasswordVisible = ref(false)
const wechatLoginVisible = ref(false)
const codeDisabled = ref(false)
const codeButtonText = ref('获取验证码')
const isMobile = ref(false)
let countdown = 60

// 表单引用
const loginFormRef = ref(null)
const forgotFormRef = ref(null)

// 监听路由变化
watch(
  () => route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
  },
  { immediate: true }
)

// 组件挂载时获取验证码和Cookie
onMounted(() => {
  getCode()
  getCookie()
  checkMobile()
})

// 检查是否为移动设备
const checkMobile = () => {
  isMobile.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

// 切换密码可见性
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
  eyeIconVisible.value = showPassword.value
}

// 获取验证码
const getCode = () => {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

// 发送验证码（忘记密码）
const sendCode = () => {
  // 实际应调用API发送验证码
  codeDisabled.value = true
  const timer = setInterval(() => {
    countdown--
    codeButtonText.value = `${countdown}秒后重新获取`
    if (countdown <= 0) {
      clearInterval(timer)
      codeDisabled.value = false
      codeButtonText.value = '获取验证码'
      countdown = 60
    }
  }, 1000)
}

// 提交忘记密码表单
const submitForgotForm = () => {
  forgotFormRef.value?.validate(valid => {
    if (valid) {
      // 这里添加实际提交逻辑
      forgotPasswordVisible.value = false
      ElMessage.success('密码重置邮件已发送，请查收')
    }
  })
}

// 获取Cookie数据
const getCookie = () => {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

// 处理登录
const handleLogin = () => {
  userStore.setUser(loginForm.value.username, loginForm.value.uuid);
  loginFormRef.value?.validate(valid => {
    if (valid) {
      loading.value = true
      // 处理记住密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }

      // 调用登录API
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}
</script>

<style lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login.jpg");
  background-size: cover;
  background-color: rgba(0, 0, 0, 0.5);
  background-blend-mode: multiply;
}

.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 950px;
  margin: 0 auto;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-image-container {
  position: relative;
  width: 500px;
  height: 550px;
}

.login-image-container img {
  height: 550px;
  width: 500px;
  object-fit: cover;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.system-title {
  position: absolute;
  top:85px; 
  left: 50%;
  transform: translateX(-50%);
  color: #696666;
  font-size: 28px;
  font-weight: bold;
  width: fit-content;
  margin: 0;
  padding: 0;
}

.login-title-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  margin-bottom: 10px;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: rgb(111, 113, 113);
  font-weight: bold;
  margin-bottom: 25px;
  font-size: 25px;
}

.login-form {
  width: 450px;
  background: #ffffff;
  height: 550px;
  padding:40px 30px;
  border-top-right-radius: 20px;
  border-bottom-right-radius: px;
}

.login-form .el-input {
  height: 42px;
}

.login-form .el-input input {
  height: 40px;
  border-radius: 20px / 40px;
}

.forgot-password {
  text-align: center;
  margin-top: 10px;
  margin-left: 125px;
  .link-type {
    color: rgb(110, 133, 213);
    cursor: pointer;
    text-decoration: none;
    font-size: 13px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.wechat-login-container {
  text-align: center;
  width: 100%;
  
  .login-divider {
    display: flex;
    align-items: center;
    margin: 10px 0;
    
    .divider-line {
      flex: 1;
      height: 1px;
      background-color: #dcdfe6;
    }
    
    .divider-text {
      padding: 0 10px;
      color: #909399;
      font-size: 12px;
    }
  }
  
  .wechat-login-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #07C160;
    padding: 5px 15px;
    border-radius: 4px;
    margin-top: 5px;
    
    .wechat-icon {
      width: 20px;
      height: 20px;
      margin-right: 8px;
    }
    
    span {
      font-size: 12px;
    }
    
    &:hover {
      background-color: rgba(7, 193, 96, 0.1);
    }
  }
}

.register-bottom {
  text-align: center;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
}

.login-code {
  width: 33%;
  height: 40px;
  float: right;
}

.login-code img {
  cursor: pointer;
  vertical-align: middle;
  height: 40px;
  padding-left: 12px;
}

.goregister {
  margin-top: 18px;
  font-size: 14px;
}

.link-type {
  color: rgb(75, 109, 231);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

/* 弹窗相关样式 */
.code-input {
  display: flex;
  gap: 10px;
}

.code-button {
  width: 120px;
}

.submit-button {
  width: 100%;
  margin-top: 10px;
}

.wechat-login-content {
  text-align: center;
  padding: 10px;
}

.qrcode-placeholder {
  width: 220px;
  height: 220px;
  margin: 0 auto 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  transition: all 0.3s ease;
  
  .qrcode-icon {
    font-size: 80px;
    color: #07C160;
    margin-bottom: 15px;
  }
  
  p {
    color: #333;
    font-size: 16px;
  }
  
  &:hover {
    box-shadow: 0 0 10px rgba(7, 193, 96, 0.2);
  }
}

.tip {
  color: #888;
  margin-bottom: 20px;
}

.mobile-tip {
  padding: 30px 0;
  
  .wechat-icon {
    font-size: 60px;
    color: #07C160;
    margin-bottom: 15px;
  }
}

.close-button {
  margin-top: 20px;
}

/* 全局弹窗样式 */
:deep(.el-dialog) {
  border-radius: 12px;
  
  .el-dialog__header {
    border-bottom: 1px solid #f0f0f0;
    margin-right: 0;
  }
  
  .el-dialog__body {
    padding: 20px;
  }
}
</style>