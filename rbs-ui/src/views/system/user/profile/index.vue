<template>
   <div class="app-container">
      <el-row :gutter="20">
         <el-col :span="24" :xs="24">
            <el-card class="box-card" :transition="fadeIn">
               <!-- <template v-slot:header>
                  <div class="clearfix">
                     <span>个人信息</span>
                  </div>
               </template> -->
               <div class="user-info-layout">
                  <div class="user-avatar-container">
                     <div class="text-center">
                        <userAvatar />
                     </div>
                  </div>
                  <div class="user-details">
                     <div class="user-name-dept">
                        <span class="user-name">{{ state.user.userName }}</span>
                        <span v-if="state.user.dept" class="user-dept">
                           {{ state.user.dept.deptName }}
                        </span>
                     </div>
                     <div class="user-other-info">
                        <div class="user-info-item">
                           <span>手机号码:</span>
                           <span>{{ state.user.phonenumber }}</span>
                        </div>
                        <div class="user-info-item">
                           <span>用户邮箱:</span>
                           <span>{{ state.user.email }}</span>
                        </div>
                        <div class="user-info-item">
                           <span>所属角色:</span>
                           <span>{{ state.roleGroup }}</span>
                        </div>
                        <div class="user-info-item">
                           <span>创建日期:</span>
                           <span>{{ state.user.createTime }}</span>
                        </div>
                     </div>
                  </div>
               </div>
            </el-card>
         </el-col>
      </el-row>
      <!-- 添加间距类名 -->
      <el-row :gutter="20">
         <el-col :span="12" :xs="24">
            <el-card :transition="fadeIn">
               <template v-slot:header>
                  <div class="clearfix">
                     <span>设置</span>
                  </div>
               </template>
               <el-tabs v-model="activeTab">
                  <el-tab-pane label="基本资料" name="userinfo">
                  </el-tab-pane>
                  <el-tab-pane label="修改密码" name="resetPwd">
                  </el-tab-pane>
               </el-tabs>
            </el-card>
         </el-col>
         <el-col :span="12" :xs="24">
            <el-card :transition="fadeIn">
               <template v-slot:header>
                  <div class="clearfix">
                     <span>{{ getHeaderTitle() }}</span>
                  </div>
               </template>
               <div v-if="activeTab === 'userinfo'">
                  <userInfo :user="state.user" />
               </div>
               <div v-if="activeTab === 'resetPwd'">
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
// import func from "../../../../../vue-temp/vue-editor-bridge";
const fadeIn = {
   enterFromClass: 'opacity-0',
   enterToClass: 'opacity-100',
   enterActiveClass: 'transition-opacity duration-300',
   leaveFromClass: 'opacity-100',
   leaveToClass: 'opacity-0',
   leaveActiveClass: 'transition-opacity duration-300'
}
const activeTab = ref("userinfo");
const state = reactive({
   user: {},
   roleGroup: {},
   postGroup: {}
});
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
}

.user-avatar-container {
   margin-right: 10px;
}

.user-details {
   display: flex;
   flex-direction: column;
}

.user-name {
   font-size: 20px;
   font-weight: bold;
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
   padding: 5px 0;
}
</style>