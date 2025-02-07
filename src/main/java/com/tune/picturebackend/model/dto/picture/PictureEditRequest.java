package com.tune.picturebackend.model.dto.picture;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

/**
 * 编辑请求（普通用户）
 * @author Tune
 **/
@Data
public class PictureEditRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Positive(message = "id必须为正数")
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
