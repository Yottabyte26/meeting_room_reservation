<template>
  <div v-if="userStore.isAdmin" class="app-container">
    <!-- 顶部区域：标题 + 统计卡片 -->
    <div class="header-section">
      <div class="page-title">管理员页面统计</div>
      <div class="stats-card-group">
        <div class="stats-card">
          <div class="card-value">{{ totalUsers }}</div>
          <div class="card-label">用户总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ totalEmployees }}</div>
          <div class="card-label">部门总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ totalRooms }}</div>
          <div class="card-label">会议室总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ totalReservations }}</div>
          <div class="card-label">总预定次数</div>
        </div>
      </div>
    </div>

    <!-- 内容主体：统计图表区域 -->
    <div class="main-content">
      <!-- 部门员工统计饼图 -->
      <div class="chart-container">
        <div class="chart-title">部门员工统计</div>
        <div class="echarts-pie" ref="deptPieChart" style="width: 100%; height: 450px;"></div>
      </div>

      <!-- 用户性别统计饼图 -->
      <div class="chart-container">
        <div class="chart-title">用户性别统计</div>
        <div class="echarts-pie" ref="genderPieChart" style="width: 100%; height: 450px;"></div>
      </div>

      <!-- 会议室预定次数条形图 -->
      <div class="chart-container">
        <div class="chart-title">会议室预定次数统计</div>
        <div class="echarts-pie" ref="reservationBarChart" style="width: 100%; height: 550px;"></div>
      </div>
    </div>
  </div>

  <div v-else class="app-container">
    <!-- 顶部区域：标题 + 统计卡片 -->
    <div class="header-section">
      <div class="page-title">用户页面统计</div>
      <div class="stats-card-group">
        <div class="stats-card">
          <div class="card-value">{{ userStats.total_count || 0 }}</div>
          <div class="card-label">个人预定总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ userStats.pending_count || 0 }}</div>
          <div class="card-label">待审核总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ userStats.approved_count || 0 }}</div>
          <div class="card-label">通过总数</div>
        </div>
        <div class="stats-card">
          <div class="card-value">{{ userStats.rejected_count || 0 }}</div>
          <div class="card-label">退回总数</div>
        </div>
      </div>
    </div>

    <!-- 内容主体：统计图表区域 -->
    <div class="main-content">
      <!-- 个人预定审核条形图：x轴分别是待审核、通过、退回 -->
      <div class="chart-container">
        <div class="chart-title">会议室预定审核统计</div>
        <div class="echarts-pie1" ref="reviewBarChart" style="width: 100%; height: 550px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import * as echarts from 'echarts';
import { useUserStore } from '@/store/modules/user';
import {
  getDeptEmployeeStat,
  getGenderStat,
  getRoomReservationStat,
  getUserMeetingStats,
  getTotalReservations,
  getRoomReservations
} from '@/api/system/statistics';

// 引入用户存储
const userStore = useUserStore();

// 图表DOM引用（管理员）
const deptPieChart = ref(null);
const genderPieChart = ref(null);
const reservationBarChart = ref(null);
// 图表DOM引用（用户）
const reviewBarChart = ref(null);

// 图表实例
const chartInstances = {
  dept: null,
  gender: null,
  reservation: null
};

// 图表实例（用户）
const userChartInstance = ref(null);

// 加载状态
const loading = ref(true);

// 统计数据
const totalUsers = ref(0);
const totalEmployees = ref(0);
const totalRooms = ref(0);
const totalReservations = ref(0);
const departmentData = ref([]);
const genderData = ref([]);
const reservationData = ref([]);

// 用户模拟统计数据
const userStats = ref({
  total_count: 0,
  pending_count: 0,
  approved_count: 0,
  rejected_count: 0
});
// 用户图表模拟数据
const userChartData = ref([]);

// 颜色集合
const colorPalette = [
  '#4e79a7', '#59a14f', '#f28e2b', '#ff9da7', '#76b7b2', '#499894'
];
// 性别图表颜色
const genderColors = ['#41a5ee', '#ee6666'];
// 用户图表颜色
const userChartColors = ['#79201e', '#91cc75', '#ee6666'];

