-- 系统管理员是全局角色，不占用社区管理员名额，也不能被罢免。
USE linzu;

UPDATE t_community_member cm
JOIN t_user u ON cm.user_id = u.id
SET cm.role = 'MEMBER'
WHERE u.role = 'PLATFORM_ADMIN'
  AND cm.role = 'ADMIN';
