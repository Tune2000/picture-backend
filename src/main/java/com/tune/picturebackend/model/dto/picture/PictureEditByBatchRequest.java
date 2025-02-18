package com.tune.picturebackend.model.dto.picture;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {

    /**
     * 图片 id 列表
     */
    @NotEmpty(message = "参数错误，图片 id 列表不能为空")
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    @NotNull(message = "参数错误，空间 id 不能为空")
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;


    private static final long serialVersionUID = 1L;
}
