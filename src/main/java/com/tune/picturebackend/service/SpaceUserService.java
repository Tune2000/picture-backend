package com.tune.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.tune.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.tune.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.tune.picturebackend.model.entity.SpaceUser;
import com.tune.picturebackend.model.vo.spaceuser.SpaceUserVO;

import java.util.List;

/**
* @author Tune
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 删除空间成员
     * @param deleteRequest
     * @return
     */
    void deleteSpaceUser(DeleteRequest deleteRequest);

    /**
     * 修改空间成员
     * @param spaceUserEditRequest
     */
    void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add       是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

}
