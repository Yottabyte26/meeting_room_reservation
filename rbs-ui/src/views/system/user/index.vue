<template>
  <div class="app-container">
    <!--用户数据-->
    <!-- 搜索框 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号码" prop="phonenumber">
        <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" style="width: 308px;">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-"
          start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
          v-hasPermi="['system:user:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:user:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport"
          v-hasPermi="['system:user:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport"
          v-hasPermi="['system:user:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 用户信息列表显示 -->
    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" align="center" width="55" />
      <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" width="80" />
      <el-table-column label="头像" align="center" prop="avatar" v-if="columns[6].visible">
        <template #default="scope">
          <image-preview :src="scope.row.avatar" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="用户账号" align="center" key="userName" prop="userName" v-if="columns[1].visible"
        :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns[2].visible"
        :show-overflow-tooltip="true" />
      <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible"
        :show-overflow-tooltip="true" width="85" />
      <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible"
        width="110" />
      <el-table-column label="邮箱" align="center" key="email" prop="email" v-if="columns[5].visible" width="180"
        :show-overflow-tooltip="true" />

      <el-table-column label="性别" align="center" key="sex" v-if="columns[7].visible" width="60">
        <template #default="scope">
          <el-tag type="success" effect="light">
            {{ scope.row.sex === '0' ? '男' : scope.row.sex === '1' ? '女' : '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" key="status" v-if="columns[8].visible" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" active-color="#52c41a"
            @change="handleStatusChange(scope.row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button-group>
            <!-- 编辑按钮 -->
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['system:user:edit']">
              编辑
            </el-button>

            <!-- 删除按钮 -->
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['system:user:remove']" v-if="scope.row.userId !== 1">
              删除
            </el-button>
            <!-- 重置密码按钮 -->

            <el-button link type="warning" icon="Key" @click="handleResetPwd(scope.row)"
              v-hasPermi="['system:user:resetPwd']" :disabled="!scope.row.email || scope.row.status === '1'"
              :title="!scope.row.email ? '该用户没有绑定邮箱' : scope.row.status === '1' ? '该用户已被停用' : ''">
              重置密码
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择归属部门" clearable filterable style="width: 100%">
                <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option v-for="dict in filteredSexOptions" :key="dict.value" :label="dict.label"
                  :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label
                }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="头像" prop="avatar">
          <image-upload v-model="form.avatar" :limit="1" />
        </el-form-item>

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" style="vertical-align: middle;" /> 是否更新已经存在的用户数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;"
              @click="importTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

<!-- 重置密码对话框 -->
<el-dialog 
  v-model="resetPwdDialog.visible" 
  :title="resetPwdDialog.title" 
  width="500px"
  class="reset-pwd-dialog"
  :modal="true"
  :center="true" 
  :append-to-body="true"
  style="position: fixed; top: 40%; left: 50%; transform: translate(-50%, -50%);"
>
  <el-form>
    <el-form-item label="新密码">
      <el-input 
        v-model="resetPwdDialog.password" 
        :type="resetPwdDialog.showPassword ? 'text' : 'password'" 
        placeholder="请输入新密码"
      >
        <template #suffix>
          <el-icon 
            style="cursor: pointer" 
            @click="resetPwdDialog.showPassword = !resetPwdDialog.showPassword"
          >
            <View v-if="resetPwdDialog.showPassword" />
            <Hide v-else />
          </el-icon>
        </template>
      </el-input>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="resetPwdDialog.visible = false">取消</el-button>
    <el-button type="primary" @click="confirmResetPwd">确定</el-button>
  </template>
</el-dialog>
    
  </div>
</template>

<script setup name="User">
import { getToken } from "@/utils/auth";
import { View, Hide } from '@element-plus/icons-vue';
import { ref, reactive, computed, getCurrentInstance } from 'vue';
import {
  changeUserStatus,
  listUser,
  resetUserPwd,
  delUser,
  getUser,
  updateUser,
  addUser, 
  sendPasswordResetNotification
} from "@/api/system/user";
import { listDeptSimple } from "@/api/system/dept";

const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict("sys_normal_disable", "sys_user_sex");

// 数据定义
const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const deptName = ref("");
const deptOptions = ref([]);
const initPassword = ref(undefined);
const roleOptions = ref([]);

// 重置密码对话框状态
const resetPwdDialog = reactive({
  visible: false,
  title: '',
  password: '123456Aa',
  showPassword: false,
  userId: null,
  userName: '',
  email: ''
});

// 用户导入参数
const upload = reactive({
  open: false,
  title: "",
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: "Bearer " + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + "/system/user/importData"
});

// 列显隐信息
const columns = ref([
  { key: 0, label: `用户编号`, visible: true },
  { key: 6, label: `头像`, visible: true },
  { key: 1, label: `用户名称`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `部门`, visible: true },
  { key: 4, label: `手机号码`, visible: true },
  { key: 5, label: `邮箱`, visible: true },
  { key: 7, label: `性别`, visible: true },
  { key: 8, label: `状态`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: undefined,
    phonenumber: undefined,
    status: undefined,
    deptId: undefined
  },
  rules: {
    userName: [{ required: true, message: "用户名称不能为空", trigger: "blur" }, {
      min: 2,
      max: 20,
      message: "用户名称长度必须介于 2 和 20 之间",
      trigger: "blur"
    }],
    nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
    password: [
      { required: true, trigger: "blur", message: "请输入您的密码" },
      { min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' },
      { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
    ],
    email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
    phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询用户列表 */
function getList() {
  loading.value = true;
  listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
    loading.value = false;
    userList.value = res.rows;
    total.value = res.total;
  });
};

/** 获取部门列表 */
const getDeptList = async () => {
  try {
    const response = await listDeptSimple()
    deptOptions.value = response.data
  } catch (error) {
    console.error('获取部门列表失败:', error)
  }
}

/** 节点单击事件 */
function handleNodeClick(data) {
  queryParams.value.deptId = data.id;
  handleQuery();
};

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  queryParams.value.deptId = undefined;
  handleQuery();
};

/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId || ids.value;
  proxy.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function () {
    return delUser(userIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
};

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/user/export", {
    ...queryParams.value,
  }, `user_${new Date().getTime()}.xlsx`);
};

/** 用户状态修改  */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗?').then(function () {
    return changeUserStatus(row.userId, row.status);
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功");
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0";
  });
};