// 初始化部门统计图表
const initDeptChart = () => {
  if (chartInstances.dept) {
    chartInstances.dept.dispose();
  }

  chartInstances.dept = echarts.init(deptPieChart.value);

  const option = {
    title: {
      text: '部门员工统计',
      left: 'center',
      textStyle: {
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}人 ({d}%)',
      textStyle: { fontSize: 14 }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: departmentData.value.map(item => item.name),
      textStyle: {
        fontSize: 14
      }
    },
    series: [
      {
        name: '部门员工分布',
        type: 'pie',
        radius: ['45%', '75%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '20',
            fontWeight: 'bold',
            formatter: '{b}\n{c}人 ({d}%)'
          }
        },
        labelLine: {
          show: false
        },
        data: departmentData.value,
        color: colorPalette
      }
    ]
  };

  chartInstances.dept.setOption(option);
};


// 初始化性别统计图表
const initGenderChart = () => {
  if (!genderPieChart.value) return;
  console.log('性別表数据：', genderData);


  console.log('初始化性别图表，数据:', genderData.value);

  if (chartInstances.gender) {
    chartInstances.gender.dispose();
  }

  chartInstances.gender = echarts.init(genderPieChart.value);

  const option = {
    title: {
      text: '用户性别统计',
      left: 'center',
      textStyle: {
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}人 ({d}%)',
      textStyle: { fontSize: 14 }
    },
    legend: {
      orient: 'vertical',
      right: 15,
      top: 'center',
      textStyle: {
        fontSize: 14,
        color: '#666'
      }
    },
    series: [
      {
        name: '性别分布',
        type: 'pie',
        radius: '75%',
        center: ['50%', '50%'],
        data: genderData.value,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          fontSize: 16,
          formatter: '{b}: {c}人 ({d}%)'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 15,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        color: genderColors
      }
    ]
  };

  chartInstances.gender.setOption(option);
};

