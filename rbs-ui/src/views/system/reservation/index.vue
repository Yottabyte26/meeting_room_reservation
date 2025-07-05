<template>
  <div class="app-container">
    <!-- 搜索表单 -->
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

    <!-- 卡片式列表 -->
    <div class="card-list">
      <div 
        v-for="(item, index) in roomList" 
        :key="index" 
        class="room-card"
      >
        <!-- 图片预览 -->
        <div class="card-header">
          <image-preview 
            :src="item.roomImage" 
            :width="100" 
            :height="80"
          ></image-preview>
       <!-- 修改 card-title 结构 -->
<div class="card-title">
  <span>{{ item.roomName }}</span>
  <!-- 状态标签单独一行 -->
  <span class="status-tag" :class="item.status === '0' ? 'available' : 'unavailable'">
    {{ item.status === '0' ? '可预定' : '不可预定' }}
  </span>
</div>
        </div>

        <!-- 卡片内容 -->
        <div class="card-content">
          <p><strong>编号：</strong>{{ item.roomCode }}</p>
          <p><strong>位置：</strong>{{ item.location }}</p>
          <p><strong>容纳人数：</strong>{{ item.capacity }}</p>
          <p><strong>类型：</strong>{{ item.roomType }}</p>
          <p><strong>描述：</strong>{{ item.description }}</p>
        </div>

        <!-- 操作按钮 -->
        <div class="card-actions">
          <el-button 
            v-if="item.status === '0'" 
            type="primary" 
            icon="Edit" 
            @click="handleAdd(item)"
          >
            预定
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <pagination 
      v-show="total > 0" 
      :total="total" 
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" 
      @pagination="getList" 
    />


 <!-- 预定弹窗-->
    <el-dialog :title="title" v-model="open" width="600px"  style="position: fixed; top: 40%; left: 50%; transform: translate(-50%, -50%);" append-to-body>
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
              <el-option v-for="time in filteredTimes" :key="time.value" :label="time.label" :value="time.value" />
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

// 计算属性：根据选中日期过滤可用时间段
const filteredTimes = computed(() => {
  if (!data.selectedDate) return [];
  
  const now = new Date();
  const selectedDate = new Date(data.selectedDate);
  const isToday = selectedDate.toDateString() === now.toDateString();
  
  // 对于今天的日期，需要过滤掉已过的时间段（考虑提前1小时）
  if (isToday) {
    // 当前时间加1小时，作为最早可预约开始时间
    const earliestStart = new Date(now.getTime() + 30 * 60 * 1000);
    return times.value.filter(time => {
      const startTimeStr = time.value.split('-')[0];
      const [hours, minutes, seconds] = startTimeStr.split(':').map(Number);
      const startTime = new Date();
      startTime.setHours(hours, minutes, seconds);
      
      // 时间段开始时间必须晚于当前时间加半小时
      return startTime >= earliestStart;
    });
  }
  
  // 非今天的日期，所有时间段都可用
  return times.value;
});


const selectDate = (dateStr) => {
  const selectedDay = recentSevenDays.value.find(day => day.date === dateStr)
  if (!selectedDay || selectedDay.disabled) return
  data.selectedDate = selectedDay.date;
  // 重置时间段选择
  data.selectedTime = null; 
}
// 生成最近7天日期
const generateSevenDays = () => {
  const today = new Date();
  today.setHours(0, 0, 0, 0); // 设置为今天凌晨

  return Array.from({ length: 7 }, (_, i) => {
    const date = new Date(today);
    date.setDate(today.getDate() + i);

    // 使用本地时间格式化日期，避免时区问题
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;

    const isPast = date < today;

    return {
      date: formattedDate,
      disabled: isPast
    };
  });
};
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
  data.selectedDate = null; 
  data.selectedTime = null; 
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
  // 修正：使用正确的响应式数据引用
  form.value.selectedDate = data.selectedDate;
  form.value.selectedTime = data.selectedTime;
  
  // 验证是否选择了日期和时间段
  if (!data.selectedDate || !data.selectedTime) {
    proxy.$message.error('请选择预约日期和时间段');
    return;
  }
  
  // 解析开始和结束时间
  const [startTimePart, endTimePart] = data.selectedTime.split('-');
  const startTime = new Date(`${data.selectedDate} ${startTimePart}`);
  const endTime = new Date(`${data.selectedDate} ${endTimePart}`);
  
  // 验证时间逻辑
  const now = new Date();
  
  // 检查是否至少提前1小时预约
  if (startTime - now < 30 * 60 * 1000) {
    proxy.$message.error('预约需至少提前1小时，请选择合适的时间');
    return;
  }
  
  // 检查开始时间是否早于结束时间
  if (startTime >= endTime) {
    proxy.$message.error('开始时间不能晚于结束时间');
    return;
  }
  
  // 设置表单中的时间值
  form.value.startTime = `${data.selectedDate} ${startTimePart}`;
  form.value.endTime = `${data.selectedDate} ${endTimePart}`;
  
  // 检查是否已被预约
  if (checkExactMatch(reservationList.value, form.value)) {
    proxy.$message.error('该会议室时间已被预约，请选择其他时间');
    return;
  }
  
  // 表单验证
  proxy.$refs["reservationRef"].validate(valid => {
    if (valid) {
      addReservation(form.value).then(response => {
        proxy.$modal.msgSuccess("新增预定成功");
        open.value = false;
        getList();           // 刷新会议室列表
        getReservationList();// 刷新预约列表
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

.card-list {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 20px;
}

/* 单个卡片样式 */
.room-card {
  width: calc(25% - 24px); /* 每行4张卡片 */
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 鼠标悬停动画 */
.room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 卡片头部（图片 + 标题） */
.card-header {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.card-header image-preview {
  margin-right: 16px;
}

.card-title {
  display: flex;
  flex-direction: column;
}

.card-title span {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 4px;
  margin-left: 10px;
}

/* 状态标签 */
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 10px;
  color: #fff;
  margin-top: 4px;
  width: 80px; /* 固定宽度 */
  display: inline-block; /* 确保宽度生效 */
  text-align: center; /* 文字居中 */
  box-sizing: border-box; /* 包含padding */
}

.status-tag.available {
  background-color: #67c23a; /* 可预定 */
}

.status-tag.unavailable {
  background-color: #f56c6c; /* 不可预定 */
}

/* 卡片内容 */
.card-content {
  padding: 16px;
  line-height: 1.6;
  color: #666;
}

.card-content p {
  margin: 0 0 8px 0;
}

/* 卡片操作按钮 */
.card-actions {
  padding: 16px;
  border-top: 1px solid #f5f5f5;
  text-align: right;
}

/* 响应式布局：屏幕变小时调整卡片数量 */
@media (max-width: 1200px) {
  .room-card {
    width: calc(33.33% - 24px);
  }
}

@media (max-width: 992px) {
  .room-card {
    width: calc(50% - 24px);
  }
}

@media (max-width: 768px) {
  .room-card {
    width: 100%;
  }
}

</style>