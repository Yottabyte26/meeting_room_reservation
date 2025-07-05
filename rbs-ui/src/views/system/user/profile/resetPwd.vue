<template>
  <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px">
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password />
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword">
      <el-input v-model="user.newPassword" placeholder="请输入新密码（5-20位字母加数字）" type="password" show-password />
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password />
    </el-form-item>

    <el-form-item label="邮箱" prop="email" class="form-item" disabled>
      <el-input v-model="user.email" placeholder="请输入注册邮箱">
        <template #prefix>
          <svg-icon icon-class="email" />
        </template>
      </el-input>
    </el-form-item>

    <el-form-item prop="code" class="form-item">
      <div class="code-container">
        <el-input v-model="user.code" style="margin-left: 10px;" placeholder="请输入6位验证码" maxlength="6" />
        <el-button type="primary" :disabled="codeDisabled || !canSendCode" @click="sendResetCode" class="code-button">
          {{ codeButtonText }}
        </el-button>
      </div>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="submit" :loading="loading">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance, computed } from "vue";
import { updateUserPwd, getUserProfile, sendPasswordResetNotification } from "@/api/system/user";
import { sendResetPwdEmailCode, verifyEmailCode } from "@/api/login";

const { proxy } = getCurrentInstance();

// 状态管理
const loading = ref(false);
const codeDisabled = ref(false);
const codeButtonText = ref("获取验证码");
let countdown = 60;
const currentUser = ref(null);

// 表单数据
const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined,
  email: "",
  code: ""
});

// 获取用户信息（自动填充邮箱）
function getUser() {
  getUserProfile().then(response => {
    currentUser.value = response.data;
    user.email = response.data.email; // 自动填充用户注册邮箱
  });
}

// 密码格式校验
const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error("新密码不能为空"));
  } else if (value.length < 5 || value.length > 20) {
    callback(new Error("密码长度必须在5-20位之间"));
  } else if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/.test(value)) {
    callback(new Error("密码必须包含字母和数字"));
  } else {
    callback();
  }
};

// 确认密码校验
const equalToPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error("确认密码不能为空"));
  } else if (user.newPassword !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

// 表单验证规则（新增邮箱和验证码校验）
const rules = {
  oldPassword: [
    { required: true, message: "旧密码不能为空", trigger: "blur" }
  ],
  newPassword: [
    { required: true, message: "新密码不能为空", trigger: "blur" },
    { min: 6, message: "新密码不能少于6位", trigger: "blur" }
  ],
  code: [
    { required: true, message: "验证码不能为空", trigger: "blur" }
  ]
}

// 清空表单
function resetForm() {
  user.oldPassword = undefined;
  user.newPassword = undefined;
  user.confirmPassword = undefined;
  user.code = "";
  proxy.$refs.pwdRef?.resetFields();
}

// 计算属性，判断是否可以发送验证码
const canSendCode = computed(() => {
  return (
    user.oldPassword &&
    user.newPassword &&
    user.confirmPassword &&
    user.newPassword === user.confirmPassword &&
    /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/.test(user.newPassword)
  );
});

// 发送邮箱验证码
function sendResetCode() {
  // 先验证密码表单
  proxy.$refs.pwdRef.validateField(['oldPassword', 'newPassword', 'confirmPassword'], (valid) => {
    if (!valid) {
      proxy.$modal.msgWarning("请先正确填写密码信息");
      return;
    }
    // 邮箱格式校验
    if (!user.email) {
      proxy.$modal.msgWarning("请输入邮箱地址");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) {
      proxy.$modal.msgWarning("请输入正确的邮箱格式");
      return;
    }

    // 发送验证码
    codeDisabled.value = true;
    codeButtonText.value = "发送中...";
    sendResetPwdEmailCode(user.email)
      .then(() => {
        proxy.$modal.msgSuccess("验证码已发送至您的邮箱，5分钟内有效");
        // 倒计时逻辑
        const timer = setInterval(() => {
          countdown--;
          codeButtonText.value = `${countdown}秒后重发`;
          if (countdown <= 0) {
            clearInterval(timer);
            codeDisabled.value = false;
            codeButtonText.value = "获取验证码";
            countdown = 60; // 重置倒计时
          }
        }, 1000);
      })
      .catch(error => {
        codeDisabled.value = false;
        codeButtonText.value = "获取验证码";
        proxy.$modal.msgError(error.msg || "验证码发送失败，请重试");
      });
  }, 1000);
}

/** 提交修改密码 */
function submit() {
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      // 先验证邮箱验证码
      verifyEmailCode(user.email, user.code)
        .then(() => updateUserPwd(user.oldPassword, user.newPassword))
        .then(() => {
          proxy.$modal.msgSuccess("密码修改成功");
          resetForm();
        })
        .catch(error => {
          const errorMsg = error.response?.data?.msg || "操作失败，请重试";
          proxy.$modal.msgError(errorMsg);
        })
        .finally(() => {
          loading.value = false;
        });
    }
  });
}

/** 关闭按钮 */
function close() {
  resetForm();
  proxy.$tab.closePage();
}

// 组件挂载时获取用户信息
onMounted(() => {
  getUser();
});

</script>

<style>
/* 验证码容器（实现同一行布局） */
.code-container {
  display: flex;
  align-items: center;
  width: 100%;
}

/* 验证码输入框样式 */
.code-input {
  flex: 1;
  /* 占满剩余空间 */
  height: 40px;
}

/* 验证码按钮样式 */
.get-code-btn {
  margin-left: 10px;
  width: 120px;
  /* 固定按钮宽度，确保文字不换行 */
  height: 40px;
  /* 与输入框高度一致 */
  padding: 0;
  /* 去除默认内边距，避免按钮过高 */
  white-space: nowrap;
  /* 防止按钮文字换行 */
}

/* 统一输入框样式 */
:deep(.el-input__wrapper) {
  border-radius: 4px;
  height: 40px;
}
</style>