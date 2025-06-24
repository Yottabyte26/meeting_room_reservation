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

    <el-table v-loading="loading" :data="roomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="40" align="center" />
      <el-table-column label="会议室ID" align="center" prop="roomId" />
      <el-table-column label="会议室名称" align="center" prop="roomName" />
      <el-table-column label="会议室编号" align="center" prop="roomCode" />
      <el-table-column label="会议室位置" align="center" prop="location" />
      <el-table-column label="容纳人数" align="center" prop="capacity" />
      <el-table-column label="会议室类型" align="center" prop="roomType" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_room_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="会议室描述" align="center" prop="description" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.status === '0' && scope.row.isEnabled === '1'" link type="primary" icon="Edit"
            @click="handleAdd(scope.row)">预定</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 预定弹窗-->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="reservationRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="会议室名称" prop="roomName">
              <el-input :placeholder="form.roomName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预定部门" prop="deptName">
              <el-select v-model="form.deptName" placeholder="请选择部门" filterable clearable remote
                :remote-method="remoteMethodDept" :loading="loadingDept">
                <el-option label="请选择预定部门" disabled></el-option>
                <el-option v-for="item in deptList" :key="item.deptId" :label="item.deptName" :value="item.deptName">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="预定人" prop="applicantName">
              <el-input :placeholder="form.applicantName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预订主题" prop="reservationTitle">
              <el-input v-model="form.reservationTitle" placeholder="请输入预订主题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-form-item label="预定日期" prop="selectedDate">
            <ul class="dateUl">
              <li v-for="(day, index) in recentSevenDays" :key="index">
                <el-button :class="{ 'is-primary': selectedDate === day.date && selectedDate !== null }"
                  :disabled="day.disabled" @click="selectDate(day.date)"
                  style="margin: 5px; min-width: 80px; height: 36px;">
                  {{ day.date }}
                </el-button>
              </li>
            </ul>
          </el-form-item>
          <el-form-item label="预定时间段" prop="selectedTime">
            <el-select v-model="selectedTime" placeholder="请选择时间段" style="width: 200px;" @change="updateStartTime">
              <el-option v-for="time in times" :key="time.value" :label="time.label" :value="time.value" />
            </el-select>
          </el-form-item>
        </el-row>

        <el-form-item label="预订用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" placeholder="请输入内容" />
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
import { listRoom, getRoom } from "@/api/system/room"
import { listReservation, addReservation, updateReservation } from "@/api/system/reservation.js";
import { listDept } from "@/api/system/dept";
const { proxy } = getCurrentInstance()
const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
import { useUserStore } from '@/store/modules/user'
const roomList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const userStore = useUserStore()
const recentSevenDays = ref([])  // 最近7天日期数据
const times = ref([
  { value: '07:00:00-08:00:00', label: '07:00:00-08:00:00' },
  { value: '08:00:00-09:00:00', label: '08:00:00-09:00:00' },
  { value: '09:00:00-10:00:00', label: '09:00:00-10:00:00' },
  { value: '10:00:00-11:00:00', label: '10:00:00-11:00:00' },
  { value: '11:00:00-12:00:00', label: '11:00:00-12:00:00' },
  { value: '12:00:00-13:00:00', label: '12:00:00-13:00:00' },
  { value: '13:00:00-14:00:00', label: '13:00:00-14:00:00' },
  { value: '14:00:00-15:00:00', label: '14:00:00-15:00:00' },
  { value: '15:00:00-16:00:00', label: '15:00:00-16:00:00' },
  { value: '16:00:00-17:00:00', label: '16:00:00-17:00:00' },
  { value: '17:00:00-18:00:00', label: '17:00:00-18:00:00' },
  { value: '18:00:00-19:00:00', label: '18:00:00-19:00:00' },
  { value: '19:00:00-20:00:00', label: '19:00:00-20:00:00' },
  { value: '20:00:00-21:00:00', label: '20:00:00-21:00:00' },
  { value: '21:00:00-22:00:00', label: '21:00:00-22:00:00' },
  { value: '22:00:00-23:00:00', label: '22:00:00-23:00:00' },
  { value: '23:00:00-24:00:00', label: '23:00:00-24:00:00' }
])
const { sys_room_status } = proxy.useDict('sys_room_status')
const deptList = ref([]);
const loadingDept = ref(false);
const reservationList = ref([]);
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    roomName: null,
    deptName: null,
    roomCode: null,
    location: null,
    capacity: null,
    roomType: null,
    status: null,
    description: null,
    isEnabled: null,
  },
  selectedDate: null,
  selectedTime: null,
  rules: {
    roomName: [
      { required: true, message: "会议室名称不能为空", trigger: "blur" }
    ],
    reservationTitle: [
      { required: true, message: "预订主题不能为空", trigger: "blur" }
    ],
    purpose: [
      { required: true, message: "预订用途不能为空", trigger: "blur" }
    ],
    applicantName: [
      { required: true, message: "预定人名称不能为空", trigger: "blur" }
    ],
    deptName: [
      { required: true, message: "预定部门不能为空", trigger: "blur" }
    ],
    selectedDate: [
      { required: true, message: "预定日期不能为空", trigger: "change" }
    ],
    selectedTime: [
      { required: true, message: "预定时间段不能为空", trigger: "change" }
    ]
  }
})
const selectDate = (dateStr) => {
  const selectedDay = recentSevenDays.value.find(day => day.date === dateStr)
  if (!selectedDay || selectedDay.disabled) return
  selectedDate.value = selectedDay.date
}
// 生成最近7天日期
const generateSevenDays = () => {
  const today = new Date()
  today.setHours(0, 0, 0, 0) // 将今天的时间重置为零点，用于比较
  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    const formattedDate = date.toISOString().split('T')[0]
    const isPast = date < today
    return {
      date: formattedDate,
      disabled: isPast
    }
  })
}
onMounted(() => {
  recentSevenDays.value = generateSevenDays()
})
// 加载全部部门
const loadAllDepts = () => {
  loadingDept.value = true;
  listDept({}).then(response => {
    deptList.value = response.data || [];
    loadingDept.value = false;
  }).catch(() => {
    loadingDept.value = false;
  });
};
const remoteMethodDept = (query) => {
  if (query) {
    loadingDept.value = true;
    listDept({
      status: undefined,
      remark: undefined,
      deptName: query
    }).then(response => {
      roomList.value.deptName = response.data.deptName;
      loadingDept.value = false;
    })
  } else {
    // 如果查询为空，加载全部部门
    loadAllDepts();
  }
};


