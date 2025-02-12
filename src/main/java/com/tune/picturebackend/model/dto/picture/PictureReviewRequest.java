package com.tune.picturebackend.model.dto.picture;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * 待审核图片 id
     */
    @NotNull(message = "待审核图片id不能为空")
    private Long id;  
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝  
     */
    @Min(value = 0, message = "审核状态必须是指定内容")
    @Max(value = 2, message = "审核状态必须是指定内容")
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  
  
  
    private static final long serialVersionUID = 1L;  
}
