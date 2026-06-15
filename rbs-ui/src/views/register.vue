<template>
  <div class="register">
    <div class="register-container">
      <div class="register-image-container">
        <img src="../assets/images/huiyi.jpg" alt="Login Background" class="login-image">
        <h3 class="system-title">会议室预约系统</h3>
      </div>

      <div class="register-form-container">
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="register-form">
          <div class="register-title-container">
            <h3 class="title" style="font-size: 25px">注册</h3>
          </div>
          <!-- 账号输入 -->
          <el-form-item prop="username">
            <el-input v-model="registerForm.username" type="text" auto-complete="off" placeholder="请输入账号"  prefix-icon="User">
            </el-input>
          </el-form-item>

          <!-- 邮箱输入 -->
          <el-form-item prop="email">
            <el-input v-model="registerForm.email" type="email" auto-complete="off" placeholder="请输入邮箱">
              <template #prefix>
                <svg-icon icon-class="email" class="el-input__icon input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <!-- 密码输入 -->
          <el-form-item prop="password">
            <el-input 
              v-model="registerForm.password" 
              :type="showPassword ? 'text' : 'password'" 
              auto-complete="off" 
              placeholder="请输入密码(5-20位数字)"
              @keyup.enter.native="handleRegister"
              prefix-icon="Lock"
            >
              <template #suffix>
                <span class="password-eye" @click.stop="togglePasswordVisibility">
                  <svg-icon icon-class="eye-open" />
                  <span class="eye-slash" :class="{ 'active': !showPassword }"></span>
                </span>
              </template>
            </el-input>
          </el-form-item>

          <!-- 确认密码输入 -->
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword" 
              :type="showConfirmPassword ? 'text' : 'password'" 
              auto-complete="off" 
              placeholder="请再次输入密码(5-20位数字)"
              @keyup.enter="handleSendCode"
              prefix-icon="Lock"
            >
              <template #suffix>
                <span class="password-eye" @click.stop="toggleConfirmPasswordVisibility">
                  <svg-icon icon-class="eye-open" />
                  <span class="eye-slash" :class="{ 'active': !showConfirmPassword }"></span>
                </span>
              </template>
            </el-input>
          </el-form-item>

          <!-- 邮箱验证码区域 -->
          <el-form-item prop="code">
            <el-input v-model="registerForm.code" autocomplete="off" placeholder="输入6位验证码" style="width: 63%"
              @keyup.enter="handleRegister">
              <template #prefix>
                <svg-icon icon-class="validCode" class="el-input__icon input-icon" />
              </template>
            </el-input>
            <div class="register-code">
              <el-button style="margin-left: 10px; width: 100%; height: 100%;" type="primary" :disabled="isCodeDisabled || !registerForm.email.trim()"  @click="handleSendCode"> {{ codeButtonText }}</el-button>
            </div>
          </el-form-item>
          <div class="checkbox" style="font-size: 15px">
            <input type="checkbox" class="check-box" v-model="registerForm.agreeTerms">
            <span style="color: gray">注册即代表您已同意</span>
            <a class="link-type" href="#" target="_blank">《服务协议》</a>及
            <a class="link-type" href="#" target="_blank">《隐私政策》</a>
          </div>
          <el-form-item style="width:100%;">
            <el-button :loading="loading" size="medium" type="primary"
              style="width:100%;background:rgb(111, 113, 113);margin-top:20px;border-radius: 35px"
              @click.native.prevent="handleRegister">
              <span v-if="!loading">注 册</span>
              <span v-else>注 册 中...</span>
            </el-button>
            <div class="gologin">
              <div style="text-align: center;font-size: 14px;">
                <span style="color: gray">已有账号，立即</span>
                <router-link class="link-type" :to="'/login'">登录</router-link>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { register, sendRegisterCode } from "@/api/login";

const router = useRouter()

// 创建表单引用
const registerFormRef = ref(null)

//密码隐藏显示
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value;
};

// 表单数据
const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  email: "",
  uuid: "",
  deptName: "",
  agreeTerms: false  // 新增属性，用于跟踪是否同意服务协议和隐私政策
})

// 验证码按钮状态
const isCodeDisabled = ref(false)
const codeButtonText = ref("发送验证码")
// 加载状态
const loading = ref(false)
const codeSendTime = ref(0)

// 表单验证规则
const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

