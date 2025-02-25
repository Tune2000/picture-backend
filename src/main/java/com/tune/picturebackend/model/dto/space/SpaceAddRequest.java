package com.tune.picturebackend.model.dto.space;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Data
public class SpaceAddRequest implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @Range(min = 0, max = 2, message = "空间级别必须在指定范围之间")
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    @Range(min = 0, max = 1, message = "空间类型必须在指定范围之间")
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}
