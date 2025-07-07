<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="85px">
      <el-form-item label="会议室名称" prop="roomName">
        <el-input v-model="queryParams.roomName" placeholder="请输入会议室名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会议室位置" prop="location">
        <el-input v-model="queryParams.location" placeholder="请输入会议室位置" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="容纳人数" prop="capacity">
        <el-input v-model="queryParams.capacity" placeholder="请输入容纳人数" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd"
          v-hasPermi="['system:room:add']">新增会议室</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
          v-hasPermi="['system:room:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:room:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport"
          v-hasPermi="['system:room:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="40" align="center" />
      <el-table-column label="会议室ID" align="center" prop="roomId" width="80" />
      <el-table-column label="会议室名称" align="center" prop="roomName" width="110" />
      <el-table-column label="会议室编号" align="center" prop="roomCode" width="90" />
      <el-table-column label="会议室位置" align="center" prop="location" />
      <el-table-column label="会议室图像" align="center" prop="roomImage">
        <template #default="scope">
          <image-preview :src="scope.row.roomImage" :width="50" :height="50" />
        </template>
      </el-table-column>
      <el-table-column label="容纳人数" align="center" prop="capacity" width="80" />
      <el-table-column label="会议室类型" align="center" prop="roomType" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'primary' : 'danger'" effect="light">
            {{ formatStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="会议室描述" align="center" prop="description" />
      <el-table-column label="状态" align="center" key="status" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" active-color="#52c41a"
            @change="handleStatusChange(scope.row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dept:edit']"
            class="custom-edit-btn">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
            v-hasPermi="['system:dept:remove']" class="custom-delete-btn">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改会议室信息对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="roomRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="会议室名称" prop="roomName">
              <el-input v-model="form.roomName" placeholder="请输入会议室名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容纳人数" prop="capacity">
              <el-input v-model="form.capacity" placeholder="请输入容纳人数" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="会议室类型" width="150" prop="roomType">
          <el-select v-model="form.roomType" placeholder="请选择会议室类型" style="width: 40%" @change="handleRoomTypeChange">
            <el-option v-for="option in roomTypeOptions" :key="option.value" :label="option.label"
              :value="option.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="会议室位置" prop="location">
          <el-input v-model="form.location" type="textarea" placeholder="请输入会议室位置" />
        </el-form-item>
        <el-form-item label="会议室描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="会议室图像" prop="roomImage">
          <div class="custom-upload-wrapper" @mouseenter="hover = true" @mouseleave="hover = false">
            <el-upload class="avatar-uploader" :show-file-list="false" :before-upload="handleUpload"
              accept="image/png,image/jpeg" list-type="picture-card">
              <img v-if="form.roomImage" :src="form.roomImage" class="avatar" />
              <el-icon v-else>
                <Plus />
              </el-icon>
            </el-upload>
            <!-- 悬浮操作区 -->
            <div v-if="form.roomImage && hover" class="upload-action-bar">
              <el-tooltip content="更换图片" placement="top">
                <el-icon class="action-icon" @click="triggerUpload">
                  <Plus />
                </el-icon>
              </el-tooltip>
              <el-tooltip content="删除图片" placement="top">
                <el-icon class="action-icon" @click="removeImage">
                  <Delete />
                </el-icon>
              </el-tooltip>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Room">
import { listRoom, getRoom, delRoom, addRoom, updateRoom, getUploadToken, uploadRoomImage } from "@/api/system/room"
import { listReservation } from "@/api/system/reservation"
import axios from "axios";
import { ElMessage } from 'element-plus'
const { proxy } = getCurrentInstance()

const roomList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const reservationList = ref([])
const hover = ref(false)
const data = reactive({
  form: {},
  roomTypeOptions: [
    { value: '普通会议室', label: '普通会议室' },
    { value: '视频会议室', label: '视频会议室' },
    { value: '多功能会议室', label: '多功能会议室' },
    { value: '董事会议室', label: '董事会议室' },
    { value: '培训会议室', label: '培训会议室' }
  ],
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    roomName: null,
    roomCode: null,
    location: null,
    capacity: null,
    roomType: null,
    status: null,
    description: null,
    isEnabled: null,
  },
  rules: {
    roomName: [
      { required: true, message: "会议室名称不能为空", trigger: "blur" }
    ],
    location: [
      { required: true, message: "会议室位置不能为空", trigger: "blur" }
    ],
    capacity: [
      { required: true, message: "容纳人数不能为空", trigger: "blur" }
    ],
    roomType: [
      { required: true, message: "会议室类型不能为空", trigger: "change" }
    ],
    status: [
      { required: true, message: "状态不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules, roomTypeOptions } = toRefs(data)

/** 查询会议室信息列表 */
function getList() {
  loading.value = true
  listRoom(queryParams.value).then(response => {
    roomList.value = response.rows
    total.value = response.total
    loading.value = false
  })

}

const uploadToken = ref(""); // 响应式变量

// 触发上传
function triggerUpload(e) {
  // 阻止事件冒泡，防止触发el-upload的点击
  e.stopPropagation()
  // 触发el-upload的input[type=file]
  document.querySelector('.avatar-uploader input[type=file]').click()
}

// 删除图片
function removeImage(e) {
  e.stopPropagation()
  form.value.roomImage = ''
}

function handleUpload(file) {
  const formData = new FormData();
  formData.append("file", file);
  return uploadRoomImage(formData).then(res => {
    if (res.data.code === 200) {
      form.value.roomImage = res.data.data.url;
      ElMessage.success("上传成功");
      return false; // 阻止 el-upload 默认上传
    } else {
      ElMessage.error(res.data.error || "上传失败");
      return false;
    }
  }).catch(() => {
    ElMessage.error("上传失败");
    return false;
  });
}
/** 会议室状态修改  */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '"' + row.roomName + '"会议室吗?')
    .then(() => {
      // 切换状态值
      row.status = row.status === "0" ? "0" : "1";
      form.value = { ...row };
      return updateRoom(form.value);
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
      getList(); // 刷新列表
    })
    .catch(() => {
      // 恢复原始状态
      row.status = row.status === "0" ? "0" : "1";
    });
}
/**  格式化会议室状态 */
function formatStatus(value) {
  return String(value) === '0' ? '可预定' : '不可预定';
}

/**  格式化是否启用字段 */
function formatEnable(value) {
  return String(value) === '1' ? '启用' : '禁用';
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    roomId: null,
    roomName: null,
    roomCode: null,
    location: null,
    capacity: null,
    roomType: null,
    status: null,
    description: null,
    isEnabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("roomRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.roomId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加会议室信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _roomId = row.roomId || ids.value
  getRoom(_roomId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会议室信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["roomRef"].validate(valid => {
    if (valid) {
      if (form.value.roomId != null) {
        updateRoom(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRoom(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}
function getReservationList() {
  listReservation({ pageSize: 1000 }).then(response => {
    reservationList.value = response.rows
  })
}
getReservationList()

/** 删除按钮操作 */
function handleDelete(row) {
  getReservationList()
  // 检查是否存在符合条件的预订记录
  const hasPendingReservation = reservationList.value.some(item => {
    return item.roomId === row.roomId && item.status === "0"; // 假设状态0表示待审核
  });

  if (hasPendingReservation) {
    proxy.$modal.msgError("该会议室有预定单没有完成，请先审核退回，再删除");
    return;
  }
  const _roomIds = row.roomId || ids.value
  proxy.$modal.confirm('是否确认删除会议室信息编号为"' + _roomIds + '"的数据项？').then(function () {
    return delRoom(_roomIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => { })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/room/export', {
    ...queryParams.value
  }, `room_${new Date().getTime()}.xlsx`)
}

getList()
</script>



<style scoped>
.custom-upload-wrapper {
  position: relative;
  display: inline-block;
}

.avatar-uploader .el-upload {
  width: 148px;
  height: 148px;
  border-radius: 6px;
}

.avatar {
  width: 148px;
  height: 148px;
  object-fit: cover;
  border-radius: 6px;
}

.upload-action-bar {
  position: absolute;
  top: 0;
  left: 0;
  width: 148px;
  height: 148px;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  z-index: 10;
  border-radius: 6px;
}

.action-icon {
  color: #fff;
  font-size: 28px;
  cursor: pointer;
  transition: color 0.2s;
}

.action-icon:hover {
  color: #409EFF;
}

/* 添加操作按钮样式 */
.el-button--primary.link-btn {
  color: #409EFF;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.el-button--primary.link-btn:hover {
  background-color: rgba(64, 158, 255, 0.1);
}

.el-button--danger.link-btn {
  color: #F56C6C;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.el-button--danger.link-btn:hover {
  background-color: rgba(245, 108, 108, 0.1);
}

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
  top: -1px;
  /* 模拟轻微抬起 */
}

.custom-edit-btn:active {
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  top: 1px;
  /* 模拟按压下去 */
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