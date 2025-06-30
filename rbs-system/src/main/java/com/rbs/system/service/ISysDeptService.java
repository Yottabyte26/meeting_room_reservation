package com.rbs.system.service;

import java.util.List;
import com.rbs.common.core.domain.TreeSelect;
import com.rbs.common.core.domain.entity.SysDept;

/**
 * 部门管理 服务层
 *
 * @author ruoyi
 */
public interface ISysDeptService
{
    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    public List<SysDept> selectDeptList(SysDept dept);

    /**
     * 查询部门简单列表（用于下拉选择）
     */
    List<SysDept> selectDeptSimpleList();

    /**
     * 查询部门列表（不再支持树形结构，改为扁平列表）
     *
     * @param dept 部门信息
     * @return 部门列表
     */
    public List<TreeSelect> selectDeptTreeList(SysDept dept);

    /**
     * 构建前端所需要列表结构（原树形方法改为列表）
     *
     * @param depts 部门列表
     * @return 列表结构
     */
    public List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 构建前端所需要下拉列表结构（原树形方法改为列表）
     *
     * @param depts 部门列表
     * @return 下拉列表结构
     */
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts);

    /**
     * 根据角色ID查询部门列表
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    public List<Long> selectDeptListByRoleId(Long roleId);

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    public SysDept selectDeptById(Long deptId);

    /**
     * 根据ID查询所有子部门（扁平结构始终返回0）
     *
     * @param deptId 部门ID
     * @return 子部门数(0)
     */
    public int selectNormalChildrenDeptById(Long deptId);

    /**
     * 是否存在部门子节点（扁平结构始终返回false）
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public boolean hasChildByDeptId(Long deptId);

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    public boolean checkDeptExistUser(Long deptId);

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    public boolean checkDeptNameUnique(SysDept dept);

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    public void checkDeptDataScope(Long deptId);

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int insertDept(SysDept dept);

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int updateDept(SysDept dept);

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteDeptById(Long deptId);
}