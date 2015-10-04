package com.samolsen.resteasy.cache.redis.example;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


@Path("/responses")
@Api("Responses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResponseResource {

    @GET
    @Path("/cached")
    @ApiOperation(value = "Get a response which may be served from the server-side cahce")
    @Cache(maxAge = 30)
    public ResponseModel getCount(
            @ApiParam(value = "Full URLs are cached. Changing a query param will result in a cache miss", required = false)
            @QueryParam("someParam") String someParam )
    {
        return new ResponseModel();
    }

    @GET
    @Path("/noCache")
    @ApiOperation(value = "Get response which should never come from the server-side cache")
    public ResponseModel getCountNoCache()
    {
        return new ResponseModel();
    }
}
