package com.tune.picturebackend.model.dto.space;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

@Data
public class SpaceUpdateRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Positive(message = "id必须为正数")
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @Min(value = 0, message = "空间级别必须在指定范围之间")
    @Max(value = 2, message = "空间级别必须在指定范围之间")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    private static final long serialVersionUID = 1L;
}
