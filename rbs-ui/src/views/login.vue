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
            <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="用户名/手机号/邮箱" prefix-icon="User">
            </el-input>
          </el-form-item>


          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              :type="showPassword ? 'text' : 'password'" 
              auto-complete="off"
              placeholder="请输入您的密码（5-20位字母加数字）" 
              @keyup.enter="handleLogin"
              prefix-icon="Lock"
            >
              <template #suffix>
                <span class="password-eye" @click="togglePasswordVisibility">
                  <svg-icon icon-class="eye-open" />
                  <span class="eye-slash" :class="{ 'active': !showPassword }"></span>
                </span>
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
    <el-dialog v-model="forgotPasswordVisible" title="重置密码" width="400px" :close-on-click-modal="false"
      custom-class="forgot-password-dialog" :show-close="true">
      <el-form :model="forgotForm" :rules="forgotRules" ref="forgotFormRef" class="forgot-form">
        <!-- 第一步：邮箱验证 -->
        <template v-if="currentStep === 1">
          <el-form-item prop="email" class="form-item">
            <el-input v-model="forgotForm.email" placeholder="请输入注册邮箱" @keyup.enter="sendResetCode">
              <template #prefix>
                <svg-icon icon-class="email" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code" class="form-item">
            <div class="code-input">
              <el-input v-model="forgotForm.code" placeholder="请输入6位验证码" maxlength="6"  @keyup.enter="nextStep"/>
              <el-button style="margin-left: 10px; width: 50%; height: 100%;" type="primary" :disabled="codeDisabled"
                @click="sendResetCode" class="code-button">
                {{ codeButtonText }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button type="primary" @click="nextStep" class="step-button" :loading="submitLoading"
              :disabled="!forgotForm.email || !forgotForm.code">
              下一步
            </el-button>
          </el-form-item>
        </template>

        <!-- 第二步：设置新密码 -->
        <template v-if="currentStep === 2">
          <el-form-item prop="newPassword" class="form-item">
            <el-input v-model="forgotForm.newPassword" :type="showPassword ? 'text' : 'password'"  placeholder="请设置新密码（5-20位字母加数字）"
              :show-password="showNewPwd" @click:suffix="showNewPwd = !showNewPwd" prefix-icon="Lock">
              <template #suffix>
                <span class="password-eye" @click="togglePasswordVisibility">
                  <svg-icon icon-class="eye-open" />
                  <span class="eye-slash" :class="{ 'active': !showPassword }"></span>
                </span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword" class="form-item">
            <el-input v-model="forgotForm.confirmPassword" :type="showPassword ? 'text' : 'password'"  placeholder="请确认新密码"
              :show-password="showConfirmPwd" @click:suffix="showConfirmPwd = !showConfirmPwd" 
              @keyup.enter="submitForgotForm" prefix-icon="Lock">
              <template #suffix>
                <span class="password-eye" @click="togglePasswordVisibility">
                  <svg-icon icon-class="eye-open" />
                  <span class="eye-slash" :class="{ 'active': !showPassword }"></span>
                </span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button type="primary" @click="submitForgotForm" class="step-button" :loading="submitLoading" :disabled="!forgotForm.newPassword || !forgotForm.confirmPassword || submitLoading">
              确认重置
            </el-button>
            <el-button type="default" @click="onCancel" class="step-button cancel-button">
              取消
            </el-button>
          </el-form-item>
        </template>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCodeImg, sendResetPwdEmailCode, resetPasswordByEmail, verifyEmailCode } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 模拟步骤流程
