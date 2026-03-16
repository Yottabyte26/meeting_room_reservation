<template>
    <div v-if="userStore.isAdmin" class="app-container">
        <!-- 顶部区域：标题 + 统计卡片 -->
        <div class="header-section">
            <div class="title-btn-group">
                <div class="page-title">管理员页面统计</div>
                <!-- 语音智能预约按钮 -->
                <button class="voice-book-btn" @click="handleVoiceBook" :disabled="isRecording">{{ isRecording ? '🎤 录音中...' : '🎤 语音预约会议室' }}</button>
            </div>
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
                <div class="echarts-pie" ref="deptPieChart" style="width: 100%; height: 450px"></div>
            </div>

            <!-- 用户性别统计饼图 -->
            <div class="chart-container">
                <div class="chart-title">用户性别统计</div>
                <div class="echarts-pie" ref="genderPieChart" style="width: 100%; height: 450px"></div>
            </div>

            <!-- 会议室预定次数条形图 -->
            <div class="chart-container">
                <div class="chart-title">会议室预定次数统计</div>
                <div class="echarts-pie" ref="reservationBarChart" style="width: 100%; height: 550px"></div>
            </div>
        </div>
    </div>

    <div v-else class="app-container">
        <!-- 顶部区域：标题 + 统计卡片 -->
        <div class="header-section">
            <div class="title-btn-group">
                <div class="page-title">用户页面统计</div>
                <!-- 语音智能预约按钮 -->
                <button class="voice-book-btn" @click="handleVoiceBook" :disabled="isRecording">{{ isRecording ? '🎤 录音中...' : '🎤 语音预约会议室' }}</button>
            </div>
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
                <div class="echarts-pie1" ref="reviewBarChart" style="width: 100%; height: 550px"></div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptEmployeeStat, getGenderStat, getRoomReservationStat, getUserMeetingStats, getTotalReservations, getRoomReservations } from '@/api/system/statistics'
import { voiceBookMeetingRoom, parseVoiceCommand } from '@/api/system/reservation'
import request from '@/utils/request'

// 引入用户存储
const userStore = useUserStore()

// 图表DOM引用（管理员）
const deptPieChart = ref(null)
const genderPieChart = ref(null)
const reservationBarChart = ref(null)
// 图表DOM引用（用户）
const reviewBarChart = ref(null)

// 图表实例
const chartInstances = {
    dept: null,
    gender: null,
    reservation: null
}

// 图表实例（用户）
const userChartInstance = ref(null)

// 加载状态
const loading = ref(true)

// 统计数据
const totalUsers = ref(0)
const totalEmployees = ref(0)
const totalRooms = ref(0)
const totalReservations = ref(0)
const departmentData = ref([])
const genderData = ref([])
const reservationData = ref([])

// 用户模拟统计数据
const userStats = ref({
    total_count: 0,
    pending_count: 0,
    approved_count: 0,
    rejected_count: 0
})
// 用户图表模拟数据
const userChartData = ref([])

// 颜色集合
const colorPalette = ['#4e79a7', '#59a14f', '#f28e2b', '#ff9da7', '#76b7b2', '#499894']
// 性别图表颜色
const genderColors = ['#41a5ee', '#ee6666']
// 用户图表颜色
const userChartColors = ['#fac858', '#91cc75', '#ee6666']

// 录音相关变量
const isRecording = ref(false)
let mediaRecorder = null
let audioChunks = []
let stream = null
let countdownTimer = null // 倒计时定时器
let remainingTime = 30 // 剩余录音时间（秒）
let messageBoxInstance = null // 弹窗实例