// 在script setup部分添加
const PasswordInput = {
  props: ['value', 'showPassword'],
  emits: ['update:value', 'update:showPassword'],
  render() {
    return h('div', { style: { position: 'relative' } }, [
      h('el-input', {
        modelValue: this.value,
        type: this.showPassword ? 'text' : 'password',
        'onUpdate:modelValue': (val) => this.$emit('update:value', val),
        style: { width: '100%' }
      }),
      h('el-icon', {
        style: {
          position: 'absolute',
          right: '10px',
          top: '10px',
          cursor: 'pointer',
          zIndex: 1
        },
        onClick: () => this.$emit('update:showPassword', !this.showPassword)
      }, [
        this.showPassword ? h(View) : h(Hide)
      ])
    ])
  }
}


/** 重置密码按钮操作 */
function handleResetPwd(row) {
  // 校验用户是否有邮箱
  if (!row.email) {
    proxy.$modal.msgError("该用户没有绑定邮箱，无法发送密码重置通知邮件");
    return;
  }

  // 校验用户状态是否正常
  if (row.status === "1") {
    proxy.$modal.msgError("该用户已被停用，无法重置密码");
    return;
  }

  // 设置对话框数据
  resetPwdDialog.title = `请输入"${row.userName}"的新密码`;
  resetPwdDialog.password = '123456Aa';
  resetPwdDialog.showPassword = false;
  resetPwdDialog.userId = row.userId;
  resetPwdDialog.userName = row.userName;
  resetPwdDialog.email = row.email;
  resetPwdDialog.visible = true;
}

/** 确认重置密码 */
const confirmResetPwd = () => {
  // 验证密码格式
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{5,20}$/.test(resetPwdDialog.password)) {
    proxy.$modal.msgError("密码格式不正确，必须包含字母和数字，长度在5-20位之间");
    return;
  }

  resetUserPwd(resetPwdDialog.userId, resetPwdDialog.password).then(response => {
    proxy.$modal.msgSuccess("修改成功，新密码是：" + resetPwdDialog.password);
    resetPwdDialog.visible = false;

    // 发送密码重置通知邮件
    const emailParams = {
      email: resetPwdDialog.email,
      newPassword: resetPwdDialog.password,
      userName: resetPwdDialog.userName
    };

    return sendPasswordResetNotification(emailParams);
  }).catch(error => {
    console.error("重置密码或发送邮件失败:", error);
    proxy.$modal.msgError("操作失败，请重试");
  });
};

/** 选择条数  */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

/** 导入按钮操作 */
function handleImport() {
  upload.title = "用户导入";
  upload.open = true;
};

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {}, `user_template_${new Date().getTime()}.xlsx`);
};
/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};
/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
};

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
};

/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    avatar: undefined,
    sex: undefined,
    status: "0",
    remark: undefined,
    roleIds: []
  };
  proxy.resetForm("userRef");
};

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
};

/** 新增按钮操作 */
function handleAdd() {
  reset();
  getUser().then(response => {
    roleOptions.value = response.roles;
    open.value = true;
    title.value = "添加用户";
    form.value.password = initPassword.value;
  });
};

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const userId = row.userId || ids.value;
  getUser(userId).then(response => {
    form.value = response.data;
    roleOptions.value = response.roles;
    form.value.roleIds = response.roleIds;
    open.value = true;
    title.value = "修改用户";
    form.password = "";
  });
};

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addUser(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
};

onMounted(() => {
  getDeptList()
  getList()
})
</script>


