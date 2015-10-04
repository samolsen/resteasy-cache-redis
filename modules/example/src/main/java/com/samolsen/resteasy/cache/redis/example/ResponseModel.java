package com.samolsen.resteasy.cache.redis.example;


import io.swagger.annotations.ApiModel;

import java.util.Date;

@ApiModel
public class ResponseModel {

    private final int _id;
    private final Date _date;

    public ResponseModel( int id,
                          Date date )
    {
        _id = id;
        _date = date;
    }

    public int getId()
    {
        return _id;
    }

    public Date getDate()
    {
        return _date;
    }
}