// 语音预约核心方法
const handleVoiceBook = async () => {
    try {
        // 1. 检查浏览器支持性
        if (!navigator.mediaDevices || !window.MediaRecorder) {
            ElMessage.error('当前浏览器不支持录音功能，请使用Chrome/Edge最新版浏览器')
            return
        }

        // 重置录音状态
        resetRecordingState()

        // 2. 显示录音确认弹窗
        remainingTime = 30
        await ElMessageBox.confirm(
            `<div id="recording-countdown">
                <p>请确保环境安静，说出指令如："预定明天下午四点十人会议室"</p>
                <p style="color: #f28e2b; font-size: 16px; margin-top: 10px;">剩余录音时间：<span id="countdown-number">${remainingTime}</span> 秒</p>
            </div>`,
            '语音预约',
            {
                confirmButtonText: '开始录音',
                cancelButtonText: '取消',
                type: 'info',
                dangerouslyUseHTMLString: true,
                // 核心修改：使用 beforeClose 控制流程
                beforeClose: async (action, instance, done) => {
                    if (action === 'confirm') {
                        // 【关键修改】使用 instance.confirmButtonLoading 控制状态
                        instance.confirmButtonLoading = true
                        instance.confirmButtonText = '录音中...'

                        try {
                            // 3. 获取麦克风权限并开始录音
                            await startRecording()

                            // 只有录音成功启动才关闭当前弹窗
                            done()

                            // 启动倒计时
                            startCountdown()
                        } catch (error) {
                            // 失败时恢复按钮状态
                            instance.confirmButtonLoading = false
                            instance.confirmButtonText = '开始录音'
                            ElMessage.error(`录音启动失败：${error.message}`)
                            // 关闭弹窗
                            done()
                        }
                    } else {
                        // 取消操作
                        done()
                    }
                }
            }
        )
    } catch (error) {
        // 捕获取消操作或初始化错误
        if (error === 'cancel') {
            ElMessage.info('已取消录音')
            resetRecordingState()
        } else {
            console.error('录音初始化失败:', error)
            ElMessage.error('录音初始化失败：' + error.message)
            resetRecordingState()
        }
    }
}
// 开始录音
// 开始录音
const startRecording = async () => {
    // 3. 获取麦克风权限
    stream = await navigator.mediaDevices.getUserMedia({
        audio: {
            sampleRate: 16000,
            channelCount: 1,
            sampleSize: 16,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true
        }
    })

    // 4. 初始化MediaRecorder（兼容多浏览器）- 修复mimeType未定义问题
    let mimeType = 'audio/webm' // 先定义默认值，避免未定义
    if (MediaRecorder.isTypeSupported('audio/wav')) {
        mimeType = 'audio/wav'
    } else if (MediaRecorder.isTypeSupported('audio/mp3')) {
        mimeType = 'audio/mp3'
    } else if (MediaRecorder.isTypeSupported('audio/mpeg')) {
        mimeType = 'audio/mpeg'
    }

    // 关键：传入定义好的mimeType
    mediaRecorder = new MediaRecorder(stream, { mimeType: mimeType })
    audioChunks = []

    // 5. 监听录音数据
    mediaRecorder.ondataavailable = e => {
        if (e.data.size > 0) audioChunks.push(e.data)
    }

    // 6. 录音结束处理
    mediaRecorder.onstop = async () => {
        // 清除倒计时
        clearInterval(countdownTimer)

        // 立即释放麦克风
        if (stream) stream.getTracks().forEach(t => t.stop())

        try {
            ElMessage.info('正在处理音频数据...')
            // 7. 转换音频格式 - 这里要使用定义好的mimeType，而非硬编码
            const audioBlob = new Blob(audioChunks, { type: mimeType })
            // 新增：转码为标准WAV（解决格式不兼容问题）
            const wavBlob = await convertToWav(audioBlob)
            const audioFile = new File([wavBlob], `voice-${Date.now()}.wav`, { type: 'audio/wav' })

            // 8. 校验文件
            if (audioFile.size < 1024) {
                ElMessage.error('录音内容为空，请重新录制（确保环境安静，清晰说话）')
                return
            }

            // 9. 提交录音文件
            await submitVoiceBookFile(audioFile)
        } catch (err) {
            ElMessage.error('录音处理失败：' + err.message)
        } finally {
            // 重置变量
            resetRecordingState()
            isRecording.value = false
        }
    }

    // 显示录音弹窗（带结束按钮）
    showRecordingDialog()

    // 10. 开始录音
    mediaRecorder.start(100)
    isRecording.value = true
    ElMessage.success('🎤 录音已开始，请清晰说出预约指令')
}

