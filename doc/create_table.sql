-- 创建库
create database if not exists picture;

-- 切换库
use picture;

-- 用户表
-- 如果user表存在则删除，方便重新创建（谨慎使用，会删除已有数据）
DROP TABLE IF EXISTS user;

-- 创建用户表以及初始的字段和约束等
CREATE TABLE user
(
    id           		bigint auto_increment comment 'id' primary key,
    userAccount  		varchar(128)                    					NOT NULL comment '账号',
    userPassword 		varchar(128)                    					NOT NULL comment '密码',
    userName     		varchar(128)                    					NULL     comment '用户昵称',
    userAvatar   		varchar(512)                   						NULL     comment '用户头像',
    userProfile  		varchar(256)                    				 	NULL     comment '用户简介',
    userRole     		varchar(256)  DEFAULT 'user'    				 	NOT NULL comment '用户角色：user/admin/root',
    editTime     		datetime      DEFAULT CURRENT_TIMESTAMP 	        NOT NULL comment '编辑时间',
    createTime   		datetime      DEFAULT CURRENT_TIMESTAMP 	        NOT NULL comment '创建时间',
    updateTime   		datetime      DEFAULT CURRENT_TIMESTAMP 	        NOT NULL comment '更新时间',
    isDelete     		tinyint       DEFAULT 0         					NOT NULL comment '是否删除',
    gender       		tinyint       DEFAULT 0             			    comment '性别 0-待定 1-男 2-女',
    phone        		varchar(128)  NULL             						comment '电话',
    email        		varchar(128)  NULL            					 	comment '邮箱',
    userStatus   		tinyint       DEFAULT 0         					NOT NULL comment '状态 0-活跃 1-锁定',
    del_unique_key 	bigint       	  DEFAULT 0         					NOT NULL comment '用于联合唯一约束的字段，默认值为0，与主键id类型和大小一致',
    UNIQUE KEY uk_userAccount_del_unique_key (userAccount, del_unique_key),
    INDEX idx_userName (userName),
    INDEX idx_userRole (userRole)
) comment '用户' collate = utf8mb4_unicode_ci;