// 用户名验证（不允许特殊字符）
const validateUsername = (rule, value, callback) => {
  const reg = /^[a-zA-Z0-9_]{2,20}$/;
  if (!reg.test(value)) {
    callback(new Error("用户名只能包含字母、数字和下划线，长度2-20位"));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
  ],
  email: [
    { required: true, trigger: "blur", message: "请输入邮箱地址" },
    { 
      pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, 
      message: '请输入有效的邮箱地址', 
      trigger: ['blur', 'change'] 
    }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [
    { required: true, trigger: "blur", message: "请输入验证码" },
    { len: 6, message: "验证码必须为6位", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码必须为数字", trigger: "blur" }
  ],
  agreeTerms: [
    { required: true, trigger: "change", message: "您必须同意服务协议和隐私政策才能注册" }
  ]
}

// 发送邮箱验证码
const handleSendCode = () => {
  console.log("发送验证码按钮被点击，开始执行逻辑");
  // 1手动验证邮箱是否为空
  if (!registerForm.value.email.trim()) {
    ElMessage.warning("请输入邮箱地址");
    return;
  }
  // 手动验证邮箱格式
  const emailReg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!emailReg.test(registerForm.value.email)) {
    ElMessage.warning("请输入有效的邮箱地址（如：xxx@example.com）");
    return;
  }
  // 验证通过，执行发送逻辑
  isCodeDisabled.value = true;
  codeButtonText.value = "发送中...";
  sendRegisterCode(registerForm.value.email)
    .then((response) => {
      // 记录验证码发送时间（时间戳）
      codeSendTime.value = Date.now();
      // 明确提示有效期（5分钟）
      ElMessage.success("验证码已发送至您的邮箱，5分钟内有效，请及时查收！");
      
      // 倒计时逻辑（未修改）
      let countdown = 60;
      const timer = setInterval(() => {
        countdown--;
        codeButtonText.value = `重新发送(${countdown}s)`;
        if (countdown <= 0) {
          clearInterval(timer);
          codeButtonText.value = "发送验证码";
          isCodeDisabled.value = false;
        }
      }, 1000);
    })
    .catch((error) => {
      // 优化错误提示（明确标注"发送失败"）
      ElMessage.error(`发送失败：${error.response?.data?.msg || "验证码发送失败，请稍后重试"}`);
      codeButtonText.value = "发送验证码";
      isCodeDisabled.value = false;
    });
};



// 处理注册
const handleRegister = () => {
  registerFormRef.value.validate(valid => {
    if (valid) {
      if (!registerForm.value.agreeTerms) {
        ElMessage.error("请勾选同意服务协议和隐私政策");
        return;
      }

      loading.value = true;
      
      const registerData = {
        username: registerForm.value.username,
        password: registerForm.value.password,
        email: registerForm.value.email,
        code: registerForm.value.code
      };
      
      register(registerData)
        .then(response => {
          // 明确判断成功条件
          if (response.code === 200) {
            ElMessageBox.alert(
              `恭喜！账号 ${registerForm.value.username} 注册成功`,
              '注册成功',
              { type: 'success' }
            ).then(() => {
              router.push("/login");
            });
          } else {
            ElMessage.error(response.msg || "注册失败");
          }
        })
        .catch(error => {
          const errorMsg = error.response?.data?.msg || error.message || "注册失败";
          ElMessage.error(`注册失败：${errorMsg}`);
        })
        .finally(() => {
          loading.value = false;
        });
    }
  });
}
</script>

<style lang="scss" scoped>
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login.jpg");
  background-size: cover;
  background-color: rgba(0, 0, 0, 0.5);
  /* 添加一个半透明黑色背景，可调整透明度来控制调暗程度 */
  background-blend-mode: multiply;
  /* 使用正片叠底混合模式来混合背景图片和纯色背景 */
}

.register-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.register-image-container {
  position: relative;
  /* 使容器成为定位上下文 */
  width: 100%;
  /* 根据需要设置宽度 */
  height: auto;
  /* 根据图片尺寸自动调整高度，或者设置具体高度 */
}

.register-image-container img {
  height: 600px;
  width: 500px;
  object-fit: cover;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
  transform: translateY(2px);
}

.system-title {
  position: absolute;
  /* 绝对定位，相对于最近的定位祖先元素（这里是.login-image-container） */
  top: 85px;
  /* 距离容器顶部10px，您可以根据需要调整这个值 */
  left: 50%;
  /* 水平居中，但需要配合transform进行微调 */
  transform: translateX(-50%);
  /* 水平方向移动自身宽度的50%，以实现真正的居中 */
  color: #696666;
  /* 根据图片背景设置合适的文字颜色，如果背景是深色则设置为浅色，反之亦然 */
  font-size: 28px;
  /* 根据需要设置字体大小 */
  font-weight: bold;
  /* 设置字体加粗，使文字更加醒目 */
  width: fit-content;
  /* 使宽度适应内容，避免不必要的扩展 */
  margin: 0;
  /* 移除默认的外边距 */
  padding: 0;
  /* 移除默认的内边距 */
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.checkbox {
  font-size: 12px;
}

.check-box {
  width: 12px;
  height: 12px;
}

.register-title-container {
  display: flex;
  justify-content: center;
  /* 添加这一行来水平居中对齐子元素 */
  align-items: center;
  margin-top: 20px;
  margin-bottom: 10px;
}

.register-title-container img {
  width: 25px;
  /* 根据需要调整 */
  height: 25px;
  /* 保持图片比例 */
  margin-bottom: 25px;
}

.icon {
  margin-right: 20px;
  /* 图片和标题之间的间距，根据需要调整 */
  transform: translateY(8px);
  //height: auto;
}

.checkbox-input {
  width: 10px;
  height: 10px;
}

.title {
  //margin: 0px;
  text-align: center;
  color: rgb(111, 113, 113);
  font-weight: bold;
  margin-bottom: 25px;
}

.register-form {
  background: #ffffff;
  width: 480px;
  height: 600px;
  padding: 25px 30px;
  border-top-right-radius: 20px;
  border-bottom-right-radius: 20px;

  .el-input {
    height: 42px;
    /* 输入框高度也适当调大，让输入区域更舒展 */

    input {
      height: 42px;
      border-radius: 20px / 40px;
    }
  }

  .input-icon {
    height: 43px;
    /* 配合输入框高度调整 */
    width: 14px;
    margin-left: 2px;
  }
}

.register-tip {
  font-size: 13px;
  text-align: center;
  color: #c3baba;
}

.register-code {
  width: 33%;
  height: 38px;
  float: right;
  position: relative; // 确保按钮在顶层
  z-index: 10; // 提高层级，避免被遮挡

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.gologin {
  margin-top: 18px;
  font-size: 13px;
}

.link-type {
  color: rgb(75, 109, 231);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.register-code-img {
  height: 38px;
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
</style>
