import useUserStore from '@/store/modules/user'

/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(value) {
  if (value && value instanceof Array && value.length > 0) {
    const permissions = useUserStore().permissions
    const permissionDatas = value
    const all_permission = "*:*:*";

   function hasPermission(permissions, route) {
      const userStore = useUserStore()
      const roles = userStore.roles
      const perms = userStore.permissions
      
      // 检查排除角色
      if (route.meta?.excludedRoles) {
        if (roles.some(role => route.meta.excludedRoles.includes(role))) {
          return false
        }
      }
      
      // 检查必需角色
      if (route.meta?.requiredRoles) {
        if (!roles.some(role => route.meta.requiredRoles.includes(role))) {
          return false
        }
      }

      // 原有权限检查逻辑
      if (permissions.includes('admin') && !route.meta?.forbiddenRoles?.includes('admin')) {
        return true
      }

      if (route.permissions) {
        return permissions.some(perm => route.permissions.includes(perm))
      }

      if (route.roles) {
        return route.roles.some(role => permissions.includes(role))
      }

      return true
    }

    if (!hasPermission) {
      return false
    }
    return true
  } else {
    console.error(`need roles! Like checkPermi="['system:user:add','system:user:edit']"`)
    return false
  }
}

/**
 * 角色权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkRole(value, excludeRoles = []) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = useUserStore().roles
    const permissionRoles = value
    const super_admin = "admin"

    // 如果用户拥有被排除的角色，则直接返回false
    if (excludeRoles.length > 0 && roles.some(role => excludeRoles.includes(role))) {
      return false
    }

    const hasRole = roles.some(role => {
      return super_admin === role || permissionRoles.includes(role)
    })

    if (!hasRole) {
      return false
    }
    return true
  } else {
    console.error(`need roles! Like checkRole="['admin','editor']"`)
    return false
  }
}