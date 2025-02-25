package com.tune.picturebackend.model.dto.spaceuser;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     * ID
     */
    private Long  id;

    /**
     * 空间 ID
     */
    @NotNull(message = "空间 ID 不能为空")
    private Long spaceId;

    /**
     * 用户 ID
     */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