const { queryParams, form, rules, selectedDate, selectedTime } = toRefs(data)

/** 查询会议室信息列表 */
function getList() {
  loading.value = true
  listRoom(queryParams.value).then(response => {
    roomList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function getReservationList() {
  listReservation({ pageSize: 1000 }).then(response => {
    reservationList.value = response.rows
  })
}
/**  格式化会议室状态 */
// function formatStatus(value) {
//   return String(value) === '0' ? '可预定' : '不能预定';
// }



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
  proxy.resetForm("reservationRef")
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

/** 预定按钮操作 */
function handleAdd(rows) {
  reset()
  loadAllDepts();
  open.value = true;
  form.value.userId = user.userId;
  form.value.deptId = user.deptId;
  form.value.roomId = rows.roomId;
  form.value.roomName = rows.roomName;
  form.value.applicantName = userStore.name;
  form.value.userId = userStore.id;
  selectedDate.value = null;
  form.value.selectedTime = selectedTime;
  title.value = "预定会议室信息"
}


function checkExactMatch(reservationList, form) {
  const currentRoomId = form.roomId;
  const currentStartTime = form.startTime;
  const currentEndTime = form.endTime;

  // 遍历已有预约，检查完全匹配
  return reservationList.some(room => {
    return (
      room.roomId === currentRoomId &&
      room.startTime === currentStartTime &&
      room.endTime === currentEndTime
    );
  });
}
/** 提交按钮 */
function submitForm() {
  form.value.selectedDate = selectedDate;
  form.value.selectedTime = selectedTime;
  if (selectedDate && selectedTime) {
    const [startTimePart, endTimePart] = selectedTime.value.split('-')
    form.value.startTime = `${selectedDate.value} ${startTimePart}`
    form.value.endTime = `${selectedDate.value} ${endTimePart}`
  }
  if (checkExactMatch(reservationList.value, form.value)) {
    proxy.$message.error('该会议室时间已被预约，请选择其他时间');
    return;
  }
  proxy.$refs["reservationRef"].validate(valid => {
    if (valid) {
      addReservation(form.value).then(response => {
        proxy.$modal.msgSuccess("新增预定成功");
        open.value = false;
        getList();
      });
    }
  });
}


getReservationList()
getList()
</script>
<style scoped>
.dateUl {
  display: flex;
  flex-wrap: wrap;
  list-style: none;
  padding: 0;
  margin: 0;
}

.dateUl li {
  margin-right: 10px;
  margin-bottom: 10px;
}

.time-preview {
  margin-top: 15px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
}

/* 自定义按钮样式 */
.el-button {
  transition: all 0.3s ease;
  border-radius: 4px;
  font-size: 12px;
}

/* 确保选中样式不被覆盖 */
.dateUl .el-button.is-primary {
  background-color: #409EFF !important;
  border-color: #409EFF !important;
  color: #fff !important;
}

.el-button.primary:hover {
  background-color: #3a8ee6;
  border-color: #3a8ee6;
}

.el-button.default {
  background-color: #fff;
  border-color: #dcdfe6;
  color: #606266;
}

.el-button.default:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
  color: #606266;
}

.el-button:disabled {
  background-color: #ebeef5;
  border-color: #ebeef5;
  color: #c0c4cc;
  cursor: not-allowed;
}
</style>