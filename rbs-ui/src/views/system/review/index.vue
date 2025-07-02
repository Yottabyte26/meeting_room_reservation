<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="85px">
      <el-form-item label="预订主题" prop="reservationTitle">
        <el-input v-model="queryParams.reservationTitle" placeholder="请输入预订主题" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable v-model="queryParams.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择开始时间" :picker-options="{
            disabledDate: (time) => {
              if (queryParams.endTime) {
                return time.getTime() > new Date(queryParams.endTime).getTime();
              }
              return false;
            }
          }"></el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable v-model="queryParams.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择结束时间" :picker-options="{
            disabledDate: (time) => {
              if (queryParams.startTime) {
                return time.getTime() < new Date(queryParams.startTime).getTime();
              }
              return false;
            }
          }"></el-date-picker>
      </el-form-item>
      <el-form-item label="预定人名称" prop="applicantName">
        <el-input v-model="queryParams.applicantName" placeholder="请输入预定人名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="reservationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="40" align="center" />
      <el-table-column label="预订ID" align="center" prop="reservationId" />
      <el-table-column label="会议室名称" align="center">
        <template #default="scope">
          <span>{{ getRoomName(scope.row.roomId) || '未知会议室' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预订主题" align="center" prop="reservationTitle" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160">
      </el-table-column>
      <el-table-column label="预订用途" align="center" prop="purpose" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_reservation_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="预定人名称" align="center" prop="applicantName" />
      <el-table-column label="预定部门名称" align="center" prop="deptName" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="130">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handlePass(scope.row)">通过</el-button>
          <el-button link type="danger" icon="Delete" @click="handleBack(scope.row)">退回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Reservation">
import { listReservation, getReservation, updateReservation } from "@/api/system/reservation"
import { listRoom } from "@/api/system/room"
import { addNotice } from "@/api/system/notice";
const { proxy } = getCurrentInstance()
const { sys_reservation_status } = proxy.useDict('sys_reservation_status')
const roomList = ref([])
const reservationList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    roomName: null,
    reservationTitle: null,
    startTime: null,
    endTime: null,
    purpose: null,
    participants: null,
    status: 0,
    applicantName: null,
    deptName: null
  },
  noticeQueryParams: {
    noticeTitle: undefined,
    noticeType: 1,
    noticeContent: undefined,
    status: "0",
    receiverType: 1,
    receiverId: undefined,
    receiverName: undefined
  },
})

const { queryParams, form, noticeQueryParams } = toRefs(data)

/** 查询会议室预订列表 */
function getList() {
  loading.value = true
  listReservation(queryParams.value).then(response => {
    reservationList.value = response.rows
    total.value = response.total
    loading.value = false
    if (reservationList.value.length > 0) {
      const roomIds = [...new Set(reservationList.value.map(item => item.roomId))];
      if (roomIds.length > 0) {
        // 根据会议室ID列表获取会议室详情
        getRoomInfoByIds(roomIds);
      }
    }
  })
}
// 根据会议室ID列表获取会议室信息
const getRoomInfoByIds = (roomIds) => {
  listRoom({ roomIds }).then(response => {
    roomList.value = response.rows;
  });
};

// 根据roomId获取会议室名称
const getRoomName = (roomId) => {
  const room = roomList.value.find(item => item.roomId === roomId);
  return room ? room.roomName : null;
};
// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    reservationId: null,
    roomId: null,
    userId: null,
    deptId: null,
    reservationTitle: null,
    startTime: null,
    endTime: null,
    purpose: null,
    participants: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    applicantName: null,
    deptName: null
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
  ids.value = selection.map(item => item.reservationId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleReservationAction(row, status, actionText, maxRemarkLength) {
  const reservationId = row.reservationId;
  return getReservation(reservationId)
    .then(response => {
      form.value = response.data;
      return proxy.$prompt(
        `是否确认${actionText}预定ID为<span style="color:red">"${reservationId}"</span>的预定？<br><br><b>请输入${actionText}原因：</b>`,
        `${actionText}确认`,
        {
          confirmButtonText: `确认${actionText}`,
          cancelButtonText: '取消',
          inputPlaceholder: `请输入${actionText}原因（必填）`,
          inputValidator: (value) => {
            if (!value.trim()) return `${actionText}原因不能为空`;
            if (value.length > maxRemarkLength) return `${actionText}原因长度不能超过${maxRemarkLength}个字符`;
            return true;
          },
          dangerouslyUseHTMLString: true
        }
      );
    })


    .then(({ value: reason }) => {
      form.value.status = status;
      form.value.remark = reason;
      return updateReservation(form.value);
    })
    .then(() => {
      noticeQueryParams.value.noticeTitle = `您${row.createTime}预约的主题为“${row.purpose}”，开始时间为${row.startTime}的会议室已${actionText}`;
      noticeQueryParams.value.receiverId = row.userId;
      noticeQueryParams.value.receiverName = row.applicantName;
      console.log("@@@", noticeQueryParams.value)
      return addNotice(noticeQueryParams.value);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(`${actionText}成功`);
    })
    .catch(error => {
      if (error && error !== 'cancel') {
        console.error(error);
        proxy.$modal.msgError(`${actionText}失败`);
      }
    });
}

/** 通过按钮操作 */
function handlePass(row) {
  return handleReservationAction(row, "1", "通过", 600);
}

/** 退回按钮操作 */
function handleBack(row) {
  return handleReservationAction(row, "2", "退回", 600);
}
getList()
</script>
