package com.samolsen.resteasy.cache.redis.example;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.cache.Cache;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Date;


@Path("/responses")
@Api("Responses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResponseResource {

    private static int COUNT = 0;

    @GET
    @ApiOperation(value = "Get response", response = ResponseModel.class)
    @Cache(maxAge = 30)
    public ResponseModel getCount()
    {
        return new ResponseModel(++COUNT, new Date());
    }
}
