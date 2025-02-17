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

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url' AFTER url;

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId  bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);