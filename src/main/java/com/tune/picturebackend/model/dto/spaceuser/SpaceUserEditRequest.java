package com.tune.picturebackend.model.dto.spaceuser;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Positive(message = "id必须为正数")
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
