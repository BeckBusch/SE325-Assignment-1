package se325.flights.service;

import se325.flights.domain.BookingException;
import se325.flights.domain.Flight;
import se325.flights.domain.FlightBooking;
import se325.flights.domain.User;
import se325.flights.domain.mappers.BookingMapper;
import se325.flights.dto.BookingRequestDTO;
import se325.flights.dto.FlightBookingDTO;
import se325.flights.util.SecurityUtils;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * A JAX-RS Resource class intended to contain methods with making and cancelling flight bookings, in
 * addition to retrieving information about existing flight bookings.
 */

@Path("/bookings")
public class BookingsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response makeBooking(@CookieParam("authToken") Cookie auth, BookingRequestDTO bookRequest) {
        EntityManager em = PersistenceManager.instance().createEntityManager();

        User bookingUser = SecurityUtils.getUserWithAuthToken(em, auth);

        try {
            em.getTransaction().begin();

            TypedQuery<Flight> flightsQuery = em.createQuery(
                            "select f from Flight f where f.id = :idLink", Flight.class)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint("javax.persistence.lock.timeout", 5000)
                    .setParameter("idLink", bookRequest.getFlightId());


            Flight bookedFlight = flightsQuery.getSingleResult();

            FlightBooking bookingObj = bookedFlight.makeBooking(bookingUser, bookRequest.getRequestedSeats());

            em.persist(bookingObj);
            em.getTransaction().commit();

            return Response.created(URI.create("/bookings/" + bookingObj.getId())).build();

        } catch (NoResultException e) {
            em.getTransaction().rollback();
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (BookingException e) {
            em.getTransaction().rollback();
            return Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            em.getTransaction().rollback();
            return Response.status(Response.Status.CONFLICT).build();
        }
        finally {
            em.close();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookings(@CookieParam("authToken") Cookie auth) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User bookingUser = SecurityUtils.getUserWithAuthToken(em, auth);

        try {
            TypedQuery<FlightBooking> flightsQuery = em.createQuery(
                            "select b from FlightBooking b where b.user = :userLink", FlightBooking.class)
                    .setParameter("userLink", bookingUser);

            List<FlightBookingDTO> bookingList = new ArrayList<>();

            for (FlightBooking i : flightsQuery.getResultList()) {
                bookingList.add(BookingMapper.toDTO(i));
            }

            return Response.ok(bookingList).build();

        } finally {
            em.close();
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSingleBooking(@PathParam("id") long id, @CookieParam("authToken") Cookie auth) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User bookingUser = SecurityUtils.getUserWithAuthToken(em, auth);

        try {
            TypedQuery<FlightBooking> flightsQuery = em.createQuery(
                            "select b from FlightBooking b where (b.id = :idLink)"
                            + "and (b.user = :userLink)"
                            , FlightBooking.class)
                    .setParameter("idLink", id)
                    .setParameter("userLink", bookingUser);

            FlightBooking bookedFlight = flightsQuery.getSingleResult();

            FlightBookingDTO responseDTO = BookingMapper.toDTO(bookedFlight);

            return Response.ok(responseDTO).build();

        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteBooking(@PathParam("id") long id, @CookieParam("authToken") Cookie auth) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User bookingUser = SecurityUtils.getUserWithAuthToken(em, auth);

        try {
            em.getTransaction().begin();

            TypedQuery<FlightBooking> flightsQuery = em.createQuery(
                            "select b from FlightBooking b where (b.id = :idLink)"
                            + "and (b.user = :userLink)"
                            , FlightBooking.class)
                    .setParameter("idLink", id)
                    .setParameter("userLink", bookingUser);

            FlightBooking flightBookingRes = flightsQuery.getSingleResult();

            long flightId = flightBookingRes.getFlight().getId();
            flightBookingRes.getFlight().removeBooking(flightBookingRes);
            em.remove(flightBookingRes);
            em.getTransaction().commit();

            SubscriptionManager.instance().processSubscriptions(flightId);
            return Response.status(Response.Status.NO_CONTENT).build();

        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } finally {
            em.close();
        }
    }
}
