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
    <el-form-item>
      <el-button type="primary" @click="submit" :loading="loading">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { updateUserPwd, getUserProfile, sendPasswordResetNotification } from "@/api/system/user";
import { addNotice } from "@/api/system/notice";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const currentUser = ref(null);

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
});

// 获取用户信息
function getUser() {
  getUserProfile().then(response => {
    currentUser.value = response.data;
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

const rules = ref({
  oldPassword: [{ required: true, message: "旧密码不能为空", trigger: "blur" }],
  newPassword: [
    { required: true, validator: validatePassword, trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, validator: equalToPassword, trigger: "blur" }
  ]
});

// 清空表单
function resetForm() {
  user.oldPassword = undefined;
  user.newPassword = undefined;
  user.confirmPassword = undefined;
  proxy.$refs.pwdRef.resetFields();
}

/** 提交按钮 */
function submit() {
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      // 再次校验密码格式
      if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/.test(user.newPassword)) {
        proxy.$modal.msgError("密码格式不正确，必须包含字母和数字，长度在5-20位之间");
        return;
      }

      loading.value = true;
      updateUserPwd(user.oldPassword, user.newPassword).then(response => {
        proxy.$modal.msgSuccess("修改成功");

        // 发送密码修改通知邮件
        if (currentUser.value && currentUser.value.email) {
          const emailParams = {
            email: currentUser.value.email,
            newPassword: user.newPassword,
            userName: currentUser.value.userName
          };

          return sendPasswordResetNotification(emailParams);
        }
      }).then(() => {
        // 获取当前日期字符串（仅年-月-日）
        const now = new Date();
        const formatDate = (date) => {
          const pad = (n) => n.toString().padStart(2, '0');
          return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
        };
      }).then(() => {
        // 提交成功后清空表单
        resetForm();
      }).catch(error => {
        // proxy.$modal.msgError("操作失败，请重试");
      }).finally(() => {
        loading.value = false;
      });
    }
  });
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage();
};

// 组件挂载时获取用户信息
onMounted(() => {
  getUser();
});
</script>