package thirtytwo.degrees.resources;

import org.springframework.stereotype.Component;
import thirtytwo.degrees.core.Hello;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: gibbsb
 * Date: 9/21/12
 * Time: 5:11 PM
 */
@Component
@Path("/hello")
public class HelloResource {

    @Inject
    Sayer sayer;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Hello hello() {
        return new Hello(sayer.hello(), "Resource");
    }
}
