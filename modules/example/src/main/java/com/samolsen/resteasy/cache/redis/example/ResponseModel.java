package com.samolsen.resteasy.cache.redis.example;


import io.swagger.annotations.ApiModel;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@ApiModel
public class ResponseModel {

    private final int _id;
    @NotNull private final Date _date;

    public ResponseModel( int id,
                          @NotNull Date date )
    {
        _id = id;
        _date = date;
    }

    public int getId()
    {
        return _id;
    }

    @NotNull
    public Date getDate()
    {
        return _date;
    }
}


