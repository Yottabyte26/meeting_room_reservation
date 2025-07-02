<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="预订主题" prop="reservationTitle">
                <el-input v-model="queryParams.reservationTitle" placeholder="请输入预订主题" clearable
                    @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
                <el-date-picker clearable v-model="queryParams.startTime" type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" :picker-options="{
                        disabledDate: (time) => {
                            if (queryParams.endTime) {
                                return time.getTime() > new Date(queryParams.endTime).getTime();
                            }
                            return false;
                        }
                    }"></el-date-picker>
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
                <el-date-picker clearable v-model="queryParams.endTime" type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" :picker-options="{
                        disabledDate: (time) => {
                            if (queryParams.startTime) {
                                return time.getTime() < new Date(queryParams.startTime).getTime();
                            }
                            return false;
                        }
                    }"></el-date-picker>
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
                    <el-option v-for="dict in sys_reservation_status" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
            </el-col>
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getListByapplicantName"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="reservationList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" align="center" width="30" />
            <el-table-column label="预订ID" align="center" prop="reservationId" width="70" />
            <el-table-column label="会议室名称" align="center" width="110">
                <template #default="scope">
                    <span>{{ getRoomName(scope.row) || '未知会议室' }}</span>
                </template>
            </el-table-column>
            <el-table-column label="预订主题" align="center" prop="reservationTitle" />
            <el-table-column label="开始时间" align="center" prop="startTime" width="100">
            </el-table-column>
            <el-table-column label="结束时间" align="center" prop="endTime" width="100">
            </el-table-column>
            <el-table-column label="预订用途" align="center" prop="purpose" />
            <el-table-column label="状态" align="center" prop="status" width="80">
                <template #default="scope">
                    <dict-tag :options="sys_reservation_status" :value="scope.row.status" />
                </template>
            </el-table-column>
            <el-table-column label="审核结果原因" align="center" prop="remark" />
            <el-table-column label="预定人名称" align="center" prop="applicantName" width="100" />
            <el-table-column label="预定部门名称" align="center" prop="deptName" width="110" />
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="130">
                <template #default="scope">
                    <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
                    <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="lisQueryParams.pageNum"
            v-model:limit="lisQueryParams.pageSize" @pagination="getListByapplicantName" />

        <!-- 添加或修改会议室预订对话框 -->
        <el-dialog :title="title" v-model="open" width="600px" append-to-body>
            <el-form ref="reservationRef" :model="form" :rules="rules" label-width="95px">
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="预定人名称" prop="applicantName">
                            <el-input v-model="form.applicantName" placeholder="请输入预定人名称" disabled />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="预订部门" prop="deptName">
                            <el-select v-model="form.deptName" placeholder="请选择部门" filterable clearable remote
                                :remote-method="remoteMethodDept">
                                <el-option v-for="item in deptList" :key="item.deptId" :label="item.deptName"
                                    :value="item.deptName">
                                </el-option>
                            </el-select>
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
                                <el-button :class="{ 'is-primary': selectedDate === day.date }" :disabled="day.disabled"
                                    @click="selectDate(day.date)" style="margin: 5px; min-width: 80px; height: 36px;">
                                    {{ day.date }}
                                </el-button>
                            </li>
                        </ul>
                    </el-form-item>
                    <el-form-item label="预定时间段" prop="selectedTime">
                        <el-select v-model="selectedTime" placeholder="请选择时间段" style="width: 200px;"
                            @change="updateStartTime">
                            <el-option v-for="time in times" :key="time.value" :label="time.label"
                                :value="time.value" />
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

