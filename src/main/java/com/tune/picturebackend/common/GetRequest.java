package com.tune.picturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}