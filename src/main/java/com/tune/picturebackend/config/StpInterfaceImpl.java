package com.tune.picturebackend.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.tune.picturebackend.constant.SpaceUserPermissionConstant;
import com.tune.picturebackend.constant.UserConstant;
import com.tune.picturebackend.enums.SpaceRoleEnum;
import com.tune.picturebackend.enums.SpaceTypeEnum;
import com.tune.picturebackend.exception.BusinessException;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.manager.auth.SpaceUserAuthContext;
import com.tune.picturebackend.manager.auth.SpaceUserAuthManager;
import com.tune.picturebackend.model.entity.Picture;
import com.tune.picturebackend.model.entity.Space;
import com.tune.picturebackend.model.entity.SpaceUser;
import com.tune.picturebackend.model.entity.User;
import com.tune.picturebackend.service.PictureService;
import com.tune.picturebackend.service.SpaceService;
import com.tune.picturebackend.service.SpaceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Slf4j
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    // 默认是 /api
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        // if (!StpKit.SPACE_TYPE.equals(loginType)) {
        //     log.info("loginType 不是 space，直接返回");
        //     return new ArrayList<>();
        // }
        // 获取管理员角色权限集合（跟“空间”权限相关）
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 1. 如果所有字段都为空，表示查询公共图库，都可以访问和进行操作
        if (isAllFieldsNull(authContext)) {
            log.info("上下文对象所有字段都为空，，表示查询公共图库，都可以访问和进行操作");
            return ADMIN_PERMISSIONS;
        }
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            log.info("从上下文中直接获取到 SpaceUser 对象， 返回对应空间角色对应权限");
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 2. 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        Long userId = loginUser.getId();
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                log.info("当前登录用户不是该团队空间管理员，无对应空间角色对应权限");
                return new ArrayList<>();
            }
            log.info("上下文对象中无 SpaceUser，通过spaceUserId 获取到 SpaceUser 对象， 返回对应空间角色对应权限");
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 3. 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                log.info("上下文对象没有 spaceUser spaceUserId spaceId pictureId，默认通过权限校验");
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                boolean isAdmin = StpUtil.hasRoleOr(UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE);
                if (picture.getUserId().equals(userId) || isAdmin) {
                    log.info("上下文对象 没有spaceUser spaceUserId spaceId，通过 pictureId 获取 spaceId ，spaceId 仍然为 null ，仅本人或管理员可操作公共图库");
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    log.info("不是自己的图片，仅可查看");
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            boolean isAdmin = StpUtil.hasRoleOr(UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE);
            if (space.getUserId().equals(userId) || isAdmin) {
                log.info("上下文对象 没有spaceUser spaceUserId，通过 spaceId 获取 space 信息，空间为私有空间，操作人员为本人或管理员，返回权限");
                return ADMIN_PERMISSIONS;
            } else {
                log.info("上下文对象 没有spaceUser spaceUserId，通过 spaceId 获取 space 信息，空间为私有空间，操作人员不是本人或管理员，返回无权限");
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                log.info("上下文对象 没有spaceUser spaceUserId，通过 spaceId 获取 space 信息，空间为团队空间，但查询对应 spaceUser 为空，返回无权限");
                return new ArrayList<>();
            }
            log.info("上下文对象 没有spaceUser spaceUserId，通过 spaceId 获取 space 信息，空间为团队空间，返回对应权限");
            log.info("permissions = " + spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        List<String> list = new ArrayList<>();
        list.add(loginUser.getUserRole());
        System.out.println("loginUser.getUserRole() = " + loginUser.getUserRole());
        return list;
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        // 获取当前 HTTP 请求
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // 获取请求的 Content-Type 头信息
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        // 根据 Content-Type 解析请求参数
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            // 将请求 URI 中的 contextPath 部分替换为空字符串，得到相对路径
            String partUri = requestUri.replace(contextPath + "/", "");
            // 用 StrUtil 工具类从相对路径中提取模块名称，即第一个 / 之前的部分
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    /**
     * 判断对象的所有字段是否为空
     *
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }
}