// 新增：音频转码函数（将浏览器录音转为标准WAV格式）
const convertToWav = async blob => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = async e => {
            try {
                // 使用音频上下文解码原始数据
                const audioContext = new (window.AudioContext || window.webkitAudioContext)({
                    sampleRate: 16000
                })
                const arrayBuffer = e.target.result
                const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)

                // 将音频数据转为16bit PCM格式
                const numberOfChannels = 1 // 强制单声道
                const sampleRate = 16000 // 强制16k采样率
                const format = 1 // PCM格式

                const offlineContext = new OfflineAudioContext(numberOfChannels, audioBuffer.length * (sampleRate / audioBuffer.sampleRate), sampleRate)
                const source = offlineContext.createBufferSource()
                source.buffer = audioBuffer
                source.connect(offlineContext.destination)
                source.start()

                // 渲染为标准格式
                const renderedBuffer = await offlineContext.startRendering()
                const pcmData = renderedBuffer.getChannelData(0)

                // 转换为16bit数组
                const wavData = new Uint16Array(pcmData.length)
                for (let i = 0; i < pcmData.length; i++) {
                    const sample = Math.max(-1, Math.min(1, pcmData[i]))
                    wavData[i] = sample < 0 ? sample * 0x8000 : sample * 0x7fff
                }

                // 构建WAV文件头
                const wavBuffer = new ArrayBuffer(44 + wavData.length * 2)
                const view = new DataView(wavBuffer)

                // 写入WAV文件头
                view.setUint32(0, 0x52494646, false) // "RIFF"
                view.setUint32(4, 36 + wavData.length * 2, true) // 文件长度
                view.setUint32(8, 0x57415645, false) // "WAVE"
                view.setUint32(12, 0x666d7420, false) // "fmt "
                view.setUint32(16, 16, true) // 格式块长度
                view.setUint16(20, format, true) // 格式（1=PCM）
                view.setUint16(22, numberOfChannels, true) // 声道数
                view.setUint32(24, sampleRate, true) // 采样率
                view.setUint32(28, sampleRate * numberOfChannels * 2, true) // 字节率
                view.setUint16(32, numberOfChannels * 2, true) // 块对齐
                view.setUint16(34, 16, true) // 位深度
                view.setUint32(36, 0x64617461, false) // "data"
                view.setUint32(40, wavData.length * 2, true) // 数据长度

                // 写入PCM数据
                const dataView = new DataView(wavBuffer, 44)
                for (let i = 0; i < wavData.length; i++) {
                    dataView.setInt16(i * 2, wavData[i], true)
                }

                // 返回WAV格式Blob
                resolve(new Blob([wavBuffer], { type: 'audio/wav' }))
            } catch (error) {
                reject(error)
            }
        }
        reader.onerror = reject
        reader.readAsArrayBuffer(blob)
    })
}

