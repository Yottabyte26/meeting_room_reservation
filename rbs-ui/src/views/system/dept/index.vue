<template>
   <div class="app-container">
      <!-- 部门信息 -->
      <!-- 搜索框 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="部门名称" prop="deptName">
            <el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable style="width: 200px"
               @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="部门状态" clearable style="width: 200px">
               <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label"
                  :value="dict.value" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <!-- 按钮 -->
      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd"
               v-hasPermi="['system:dept:add']">新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport"
               v-hasPermi="['system:dept:export']">导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 部门信息列表显示 -->
      <el-table v-if="refreshTable" v-loading="loading" :data="deptList" row-key="deptId"
         :default-expand-all="isExpandAll" :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
         <el-table-column type="selection" width="50" align="center" />
         <el-table-column prop="deptName" label="部门名称" align="center" />
         <el-table-column prop="remark" label="备注" align="center">
            <template #default="scope">
               <div class="ellipsis">{{ scope.row.remark || ' ' }}</div>
            </template>
         </el-table-column>

         <el-table-column label="状态" align="center" key="status" width="150">
            <template #default="scope">
               <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" active-color="#52c41a"
                  @change="handleStatusChange(scope.row)"></el-switch>
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="250">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                  v-hasPermi="['system:dept:edit']" class="custom-edit-btn">修改</el-button>
               <!-- <el-button link type="success" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['system:dept:add']">新增</el-button> -->
               <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                  v-hasPermi="['system:dept:remove']" class="custom-delete-btn">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <!-- 添加或修改部门对话框 -->
      <el-dialog :title="title" v-model="open" width="600px" append-to-body>
         <el-form ref="deptRef" :model="form" :rules="rules" label-width="80px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="部门名称" prop="deptName">
                     <el-input v-model="form.deptName" placeholder="请输入部门名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="负责人" prop="leader">
                     <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="联系电话" prop="phone">
                     <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                     <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="部门状态">
                     <el-radio-group v-model="form.status">
                        <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label
                        }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="备注" prop="remark">
                     <el-input v-model="form.remark" type="textarea" placeholder="请输入部门备注信息" rows="3" maxlength="255" />
                  </el-form-item>
               </el-col>

            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
      <!-- 部门导入对话框 -->
      <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
         <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="upload.headers"
            :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
            :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
               <div class="el-upload__tip text-center">
                  <div class="el-upload__tip">
                     <el-checkbox v-model="upload.updateSupport" style="vertical-align: middle;" /> 是否更新已经存在的部门数据
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
   </div>
</template>

<script setup name="Dept">
import { listDept, getDept, delDept, addDept, updateDept, listDeptExcludeChild } from "@/api/system/dept";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const deptList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
import { getToken } from "@/utils/auth";
const deptOptions = ref([]);
const isExpandAll = ref(true);
const refreshTable = ref(true);
const exportLoading = ref(false);
/*** 部门导入参数 */
const upload = reactive({
   // 是否显示弹出层（部门导入）
   open: false,
   // 弹出层标题（部门导入）
   title: "",
   // 是否禁用上传
   isUploading: false,
   // 是否更新已经存在的部门数据
   updateSupport: 0,
   // 设置上传的请求头部
   headers: { Authorization: "Bearer " + getToken() },
   // 上传的地址
   url: import.meta.env.VITE_APP_BASE_API + "/system/dept/importData"
});
const data = reactive({
   form: {},
   queryParams: {
      deptName: undefined,
      status: undefined,
      remark: undefined
   },
   rules: {
      deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
      email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
      phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }],
      remark: [{ max: 255, message: '备注长度不能超过255个字符', trigger: 'blur' }]
   },
});

const { queryParams, form, rules } = toRefs(data);
function handleImport() {
   upload.title = "部门导入";
   upload.open = true;
};
/** 查询部门列表 */
function getList() {
   loading.value = true;
   listDept(queryParams.value).then(response => {
      deptList.value = response.data;
      loading.value = false;
   });
}
/** 取消按钮 */
function cancel() {
   open.value = false;
   reset();
}
/** 表单重置 */
function reset() {
   form.value = {
      deptId: undefined,
      deptName: undefined,
      leader: undefined,
      phone: undefined,
      email: undefined,
      status: "0",
      remark: ''
   };
   proxy.resetForm("deptRef");
}
/** 搜索按钮操作 */
function handleQuery() {
   getList();
}
/** 重置按钮操作 */
function resetQuery() {
   proxy.resetForm("queryRef");
   handleQuery();
}
/** 新增按钮操作 */
function handleAdd(row) {
   reset();
   open.value = true;
   title.value = "添加部门";
}

/** 导出按钮操作 */
function handleExport() {
   proxy.download("system/dept/export", {
      ...queryParams.value,
   }, `dept_${new Date().getTime()}.xlsx`);
};

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   listDeptExcludeChild(row.deptId).then(response => {
      deptOptions.value = proxy.handleTree(response.data, "deptId");
   });
   getDept(row.deptId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改部门";
   });
}
/** 提交按钮 */
function submitForm() {
   proxy.$refs["deptRef"].validate(valid => {
      if (valid) {
         if (form.value.deptId != undefined) {
            updateDept(form.value).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addDept(form.value).then(response => {
               proxy.$modal.msgSuccess("新增成功");
               open.value = false;
               getList();
            });
         }
      }
   });
}
/** 删除按钮操作 */
function handleDelete(row) {
   proxy.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?').then(function () {
      return delDept(row.deptId);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
}

/** 状态变更处理 */
function handleStatusChange(row) {
   row.status = row.status === '0' ? '1' : '0';
   updateDept(row).then(() => {
      proxy.$modal.msgSuccess("状态修改成功");
   }).catch(() => {
      row.status = row.status === '0' ? '1' : '0'; // 回滚状态
   });
}

getList();
</script>



<style scoped>
.custom-edit-btn {
  background: #e6f3ff;
  color: #409eff;
  border: none;
  border-radius: 4px;
  padding: 10px 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  text-decoration: none;
  position: relative;
}
.custom-edit-btn:hover {
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.2);
  top: -1px; /* 模拟轻微抬起 */
}
.custom-edit-btn:active {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  top: 1px; /* 模拟按压下去 */
}

.custom-delete-btn {
  background: #ffe6e6;
  color: #f56c6c;
  border: none;
  border-radius: 4px;
  padding: 10px 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  text-decoration: none;
  position: relative;
}
.custom-delete-btn:hover {
  box-shadow: 0 4px 8px rgba(245, 108, 108, 0.2);
  top: -1px;
}
.custom-delete-btn:active {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  top: 1px;
}
</style>