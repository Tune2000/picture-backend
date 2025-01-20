package com.tune.picturebackend.common;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

@Data
public class GetRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Positive(message = "id必须为正数")
    private Long id;

    private static final long serialVersionUID = 1L;
}