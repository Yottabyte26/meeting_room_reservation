package com.rbs.web.controller.system;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.rbs.common.annotation.Log;
import com.rbs.common.core.controller.BaseController;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.core.domain.entity.SysDept;
import com.rbs.common.core.domain.entity.SysRole;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.common.core.page.TableDataInfo;
import com.rbs.common.enums.BusinessType;
import com.rbs.common.utils.SecurityUtils;
import com.rbs.common.utils.StringUtils;
import com.rbs.common.utils.poi.ExcelUtil;
import com.rbs.system.service.ISysDeptService;
import com.rbs.system.service.ISysPostService;
import com.rbs.system.service.ISysRoleService;
import com.rbs.system.service.ISysUserService;
import com.rbs.system.mapper.SysUserMapper;
import com.rbs.system.service.ISysEmailCodeService;

import static com.rbs.framework.datasource.DynamicDataSourceContextHolder.log;

/**
 * 用户信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    // 注入邮箱验证码服务
    @Autowired
    private ISysEmailCodeService emailCodeService;

    // 注入用户Mapper
    @Autowired
    private SysUserMapper userMapper;

    /**
     * 获取用户列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        if (ArrayUtils.contains(userIds, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }

    @PostMapping("/verifyCode")
    public AjaxResult verifyCode(@RequestParam String email,
                                 @RequestParam String code,
                                 @RequestParam(required = false, defaultValue = "2") Integer type) {
        boolean isValid = emailCodeService.validateCode(email, code, type);
        if (!isValid) {
            return AjaxResult.error("验证码错误或已过期");
        }
        return AjaxResult.success();
    }
    /**
     * 通过邮箱验证码重置密码（找回密码功能）
     */
    @PostMapping("/resetPasswordByEmail")
    public AjaxResult resetPasswordByEmail(@RequestBody Map<String, String> params) {
        // 从请求参数中获取邮箱、验证码、新密码（不依赖登录用户）
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");

        // 验证参数非空
        if (StringUtils.isAnyBlank(email, code, newPassword)) {
            return AjaxResult.error("参数不能为空");
        }

        // 1. 验证验证码是否正确
//        boolean isValid = emailCodeService.validateCode(email, code, 2); // 2表示找回密码类型
//        if (!isValid) {
//            return AjaxResult.error("验证码错误或已过期");
//        }

        // 2. 通过邮箱查询用户
        SysUser user = userService.selectUserByEmail(email);
        if (user == null) {
            return AjaxResult.error("该邮箱未注册");
        }

        // 3. 更新密码（注意：需要加密存储）
        user.setPassword(SecurityUtils.encryptPassword(newPassword)); // 加密新密码
        user.setUpdateBy("system"); // 重置密码的操作人可设为system
        userService.updateUser(user);

        return AjaxResult.success("密码重置成功");
    }
    /**
     * 发送密码重置通知邮件
     */
    @PostMapping("/sendPasswordResetNotification")
    public AjaxResult sendPasswordResetNotification(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String newPassword = params.get("newPassword");
        String userName = params.get("userName");

        log.info("开始处理发送密码重置通知邮件，邮箱：{}，用户名：{}", email, userName);

        // 1. 验证参数
        if (StringUtils.isBlank(email)) {
            return error("邮箱不能为空");
        }
        if (StringUtils.isBlank(newPassword)) {
            return error("新密码不能为空");
        }
        if (StringUtils.isBlank(userName)) {
            return error("用户名不能为空");
        }

        // 2. 验证邮箱是否已注册
        SysUser user = userService.selectUserByEmail(email);
        if (user == null) {
            return error("该邮箱未注册");
        }

        // 3. 发送密码重置通知邮件
        try {
            emailCodeService.sendPasswordResetNotificationSync(email, userName, newPassword);
            return success("密码重置通知邮件发送成功");
        } catch (Exception e) {
            log.error("发送密码重置通知邮件失败", e);
            return error("邮件发送失败，请重试");
        }
    }

    /**
     * 发送重置密码的邮箱验证码
     */
    @PostMapping("/sendResetPwdCode")
    public AjaxResult sendResetPwdCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        log.info("开始处理发送验证码请求，邮箱：{}", email);
        // 1. 验证邮箱参数
        if (StringUtils.isBlank(email)) {
            return error("邮箱不能为空");
        }

        // 2. 验证邮箱是否已注册
        SysUser user = userService.selectUserByEmail(email);
        if (user == null) {
            return error("该邮箱未注册");
        }

        // 3. 调用现有服务方法发送验证码（类型：2=找回密码）
        return emailCodeService.sendCode(email, 2);
    }
}
