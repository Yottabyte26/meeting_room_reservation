<template>
   <div class="app-container main">
      <el-row :gutter="20">
         <el-col :span="22" :xs="24">
            <el-card class="box-card" :transition="fadeIn">
               <div class="user-info-layout">
                  <div class="user-avatar-container">
                     <div class="text-center">
                        <userAvatar />
                     </div>
                  </div>
                  <div class="user-details move-right">
                     <div class="user-name-dept">
                        <span class="user-name">{{ state.user.userName }}</span>
                     </div>

                     <div class="user-other-info">
                        <div class="user-info-item">
                           <span>所属部门：{{ state.user?.dept?.deptName ?? '未分配部门' }}</span>
                        </div>
                        <div class="user-info-item">
                           <span>用户邮箱：{{ state.user.email }}</span>
                        </div>

                        <div class="user-info-item">
                           <span>手机号码：{{ state.user?.phonenumber ?? '未填' }}</span>
                        </div>
                        <div class="user-info-item">
                           <span>创建日期：{{ state.user.createTime }}</span>
                        </div>
                     </div>
                  </div>
               </div>
            </el-card>
         </el-col>
      </el-row>
      <!-- 添加间距类名 -->
      <el-row :gutter="20" class="footer">
         <el-col :span="4" :xs="24">
            <el-card class="heigh-footer">
               <template v-slot:header>
                  <div class="clearfix">
                     <span>设置</span>
                  </div>
               </template>
               <el-menu :default-active="activeTab" class="vertical-menu" @select="handleMenuSelect">
                  <el-menu-item index="userinfo">基本资料</el-menu-item>
                  <el-menu-item index="resetPwd">修改密码</el-menu-item>
               </el-menu>
            </el-card>
         </el-col>
         <el-col :span="18" :xs="24">
            <el-card class="heigh-footer">
               <template v-slot:header>
                  <div class="clearfix">
                     <span>{{ getHeaderTitle() }}</span>
                  </div>
               </template>
               <div v-if="activeTab === 'userinfo'" class="active">
                  <userInfo :user="state.user" />
               </div>
               <div v-if="activeTab === 'resetPwd'" class="active">
                  <resetPwd />
               </div>
            </el-card>
         </el-col>
      </el-row>
   </div>
</template>

<script setup name="Profile">
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";
const activeTab = ref("userinfo");
const state = reactive({
   user: {},
   roleGroup: {},
   postGroup: {}
});
const handleMenuSelect = (index) => {
   activeTab.value = index;
};
function getHeaderTitle() {
   return this.activeTab === 'userinfo' ? '基本资料' : '修改密码';
}

function getUser() {
   getUserProfile().then(response => {
      state.user = response.data;
      state.roleGroup = response.roleGroup;
      state.postGroup = response.postGroup;
   });
};

getUser();
</script>
<style scoped>
.main {
   margin: 10px 150px;
}

.el-row {
   display: flex;
}

.icon-spacing {
   margin-right: 10px;
}

.mt-4 {
   margin-top: 1rem;
}

.user-info-layout {
   display: flex;
   align-items: center;
   margin-left: 10px;
}

.user-avatar-container {
   margin-right: 10px;
}

.user-details {
   display: flex;
   flex-direction: column;
}

.user-name {
   font-size: 23px;
   font-weight: bold;
}

.user-name-dept {
   margin-bottom: 6px;
   margin-top: 10px;
}

.user-dept {
   font-size: 16px;
   color: #999;
}

.user-other-info {
   font-size: 14px;
   color: #666;
}

.user-info-item {
   display: flex;
   justify-content: space-between;
   border-bottom: none;
   padding: 7px 0;
}

.move-right {
   margin-left: 30px;
}

.footer {
   margin-top: 25px;
}

.vertical-menu .el-menu-item {
   text-align: center;
}

.vertical-menu .el-menu-item.is-active::before {
   content: '';
   position: absolute;
   right: -1.5px;
   top: 0;
   height: 100%;
   width: 2px;
   background-color: #409EFF;
}

.heigh-footer {
   height: 370px;
}

.active {
   padding-top: 15px;
}
</style>