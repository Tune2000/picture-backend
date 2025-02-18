package com.tune.picturebackend.model.dto.picture;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    @NotBlank(message = "图片主色调不能为空")
    private String picColor;

    /**
     * 空间 id
     */
    @NotNull(message = "空间不能为空")
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