<script setup name="Reservation">
import { listReservation, getReservation, delReservation, addReservation, updateReservation, getReservationByapplicantName } from "@/api/system/reservation"
import { useUserStore } from '@/store/modules/user'
import { listDept } from "@/api/system/dept";
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
const title = ref("")
const userStore = useUserStore()
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
        status: null,
        applicantName: null,
        deptName: null
    },
    lisQueryParams: {
        applicantName: userStore.name,
        pageNum: 1,
        pageSize: 10
    },
    selectedDate: null,
    selectedTime: null,
    rules: {
        reservationTitle: [
            { required: true, message: "预订主题不能为空", trigger: "blur" }
        ],
        purpose: [
            { required: true, message: "预订用途不能为空", trigger: "blur" }
        ],
        selectedDate: [
            { required: true, message: "预定日期不能为空", trigger: "change" }
        ],
        selectedTime: [
            { required: true, message: "预定时间段不能为空", trigger: "change" }
        ],
        applicantName: [
            { required: true, message: "预定人名称不能为空", trigger: "blur" }
        ],
        deptName: [
            { required: true, message: "预定部门名称不能为空", trigger: "change" }
        ]
    }

})
const recentSevenDays = ref([])
const times = ref([
    { value: '07:00:00-08:00:00', label: '7:00:00-8:00:00' },
    { value: '08:00:00-09:00:00', label: '8:00:00-09:00:00' },
    { value: '09:00:00-10:00:00', label: '9:00:00-10:00:00' },
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
const selectDate = (date) => {
    if (!date || date.disabled) return
    selectedDate.value = date
}
// 生成最近7天日期（包含是否禁用状态）
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
const loadingDept = ref(false);
const { queryParams, form, rules, selectedDate, selectedTime, lisQueryParams } = toRefs(data)
const remoteMethodDept = (query) => {
    loadAllDepts();
};
const deptList = ref([]);
const loadAllDepts = () => {
    loadingDept.value = true;
    listDept({}).then(response => {
        deptList.value = response.data || [];
        loadingDept.value = false;
    }).catch(() => {
        loadingDept.value = false;
    });
};
/** 查询会议室预订列表 */
function getList() {
    loading.value = true
    queryParams.value.applicantName = userStore.name;
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
const getListByapplicantName = () => {
    loading.value = true
    getReservationByapplicantName(lisQueryParams.value).then(response => {
        reservationList.value = response.rows
        total.value = response.total
        loading.value = false
    }).catch(error => {
        loading.value = false
    })
}

// 根据会议室ID列表获取会议室信息
const getRoomInfoByIds = (roomIds) => {
    listRoom({ roomIds }).then(response => {
        roomList.value = response.rows;
    });
};
const getRoomInfo = () => {
    listRoom(queryParams.value).then(response => {
        roomList.value = response.rows;
    });
};
// 根据roomId获取会议室名称
const getRoomName = (rows) => {
    const room = roomList.value.find(item => item.roomId === rows.roomId);
    return room ? room.roomName : null;
};
/** 删除按钮操作 */
function handleDelete(row) {
    const _reservationIds = row.reservationId || ids.value
    proxy.$modal.confirm('是否确认删除会议室预订编号为"' + _reservationIds + '"的数据项？').then(function () {
        return delReservation(_reservationIds)
    }).then(() => {
        getListByapplicantName()
        proxy.$modal.msgSuccess("删除成功")
    }).catch(() => { })
}
/** 修改按钮操作 */
function handleUpdate(row) {
    const endTime = new Date(row.endTime);
    const currentDate = new Date();
    if (endTime < currentDate) {
        proxy.$modal.msgError('该预定时间已过期，不可修改');
        return;
    }
    reset();
    loadAllDepts();
    const _reservationId = row.reservationId || ids.value
    getReservation(_reservationId).then(response => {
        form.value = response.data;
        selectedDate.value = form.value.startTime.split(' ')[0];
        const startTimePart = form.value.startTime.split(' ')[1];
        const endTimePart = form.value.endTime.split(' ')[1];
        selectedTime.value = `${startTimePart}-${endTimePart}`;
        form.value.selectedDate = selectedDate;
        form.value.selectedTime = selectedTime;
        open.value = true
        title.value = "修改会议室预订"
    })
}

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
    queryParams.value.applicantName = userStore.name;
    listReservation(queryParams.value).then(response => {
        reservationList.value = response.rows
        total.value = response.total;
    })
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

/** 提交按钮 */
function submitForm() {
    form.value.selectedDate = selectedDate;
    form.value.selectedTime = selectedTime;
    if (selectedDate && selectedTime) {
        const [startTimePart, endTimePart] = selectedTime.value.split('-')
        form.value.startTime = `${selectedDate.value} ${startTimePart}`
        form.value.endTime = `${selectedDate.value} ${endTimePart}`
    }
    const dept = deptList.value.find(item => item.deptName === form.value.deptName);
    form.value.deptId = dept.deptId;
    proxy.$refs["reservationRef"].validate(valid => {
        if (valid) {
            form.value.status = 0;
            form.value.remark = "";
            updateReservation(form.value).then(response => {
                proxy.$modal.msgSuccess("修改成功");
                open.value = false;
                getListByapplicantName();
            });
        }
    });
}


/** 导出按钮操作 */
function handleExport() {
    proxy.download('system/reservation/export', {
        ...queryParams.value
    }, `reservation_${new Date().getTime()}.xlsx`)
}
getRoomInfo();
getListByapplicantName();
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