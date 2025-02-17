package com.tune.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.model.dto.space.SpaceAddRequest;
import com.tune.picturebackend.model.dto.space.SpaceEditRequest;
import com.tune.picturebackend.model.dto.space.SpaceQueryRequest;
import com.tune.picturebackend.model.dto.space.SpaceUpdateRequest;
import com.tune.picturebackend.model.entity.Space;
import com.tune.picturebackend.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Tune
* @description 针对表【space(空间)】的数据库操作Service
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间数据
     * @param space
     * @param add 区分创建数据时校验还是编辑时校验
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间等级填充限额信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 添加空间
     * @param spaceAddRequest
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest);

    /**
     * 更新空间
     * @param spaceUpdateRequest
     * @return
     */
    void updateSpace(SpaceUpdateRequest spaceUpdateRequest);

    /**
     * 编辑（用户登录时）
     *
     * @param spaceEditRequest
     * @return
     */
    void editSpace(SpaceEditRequest spaceEditRequest);

    /**
     * 删除空间
     * @param deleteRequest
     * @return
     */
    void deleteSpace(DeleteRequest deleteRequest);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space);
    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

}
