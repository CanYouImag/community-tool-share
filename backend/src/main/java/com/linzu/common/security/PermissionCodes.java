package com.linzu.common.security;

/**
 * 权限码清单（钟姝伶维护），与 {@link RequirePermission} / {@link PermissionInterceptor} 对接。
 *
 * <p>前缀约定（由 {@link PermissionInterceptor} 校验）：</p>
 * <ul>
 *   <li>{@code community:*} → 社区管理员或系统管理员</li>
 *   <li>{@code platform:*} → 仅系统管理员</li>
 *   <li>无前缀 → 任意已登录用户</li>
 * </ul>
 *
 * <p>权限矩阵依据《需求分析文档》附录 A；具体到用例参见下方注释。</p>
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    // ===== 社区治理（社区管理员 + 系统管理员）=====

    /** UC-B3 审核加入申请 */
    public static final String COMMUNITY_JOIN_AUDIT = "community:join-audit";

    /** UC-B5 发布/管理社区公告 */
    public static final String COMMUNITY_ANNOUNCE = "community:announce";

    /** UC-B6 成员管理 / 踢出社区成员 */
    public static final String COMMUNITY_MEMBER_MANAGE = "community:member-manage";

    /** UC-G7 本社区数据统计 */
    public static final String COMMUNITY_STATS = "community:stats";

    // ===== 纠纷治理 =====

    /** UC-F2/F3 纠纷调解（留言 / 标记调解成功） */
    public static final String COMMUNITY_MEDIATE = "community:mediate";

    /** UC-F4 上报仲裁 */
    public static final String COMMUNITY_REPORT_ESCALATE = "community:report-escalate";

    /** UC-F8 处理举报（社区侧） */
    public static final String COMMUNITY_REPORT_HANDLE = "community:report-handle";

    // ===== 平台治理（仅系统管理员）=====

    /** UC-G1 社区 CRUD（创建/编辑/停用社区） */
    public static final String PLATFORM_COMMUNITY = "platform:community";

    /** UC-G2 任命/罢免社区管理员 */
    public static final String PLATFORM_ADMIN_APPOINT = "platform:admin-appoint";

    /** UC-G3 封禁/解封账号 */
    public static final String PLATFORM_BAN = "platform:ban";

    /** UC-G4 平台参数配置 */
    public static final String PLATFORM_CONFIG = "platform:config";

    /** UC-G5 全平台数据统计 */
    public static final String PLATFORM_STATS = "platform:stats";

    /** UC-G6 查询操作日志 */
    public static final String PLATFORM_LOG = "platform:log";

    /** UC-F5/F6 仲裁裁决 / 生成裁决书并划转押金 */
    public static final String PLATFORM_ARBITRATE = "platform:arbitrate";

    /** UC-A4 审核认证申请 */
    public static final String PLATFORM_CERT = "platform:cert";

    /** UC-F8 处理举报（平台侧） */
    public static final String PLATFORM_REPORT_HANDLE = "platform:report-handle";
}