const currentStep = ref(1)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)
const submitLoading = ref(false)

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
  code: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const loginRules = {
  username: [{ required: true, trigger: "blur", message: "用户名不得为空！" }],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
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
const isMobile = ref(false)
// 验证码相关状态
const codeDisabled = ref(false)
const codeButtonText = ref('获取验证码')
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

// 获取图形验证码
const getCode = () => {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
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


// 发送验证码重置密码
const sendResetCode = () => {
  // 验证邮箱格式
  if (!forgotForm.value.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotForm.value.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  // 打印要发送的邮箱
  console.log('发送验证码的邮箱：', forgotForm.value.email)

  // 调用后端接口发送验证码
  codeDisabled.value = true
  codeButtonText.value = '发送中...'
  sendResetPwdEmailCode(forgotForm.value.email).then(() => {
    ElMessage.success('验证码已发送至您的邮箱，5分钟内有效')
    // 启动倒计时
    const timer = setInterval(() => {
      countdown--
      codeButtonText.value = `${countdown}秒后重发`
      if (countdown <= 0) {
        clearInterval(timer)
        codeDisabled.value = false
        codeButtonText.value = '获取验证码'
        countdown = 60 // 重置倒计时
      }
    }, 1000)
  }).catch(error => {
    console.error('验证码发送失败：', error)
    codeDisabled.value = false
    codeButtonText.value = '获取验证码'
    ElMessage.error(error.msg || '验证码发送失败，请重试')
  })
}

const nextStep = () => {
  forgotFormRef.value.validateField(['email', 'code'], (errors) => {
    if (Object.keys(errors).length === 0) {
      submitLoading.value = true;
      verifyEmailCode(forgotForm.value.email, forgotForm.value.code)
        .then(() => {
          currentStep.value = 2;
          nextTick(() => {
            const newPwdInput = document.querySelector('input[placeholder="请设置新密码（5-20位字母加数字）"]');
            newPwdInput?.focus();
          });
        })
        .catch((error) => {
          console.log("!!!!!!!!!!!", error.response)
          if (error.response) {
            ElMessage.error(error.response.data.msg || '验证码错误或已过期');
          }
        })
        .finally(() => {
          submitLoading.value = false;
        });
    } else {
      if (errors.email) {
        ElMessage.warning(errors.email);
      } else if (errors.code) {
        ElMessage.warning(errors.code);
      }
    }
  });
}
const onCancel = () => {
  submitLoading.value = false;
  forgotPasswordVisible.value = false
}

// 提交密码重置（核心接口调用）
const submitForgotForm = () => {
  forgotFormRef.value.validateField(['newPassword', 'confirmPassword'], (errors) => {
    if (Object.keys(errors).length === 0) {
      submitLoading.value = true
      // 构造参数调用后端重置接口
      const params = {
        email: forgotForm.value.email,
        code: forgotForm.value.code,
        newPassword: forgotForm.value.newPassword
      }
      resetPasswordByEmail(params).then(() => {
        submitLoading.value = false
        forgotPasswordVisible.value = false
        ElMessage.success('密码重置成功，请使用新密码登录')
        // 重置表单状态
        resetForgotForm()
      }).catch(error => {
        submitLoading.value = false
        ElMessage.error(error.msg || '密码重置失败，请重试')
      })
    } else {
      const firstError = Object.values(errors)[0]
      ElMessage.warning(firstError)
    }
  })
}

// 重置忘记密码表单
const resetForgotForm = () => {
  currentStep.value = 1
  forgotForm.value = {
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
  }
  codeDisabled.value = false
  codeButtonText.value = '获取验证码'
  countdown = 60
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
  width: 980px;
  margin: 0 auto;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-image-container {
  position: relative;
  width: 500px;
  height: 600px;
}

.login-image-container img {
  height: 600px;
  width: 500px;
  object-fit: cover;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.system-title {
  position: absolute;
  top: 83px;
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
  margin-top: 0px;
  margin-bottom: 30px;
}

.title {
  margin: 0px auto 40px auto;
  text-align: center;
  color: rgb(111, 113, 113);
  font-weight: bold;
  margin-bottom: 18px;
  font-size: 25px;
}

.login-form {
  width: 480px;
  background: #ffffff;
  height: 600px;
  padding: 85px 30px 40px; /* 关键修改：增加顶部内边距，让内容整体下移 */
  border-top-right-radius: 20px;
  border-bottom-right-radius: 20px; /* 补充右下角圆角，与注册页统一 */
  box-sizing: border-box; /* 确保内边距不会增加容器总高度 */
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
  margin-top: 19px;
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

.close-button {
  margin-top: 20px;
}
.password-eye {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 8px;
  cursor: pointer;
  color: #c0c4cc;
  transition: color 0.2s;
  
  &:hover {
    color: #909399;
  }
  
  .svg-icon {
    width: 18px;
    height: 18px;
  }
  
  .eye-slash {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 20px;
    height: 2px;
    background-color: transparent;
    transform: translate(-50%, -50%) rotate(45deg);
    transform-origin: center;
    transition: all 0.2s;
    
    &.active {
      background-color: #c0c4cc;
    }
  }
  
  &:hover .eye-slash.active {
    background-color: #909399;
  }
}

:deep(.el-dialog) {
  // 绝对定位实现居中
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  // 移除默认偏移量
  margin: 0 !important;
  // 确保弹窗在最上层
  z-index: 3000 !important;

  // 保持原有弹窗样式
  .el-dialog__body {
    padding: 20px 30px;
  }

  .el-dialog__header {
    border-bottom: 1px solid #f0f0f0;
    padding: 18px 20px;
  }

  .el-dialog__title {
    font-size: 16px;
    font-weight: 500;
  }
}
</style>