// 显示录音中的弹窗（带倒计时和结束按钮）
// 显示录音中的弹窗
const showRecordingDialog = () => {
    // 初始更新倒计时显示
    const updateCountdown = () => {
        const el = document.getElementById('countdown-number')
        if (el) el.textContent = remainingTime
    }
    updateCountdown()

    // 创建录音中的弹窗
    ElMessageBox.confirm(
        `<div>
            <p style="font-size: 16px;">🎤 正在录音...</p>
            <p style="color: #f28e2b; margin: 10px 0;">剩余时间：<span id="countdown-number">${remainingTime}</span> 秒</p>
            <p style="font-size: 14px; color: #666;">请清晰说出您的预约需求</p>
        </div>`,
        '语音录音中',
        {
            confirmButtonText: '结束录音',
            cancelButtonText: '取消录音',
            type: 'warning',
            dangerouslyUseHTMLString: true,
            // 关键修改：使用 Loading 状态禁用按钮
            beforeClose: (action, instance, done) => {
                // 禁用按钮防止重复点击
                instance.confirmButtonLoading = true
                instance.cancelButtonLoading = true
                instance.confirmButtonText = '处理中...'

                if (mediaRecorder && mediaRecorder.state === 'recording') {
                    mediaRecorder.stop()
                    ElMessage.info('录音已结束，正在识别语音指令...')
                }

                // 延迟一点关闭弹窗，让用户看到状态变化（可选）
                setTimeout(() => {
                    done()
                }, 200)
            }
        }
    ).catch(error => {
        if (error === 'cancel' && mediaRecorder && mediaRecorder.state === 'recording') {
            mediaRecorder.stop()
            ElMessage.info('已取消录音')
        }
    })
}

// 启动倒计时
const startCountdown = () => {
    countdownTimer = setInterval(() => {
        remainingTime--
        const countdownElement = document.getElementById('countdown-number')
        if (countdownElement) {
            countdownElement.textContent = remainingTime
        }

        // 倒计时结束自动停止录音
        if (remainingTime <= 0) {
            clearInterval(countdownTimer)
            if (mediaRecorder && mediaRecorder.state === 'recording') {
                mediaRecorder.stop()

                // 关闭所有弹窗
                if (messageBoxInstance) {
                    messageBoxInstance.close()
                }

                ElMessage.info('录音时间已到，自动结束录音，正在识别...')
            }
        }
    }, 1000)
}

// 重置录音状态
const resetRecordingState = () => {
    // 清除倒计时
    if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
    }

    // 停止录音
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop()
    }

    // 释放麦克风
    if (stream) {
        stream.getTracks().forEach(t => t.stop())
        stream = null
    }

    // 重置变量
    mediaRecorder = null
    audioChunks = []
    remainingTime = 30
    isRecording.value = false
    messageBoxInstance = null
}

// 提交音频文件
const submitVoiceBookFile = async audioFile => {
    try {
        ElMessage.info('正在识别语音指令...')
        // 调用封装的接口
        const result = await voiceBookMeetingRoom(audioFile)
        if (result.code === 200) {
            ElMessage.success(`🎉 ${result.msg}`)
            // 刷新数据
            userStore.isAdmin ? fetchAdminData() : fetchUserStats()
        } else {
            // 针对性提示百度错误码
            if (result.msg.includes('3301')) {
                ElMessage.error('❌ 语音质量过低：请在安静环境下清晰说话，靠近麦克风')
            } else if (result.msg.includes('3307')) {
                // 新增3307错误处理
                ElMessage.error('❌ 语音识别失败：无法识别有效语音内容，请重新录制（请用普通话清晰说出预约需求）')
            } else {
                ElMessage.error(`❌ 预约失败：${result.msg}`)
            }
        }
    } catch (error) {
        console.error('接口调用失败:', error)
        ElMessage.error('网络异常或服务器错误，请稍后重试')
    }
}

// 初始化部门统计图表
const initDeptChart = () => {
    if (chartInstances.dept) {
        chartInstances.dept.dispose()
    }

    chartInstances.dept = echarts.init(deptPieChart.value)

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
    }

    chartInstances.dept.setOption(option)
}

// 初始化性别统计图表
const initGenderChart = () => {
    if (!genderPieChart.value) return
    console.log('性別表数据：', genderData)

    console.log('初始化性别图表，数据:', genderData.value)

    if (chartInstances.gender) {
        chartInstances.gender.dispose()
    }

    chartInstances.gender = echarts.init(genderPieChart.value)

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
    }

    chartInstances.gender.setOption(option)
}