// 初始化预定统计图表
const initReservationChart = () => {
  if (!reservationBarChart.value) return;

  if (chartInstances.reservation) {
    chartInstances.reservation.dispose();
  }

  chartInstances.reservation = echarts.init(reservationBarChart.value);

  const option = {
    title: {
      text: '会议室预定次数统计',
      left: 'center',
      textStyle: {
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      textStyle: { fontSize: 14 }
    },
    xAxis: {
      type: 'category',
      data: reservationData.value.map(item => item.room_name),
      axisLabel: {
        interval: 0,
        rotate: 45, // 增大旋转角度
        fontSize: 12, // 减小字体大小
        margin: 15,
        width: 100,
        overflow: 'truncate', // 截断长文本
        ellipsis: '...' // 截断后显示省略号
      }
    },
    yAxis: {
      type: 'value',
      name: '预定次数',
      nameTextStyle: {
        fontSize: 16,
        padding: [30, 0, 10, -40]
      },
      axisLabel: {
        fontSize: 14
      }
    },
    series: [
      {
        name: '预定次数',
        type: 'bar',
        barWidth: '35%', // 减小柱状图宽度
        data: reservationData.value.map(item => item.reservation_count),
        itemStyle: {
          color: function (params) {
            const colorList = ['#91cc75', '#fac858', '#ee6666', '#5470c6', '#9a60b4', '#ea7ccc'];
            return colorList[params.dataIndex % colorList.length];
          },
          borderRadius: 6
        },
        label: {
          show: true,
          position: 'top',
          fontSize: 14,
          distance: 5,
          formatter: function (params) {
            return params.value > 0 ? params.value : ''; // 只显示大于0的值
          }
        }
      }
    ]
  };

  chartInstances.reservation.setOption(option);
};

// 初始化用户预定审核图表（用户）
const initUserReviewChart = () => {
  if (!reviewBarChart.value) return;

  if (userChartInstance.value) {
    userChartInstance.value.dispose();
  }

  userChartInstance.value = echarts.init(reviewBarChart.value);

  // 确保userChartData有数据
  if (userChartData.value.length === 0) {
    console.log('用户图表数据为空，使用默认数据');
    userChartData.value = [
      { name: '待审核', value: 0 },
      { name: '已通过', value: 0 },
      { name: '已退回', value: 0 }
    ];
  }

  const option = {
    title: {
      text: '个人预订审核状态',
      left: 'center',
      textStyle: {
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      textStyle: { fontSize: 14 }
    },
    xAxis: {
      type: 'category',
      data: userChartData.value.map(item => item.name),
      axisLabel: {
        fontSize: 14,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '次数',
      nameTextStyle: {
        fontSize: 16,
        padding: [30, 0, 10, -40]
      },
      axisLabel: {
        fontSize: 14
      }
    },
    series: [
      {
        type: 'bar', // 明确指定图表类型
        name: '次数',
        barWidth: '40%',
        data: userChartData.value.map(item => item.value),
        itemStyle: {
          color: function (params) {
            return userChartColors[params.dataIndex % userChartColors.length];
          },
          borderRadius: 6
        },
        label: {
          show: true,
          position: 'top',
          fontSize: 14,
          distance: 5,
          formatter: '{c}'
        }
      }
    ]
  };
  userChartInstance.value.setOption(option);
};

// 统一处理窗口大小变化
const handleResize = () => {
  if (chartInstances.dept) chartInstances.dept.resize();
  if (chartInstances.gender) chartInstances.gender.resize();
  if (chartInstances.reservation) chartInstances.reservation.resize();
  if (userChartInstance.value) userChartInstance.value.resize();
};

// 加载用户数据
const fetchUserStats = async () => {
  loading.value = true;
  try {
    const response = await getUserMeetingStats();
    console.log('用户统计数据:', response);
    if (response.code === 200) {
      userStats.value = response.data;
      userChartData.value = [
        { name: '待审核', value: userStats.value.pending_count || 0 },
        { name: '已通过', value: userStats.value.approved_count || 0 },
        { name: '已退回', value: userStats.value.rejected_count || 0 }
      ];

      // 验证数据有效性
      if (userChartData.value.some(item => item.value > 0)) {
        await nextTick();
        initUserReviewChart();
      } else {
        console.log('用户无预订记录，显示空图表');
        await nextTick();
        initUserReviewChart(); // 即使数据为0，也初始化图表
      }
    } else {
      console.error('接口错误:', response.msg);
    }
  } catch (error) {
    console.error('请求异常:', error);
  } finally {
    loading.value = false;
  }
};


// 加载管理员统计数据
const fetchAdminData = async () => {
  try {
    loading.value = true;

    const [deptRes, genderRes, reservationRes, totalRes, roomRes] = await Promise.all([
      getDeptEmployeeStat(),
      getGenderStat(),
      getRoomReservationStat(),
      getTotalReservations(),
      getRoomReservations()
    ]);

    console.log('部门数据:', deptRes.data);
    console.log('性别数据:', genderRes.data);
    console.log('预定数据:', reservationRes.data);

    // 处理部门员工数据（保留deptName为null的记录）
    departmentData.value = [];
    if (deptRes.data && Array.isArray(deptRes.data)) {
      departmentData.value = deptRes.data
        .filter(item => {
          return item.employeeCount !== undefined &&
            item.deptName &&
            !['未知部门', '未命名部门'].includes(item.deptName);
        })
        .map(item => ({
          value: item.employeeCount,
          name: item.deptName || '未命名部门' // 处理deptName为null的情况
        }));
      totalEmployees.value = departmentData.value.length;
      console.log('处理后的部门数据:', departmentData.value);
    }

    // 处理性别数据
    genderData.value = [];
    if (genderRes.data && Array.isArray(genderRes.data)) {
      genderData.value = genderRes.data
        .filter(item => item.genderName && item.genderCount !== undefined)
        .map(item => ({
          value: item.genderCount,
          name: item.genderName
        }));
      totalUsers.value = genderData.value.reduce(
        (sum, item) => sum + item.value,
        0
      );
      console.log('处理后的性别数据:', genderData.value);
    }

    // 处理总预订次数
    totalReservations.value = totalRes.data || 0;
    console.log('总预订次数:', totalReservations.value);

    // 处理会议室预订数据
    reservationData.value = [];
    if (roomRes.data && Array.isArray(roomRes.data)) {
      reservationData.value = roomRes.data.map(item => ({
        room_name: item.roomName || '未命名会议室',
        reservation_count: item.reservationCount || 0
      }));

      // 计算会议室总数（去重）
      const uniqueRooms = new Set(reservationData.value.map(item => item.room_name));
      totalRooms.value = uniqueRooms.size;
    } else {
      // 无数据时设置默认值
      reservationData.value = [];
      totalRooms.value = 0;
    }

    console.log('会议室预订数据:', reservationData.value);
    console.log('会议室总数:', totalRooms.value);

    // 等待DOM渲染完成
    await nextTick();

    console.log('DOM渲染完成，开始初始化图表');

    // 初始化图表
    if (departmentData.value.length > 0) {
      initDeptChart();
      console.log('部门图表初始化完成');
    }

    if (genderData.value.length > 0) {
      initGenderChart();
      console.log('性别图表初始化完成');
    }

    if (reservationData.value.length > 0) {
      initReservationChart();
      console.log('预订图表初始化完成');
    }

  } catch (error) {
    console.error('获取统计数据失败:', error);
  } finally {
    loading.value = false;
    console.log('数据加载流程结束');
  }
};

onMounted(() => {
  console.log('当前用户是否为管理员:', userStore.isAdmin);

  if (userStore.isAdmin) {
    fetchAdminData();
  } else {
    // 用户页面直接初始化模拟数据图表
    fetchUserStats();
  }

  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);

  if (chartInstances.dept) chartInstances.dept.dispose();
  if (chartInstances.gender) chartInstances.gender.dispose();
  if (chartInstances.reservation) chartInstances.reservation.dispose();
  if (userChartInstance.value) userChartInstance.value.dispose();
});
</script>


<style scoped lang="scss">
.app-container {
  padding: 30px;
  background-color: #fff;
  min-height: calc(100vh - 64px);
  box-sizing: border-box;

  .header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40px;

    .page-title {
      font-size: 28px;
      font-weight: bold;
      color: #333;
    }

    .stats-card-group {
      display: flex;
      gap: 20px;

      .stats-card {
        background-color: #f8f9fa;
        border: 1px solid #e9ecef;
        border-radius: 10px;
        padding: 20px 25px;
        min-width: 140px;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

        .card-value {
          font-size: 32px;
          font-weight: bold;
          color: #007bff;
          margin-bottom: 10px;
        }

        .card-label {
          font-size: 18px;
          color: #6c757d;
        }
      }
    }
  }

  .main-content {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
    gap: 30px;

    .chart-container {
      background-color: #f8f9fa;
      border: 1px solid #e9ecef;
      border-radius: 10px;
      padding: 30px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

      .chart-title {
        font-size: 20px;
        font-weight: bold;
        margin-bottom: 20px;
        color: #333;
      }
    }

    .chart-container:last-child {
      grid-column: span 2;
    }
  }
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  }

  .header-section {
    .stats-card-group {
      flex-wrap: wrap;
      justify-content: flex-start;

      .stats-card {
        min-width: calc(50% - 10px);
        margin-bottom: 15px;
      }
    }
  }
}

@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 25px;

    .stats-card-group {
      width: 100%;

      .stats-card {
        min-width: calc(50% - 10px);
      }
    }
  }

  .main-content {
    grid-template-columns: 1fr;
  }

  .chart-container {
    padding: 25px;
  }
}

@media (max-width: 480px) {
  .header-section {
    .stats-card-group {
      .stats-card {
        min-width: 100%;
      }
    }
  }
}

.echarts-pie1 {
  width: 100%;
  height: 100%;
}
</style>