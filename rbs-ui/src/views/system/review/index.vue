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
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="完成时间" align="center" prop="endTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预订用途" align="center" prop="purpose" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_reservation_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <!-- <el-table-column label="备注" align="center" prop="remark" /> -->
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
import { addNotice } from "@/api/system/notice.js";
import { listRoom } from "@/api/system/room"
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
  rules: {
    reservationTitle: [
      { required: true, message: "预订主题不能为空", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "开始时间不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "结束时间不能为空", trigger: "blur" }
    ],
    purpose: [
      { required: true, message: "预订用途不能为空", trigger: "blur" }
    ],
    participants: [
      { required: true, message: "参与人员不能为空", trigger: "blur" }
    ],
    applicantName: [
      { required: true, message: "预定人名称不能为空", trigger: "blur" }
    ],
    deptName: [
      { required: true, message: "预定部门名称不能为空", trigger: "change" }
    ]
  }

})

const { queryParams, form, rules } = toRefs(data)

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
function parseTime(time, cFormat) {
  if (arguments.length === 0) {
    return null;
  }
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}';
  let date;
  if (typeof time === 'object') {
    date = time;
  } else {
    if (typeof time === 'string' && /^[0-9]+$/.test(time)) {
      time = parseInt(time);
    }
    if (typeof time === 'number' && time.toString().length === 10) {
      time = time * 1000;
    }
    date = new Date(time);
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  };
  const time_str = format.replace(/{([ymdhisa])+}/g, (result, key) => {
    const value = formatObj[key];
    if (key === 'a') {
      return ['日', '一', '二', '三', '四', '五', '六'][value];
    }
    return value.toString().padStart(2, '0');
  });
  return time_str;
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

/** 通过按钮操作 */
function handlePass(row) {
  const _reservationId = row.reservationId;

  // 先获取预订详情
  getReservation(_reservationId)
    .then(response => {
      // 将数据赋值给表单
      this.form = response.data;

      // 弹出确认对话框
      return proxy.$prompt(
        // 拼接提示内容，用 HTML 标签做样式
        `是否确认将预订编号为<span style="color:red">"${_reservationId}"</span>的状态修改为通过？<br><br><b>请输入审核意见：</b>`,
        '通过确认',
        {
          confirmButtonText: '确认通过',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入审核意见（必填）',
          inputValidator: (value) => {
            if (!value.trim()) return '审核意见不能为空';
            if (value.length > 500) return '审核意见长度不能超过500个字符';
            return true;
          },
          // 关键配置：允许 message 解析 HTML 字符串
          dangerouslyUseHTMLString: true
        }
      );
    })

    .then(({ value: reason }) => {
      // 获取当前日期字符串（仅年-月-日）
      const now = new Date();
      const formatDate = (date) => {
        const pad = (n) => n.toString().padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
      };

      // 构造公告对象
      // const noticeForm = {
      //   noticeTitle: `温馨提示：${formatDate(now)} 您的会议室预定申请已通过`,
      //   noticeType: "1", //
      //   noticeContent: `您的会议室预定申请已通过。`,
      //   status: "0" //
      // };

      // 保存理由到表单中
      this.form.status = "1";  // 状态：通过
      this.form.remark = reason;

      // 先发通知，再更新预约信息
      // return addNotice(noticeForm);
    })
    .then(() => {
      return updateReservation(this.form);
    })
    .then(() => {
      // 更新成功后刷新列表并显示提示
      getList();
      proxy.$modal.msgSuccess("通过成功");
    })
    .catch(error => {
      // 处理取消或错误
      if (error && error !== 'cancel') {
        console.error(error);
        proxy.$modal.msgError("操作失败");
      }
    });
}
/** 退回按钮操作 */
function handleBack(row) {
  const _reservationId = row.reservationId;//提取预定id

  // 先获取预订详情
  getReservation(_reservationId)//调用前端封装的 getReservation 函数，向后端请求该预订的详细信息。
    //该函数会发送 GET 请求到 "/system/reservation/{reservationId}" 接口，后端根据 ID 查询数据库并返回预订详情。
    .then(response => {
      // 将数据赋值给表单
      this.form = response.data;

      // 弹出确认对话框
      return proxy.$prompt(
        `是否确认将预订编号为<span style="color:red">"${_reservationId}"</span>的状态修改为退回？<br><br><b>请输入审核意见：</b>`,
        '退回确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入退回理由（必填）',
        inputValidator: (value) => {
          if (!value.trim()) return '退回理由不能为空';
          if (value.length > 500) return '审核意见长度不能超过500个字符';
          return true;
        },
        dangerouslyUseHTMLString: true
      });
    })
    .then(({ value: reason }) => {//用于从弹窗的返回结果中提取 value 字段，并将其重命名为 reason。
      // 获取当前日期字符串（仅年-月-日）
      const now = new Date();
      const formatDate = (date) => {//将 Date 对象格式化为 YYYY-MM-DD 格式的字符串
        const pad = (n) => n.toString().padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
      };
      // 保存理由到表单中
      this.form.status = "2";  // 状态：退回
      this.form.remark = reason;
      //前端通过 updateReservation(this.form) 发送 PUT 请求到 /system/reservation 接口，将 this.form 作为请求体传递给后端。
    })
    .then(() => {
      return updateReservation(this.form);
      //调用前端封装的 updateReservation 函数，将修改后的预订数据（包含状态和审核意见）发送到后端。
    })
    .then(() => {
      // 更新成功后刷新列表并显示提示
      getList();
      proxy.$modal.msgSuccess("退回成功");
    })
    .catch(error => {
      if (error && error !== 'cancel') {
        console.error(error);
        proxy.$modal.msgError("操作失败");
      }
    });
}

getList()
</script>