// 初始化预定统计图表
const initReservationChart = () => {
    if (!reservationBarChart.value) return

    if (chartInstances.reservation) {
        chartInstances.reservation.dispose()
    }

    chartInstances.reservation = echarts.init(reservationBarChart.value)

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
                        const colorList = ['#91cc75', '#fac858', '#ee6666', '#5470c6', '#9a60b4', '#ea7ccc']
                        return colorList[params.dataIndex % colorList.length]
                    },
                    borderRadius: 6
                },
                label: {
                    show: true,
                    position: 'top',
                    fontSize: 14,
                    distance: 5,
                    formatter: function (params) {
                        return params.value > 0 ? params.value : '' // 只显示大于0的值
                    }
                }
            }
        ]
    }

    chartInstances.reservation.setOption(option)
}

// 初始化用户预定审核图表（用户）
const initUserReviewChart = () => {
    if (!reviewBarChart.value) return

    if (userChartInstance.value) {
        userChartInstance.value.dispose()
    }

    userChartInstance.value = echarts.init(reviewBarChart.value)

    // 确保userChartData有数据
    if (userChartData.value.length === 0) {
        console.log('用户图表数据为空，使用默认数据')
        userChartData.value = [
            { name: '待审核', value: 0 },
            { name: '已通过', value: 0 },
            { name: '已退回', value: 0 }
        ]
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
                        return userChartColors[params.dataIndex % userChartColors.length]
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
    }
    userChartInstance.value.setOption(option)
}

// 统一处理窗口大小变化
const handleResize = () => {
    if (chartInstances.dept) chartInstances.dept.resize()
    if (chartInstances.gender) chartInstances.gender.resize()
    if (chartInstances.reservation) chartInstances.reservation.resize()
    if (userChartInstance.value) userChartInstance.value.resize()
}

// 加载用户数据
const fetchUserStats = async () => {
    loading.value = true
    try {
        const response = await getUserMeetingStats()
        console.log('用户统计数据:', response)
        if (response.code === 200) {
            userStats.value = response.data
            userChartData.value = [
                { name: '待审核', value: userStats.value.pending_count || 0 },
                { name: '已通过', value: userStats.value.approved_count || 0 },
                { name: '已退回', value: userStats.value.rejected_count || 0 }
            ]

            // 验证数据有效性
            if (userChartData.value.some(item => item.value > 0)) {
                await nextTick()
                initUserReviewChart()
            } else {
                console.log('用户无预订记录，显示空图表')
                await nextTick()
                initUserReviewChart() // 即使数据为0，也初始化图表
            }
        } else {
            console.error('接口错误:', response.msg)
        }
    } catch (error) {
        console.error('请求异常:', error)
    } finally {
        loading.value = false
    }
}

