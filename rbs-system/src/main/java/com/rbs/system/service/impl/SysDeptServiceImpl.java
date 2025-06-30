package com.rbs.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rbs.common.annotation.DataScope;
import com.rbs.common.constant.UserConstants;
import com.rbs.common.core.domain.TreeSelect;
import com.rbs.common.core.domain.entity.SysDept;
import com.rbs.common.core.domain.entity.SysRole;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.common.exception.ServiceException;
import com.rbs.common.utils.SecurityUtils;
import com.rbs.common.utils.StringUtils;
import com.rbs.common.utils.spring.SpringUtils;
import com.rbs.system.mapper.SysDeptMapper;
import com.rbs.system.mapper.SysRoleMapper;
import com.rbs.system.service.ISysDeptService;

/**
 * 部门管理 服务实现
 *
 * @author ruoyi
 */
@Service
public class SysDeptServiceImpl implements ISysDeptService
{
    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<SysDept> selectDeptList(SysDept dept)
    {
        return deptMapper.selectDeptList(dept);
    }

    @Override
    public List<SysDept> selectDeptSimpleList() {
        return deptMapper.selectDeptSimpleList();
    }

    /**
     * 查询部门树结构信息（扁平结构，返回列表）
     *
     * @param dept 部门信息
     * @return 部门列表
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept)
    {
        List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        // 不再构建树结构，直接转换为TreeSelect
        return depts.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 构建前端所需要树结构（扁平结构，返回原列表）
     *
     * @param depts 部门列表
     * @return 部门列表（不再构建树）
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts)
    {
        // 扁平结构无需构建树，直接返回
        return depts;
    }

    /**
     * 构建前端所需要下拉树结构（扁平结构）
     *
     * @param depts 部门列表
     * @return 下拉列表（不再构建树）
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts)
    {
        // 不再构建树结构，直接转换为TreeSelect
        return depts.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId)
    {
        SysRole role = roleMapper.selectRoleById(roleId);
        return deptMapper.selectDeptListByRoleId(roleId, role.isDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public SysDept selectDeptById(Long deptId)
    {
        return deptMapper.selectDeptById(deptId);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数（扁平结构始终返回0）
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId)
    {
        return 0; // 扁平结构无父子关系
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果（扁平结构始终返回false）
     */
    @Override
    public boolean hasChildByDeptId(Long deptId)
    {
        return false; // 扁平结构无父子关系
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        int result = deptMapper.checkDeptExistUser(deptId);
        return result > 0;
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptMapper.checkDeptNameUnique(dept.getDeptName());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId)
    {
        if (!SysUser.isAdmin(SecurityUtils.getUserId()) && StringUtils.isNotNull(deptId))
        {
            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (StringUtils.isEmpty(depts))
            {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(SysDept dept)
    {
        // 扁平结构无需校验父部门状态
        return deptMapper.insertDept(dept);
    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int updateDept(SysDept dept)
    {
        // 扁平结构无需更新子部门层级关系
        return deptMapper.updateDept(dept);
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public int deleteDeptById(Long deptId)
    {
        return deptMapper.deleteDeptById(deptId);
    }
}