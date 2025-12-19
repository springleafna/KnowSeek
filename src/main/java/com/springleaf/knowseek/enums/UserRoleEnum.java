package com.springleaf.knowseek.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    /**
     * 普通用户
     */
    USER("USER", "普通用户"),

    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员");

    private final String value;
    private final String description;

    /**
     * 根据字符串值获取对应的枚举
     * @param value 数据库存储的值
     * @return 对应的枚举，如果找不到返回null
     */
    public static UserRoleEnum fromValue(String value) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 检查给定的字符串是否是有效的角色值
     * @param value 要检查的值
     * @return 如果是有效角色值返回true，否则返回false
     */
    public static boolean isValid(String value) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
