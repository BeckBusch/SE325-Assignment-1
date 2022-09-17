package se325.flights.service;

import se325.flights.domain.User;
import se325.flights.dto.BookingRequestDTO;
import se325.flights.util.SecurityUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A JAX-RS Resource class intended to contain methods with making and cancelling flight bookings, in
 * addition to retrieving information about existing flight bookings.
 */

@Path("/bookings")
public class BookingsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response makeBooking(@CookieParam("authToken") Cookie auth, BookingRequestDTO bookRequest){
        EntityManager em = PersistenceManager.instance().createEntityManager();

        User bookingUser = SecurityUtils.getUserWithAuthToken(em, auth);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookings(){

    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSingleBooking(@PathParam("id") long id){

    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteBooking(@PathParam("id") long id){

    }
}