// 加载管理员统计数据
const fetchAdminData = async () => {
    try {
        loading.value = true

        const [deptRes, genderRes, reservationRes, totalRes, roomRes] = await Promise.all([getDeptEmployeeStat(), getGenderStat(), getRoomReservationStat(), getTotalReservations(), getRoomReservations()])

        console.log('部门数据:', deptRes.data)
        console.log('性别数据:', genderRes.data)
        console.log('预定数据:', reservationRes.data)

        // 处理部门员工数据（保留deptName为null的记录）
        departmentData.value = []
        if (deptRes.data && Array.isArray(deptRes.data)) {
            departmentData.value = deptRes.data
                .filter(item => {
                    return item.employeeCount !== undefined && item.deptName && !['未知部门', '未命名部门'].includes(item.deptName)
                })
                .map(item => ({
                    value: item.employeeCount,
                    name: item.deptName || '未命名部门' // 处理deptName为null的情况
                }))
            totalEmployees.value = departmentData.value.length
            console.log('处理后的部门数据:', departmentData.value)
        }

        // 处理性别数据
        genderData.value = []
        if (genderRes.data && Array.isArray(genderRes.data)) {
            genderData.value = genderRes.data
                .filter(item => item.genderName && item.genderCount !== undefined)
                .map(item => ({
                    value: item.genderCount,
                    name: item.genderName
                }))
            totalUsers.value = genderData.value.reduce((sum, item) => sum + item.value, 0)
            console.log('处理后的性别数据:', genderData.value)
        }

        // 处理总预订次数
        totalReservations.value = totalRes.data || 0
        console.log('总预订次数:', totalReservations.value)

        // 处理会议室预订数据
        reservationData.value = []
        if (roomRes.data && Array.isArray(roomRes.data)) {
            reservationData.value = roomRes.data.map(item => ({
                room_name: item.roomName || '未命名会议室',
                reservation_count: item.reservationCount || 0
            }))

            // 计算会议室总数（去重）
            const uniqueRooms = new Set(reservationData.value.map(item => item.room_name))
            totalRooms.value = uniqueRooms.size
        } else {
            // 无数据时设置默认值
            reservationData.value = []
            totalRooms.value = 0
        }

        console.log('会议室预订数据:', reservationData.value)
        console.log('会议室总数:', totalRooms.value)

        // 等待DOM渲染完成
        await nextTick()

        console.log('DOM渲染完成，开始初始化图表')

        // 初始化图表
        if (departmentData.value.length > 0) {
            initDeptChart()
            console.log('部门图表初始化完成')
        }

        if (genderData.value.length > 0) {
            initGenderChart()
            console.log('性别图表初始化完成')
        }

        if (reservationData.value.length > 0) {
            initReservationChart()
            console.log('预订图表初始化完成')
        }
    } catch (error) {
        console.error('获取统计数据失败:', error)
    } finally {
        loading.value = false
        console.log('数据加载流程结束')
    }
}

onMounted(() => {
    console.log('当前用户是否为管理员:', userStore.isAdmin)

    if (userStore.isAdmin) {
        fetchAdminData()
    } else {
        // 用户页面直接初始化模拟数据图表
        fetchUserStats()
    }

    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)

    if (chartInstances.dept) chartInstances.dept.dispose()
    if (chartInstances.gender) chartInstances.gender.dispose()
    if (chartInstances.reservation) chartInstances.reservation.dispose()
    if (userChartInstance.value) userChartInstance.value.dispose()
})

// 组件卸载时强制释放资源
onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    // 销毁录音资源
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop()
    }
    if (stream) stream.getTracks().forEach(t => t.stop())
    // 销毁图表
    if (chartInstances.dept) chartInstances.dept.dispose()
    if (chartInstances.gender) chartInstances.gender.dispose()
    if (chartInstances.reservation) chartInstances.reservation.dispose()
    if (userChartInstance.value) userChartInstance.value.dispose()
})
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

        .title-btn-group {
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .page-title {
            font-size: 28px;
            font-weight: bold;
            color: #333;
        }

        .voice-book-btn {
            background: linear-gradient(135deg, #6a85b6 0%, #bac8e0 100%);
            color: white;
            border: none;
            border-radius: 8px;
            padding: 25px 25px;
            font-size: 25px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow: 0 4px 12px rgba(106, 133, 182, 0.3);

            &:disabled {
                background: #ccc;
                cursor: not-allowed;
                transform: none !important;
                box-shadow: none !important;
                opacity: 0.8;
            }

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 6px 16px rgba(106, 133, 182, 0.4);
            }

            &:active {
                transform: translateY(0);
            }
        }

        // 合并后的.stats-card-group（仅保留一个）
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
                transition: all 0.3s ease; // 新增过渡动画，让hover更丝滑

                // 合并hover/active效果
                &:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 6px 16px rgba(106, 133, 182, 0.4);
                }

                &:active {
                    transform: translateY(0);
                }

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

    .header-section .stats-card-group {
        flex-wrap: wrap;
        justify-content: flex-start;

        .stats-card {
            min-width: calc(50% - 10px);
            margin-bottom: 15px;
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
    .header-section .stats-card-group .stats-card {
        min-width: 100%;
    }
}

.echarts-pie1 {
    width: 100%;
    height: 100%;
}
</style>
