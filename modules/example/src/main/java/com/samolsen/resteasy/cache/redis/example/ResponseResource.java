package com.samolsen.resteasy.cache.redis.example;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;


@Path("/responses")
@Api("Responses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResponseResource {

    private static int COUNT = 0;

    @GET
    @ApiOperation(value = "Get cache-able response", response = ResponseModel.class)
    @Cache(maxAge = 30)
    @NotNull
    public ResponseModel getCount( @QueryParam("someParam") @Nullable String someParam )
    {
        return new ResponseModel(++COUNT, new Date());
    }

    @GET
    @Path("/noCache")
    @ApiOperation(value = "Get response which should never come from the cache", response = ResponseModel.class)
    @NotNull
    public ResponseModel getCountNoCache( @QueryParam("someParam") @Nullable String someParam )
    {
        return getCount(someParam);
    }
}
