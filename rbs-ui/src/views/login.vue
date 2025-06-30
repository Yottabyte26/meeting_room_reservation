<template>
  <div class="login">
    <div class="login-container">
      <div class="login-image-container">
        <img src="../assets/images/huiyi.jpg" alt="Login Background" class="login-image">
        <h1 class="system-title">会议室预定管理系统</h1>
      </div>
      <div class="login-form-container">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
          <div class="login-title-container">
            <h3 class="title">登录</h3>
          </div>
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="请输入您的账号">
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
            <div class="goregister">
              <div style="float: left;" v-if="register">
                <span style="color: gray">没有账号，去</span>
                <router-link class="link-type" :to="'/register'">注册</router-link>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 表单数据
const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

// 表单验证规则
const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

// 响应式数据
const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(true)
const redirect = ref(undefined)
const showPassword = ref(false)
const eyeIconVisible = ref(true)

// 表单引用
const loginFormRef = ref(null)
// 监听路由变化
watch(
  () => route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
  },
  { immediate: true }
)

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

// 组件挂载时获取验证码和Cookie
onMounted(() => {
  getCode()
  getCookie()
})
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
  width: 850px;
  /* 设置固定宽度，避免左右布局问题 */
  margin: 0 auto;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-image-container {
  position: relative;
  width: 450px;
  height: 450px;
}

.login-image-container img {
  height: 100%;
  width: 100%;
  object-fit: cover;
  border-top-left-radius: 18px;
  border-bottom-left-radius: 18px;
}

.system-title {
  position: absolute;
  top: 63px;
  left: 50%;
  transform: translateX(-50%);
  color: #696666;
  font-size: 25px;
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
  margin-bottom: 20px;
  font-size: 23px;
}

.login-form {
  width: 400px;
  background: #ffffff;
  height: 450px;
  padding: 25px 25px 5px 25px;
  border-top-right-radius: 18px;
  border-bottom-right-radius: 18px;
}

.login-form .el-input {
  height: 40px;
}

.login-form .el-input input {
  height: 40px;
  border-radius: 20px / 40px;
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
</style>