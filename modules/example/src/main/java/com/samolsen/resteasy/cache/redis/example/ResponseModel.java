package com.samolsen.resteasy.cache.redis.example;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@ApiModel
public class ResponseModel {

    @NotNull
    private final Date _date = new Date();

    @ApiModelProperty(value = "date this model was created")
    @NotNull
    public Date getDate()
    {
        return _date;
    }
}


