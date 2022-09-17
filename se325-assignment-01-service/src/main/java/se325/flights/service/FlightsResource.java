package se325.flights.service;

import org.hibernate.type.ZonedDateTimeType;
import se325.flights.domain.Airport;
import se325.flights.domain.Flight;
import se325.flights.domain.User;
import se325.flights.domain.mappers.FlightMapper;
import se325.flights.dto.BookingInfoDTO;
import se325.flights.dto.FlightDTO;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A JAX-RS Resource class for retrieving information about particular flights.
 */

@Path("/flights")
public class FlightsResource {

    /**
     * Parses the given departure date query. If the query matches the format "YYYY-MM-DD" (e.g. "2021-08-16"), an array
     * of two {@link ZonedDateTime} instances corresponding to 00:00:00 and 23:59:59 on the given date in the given
     * timezone is returned. If dayRange is > 0, the range is expanded by (24 * dayRange) hours on either side.
     *
     * @param departureDateQuery the date / time query to parse
     * @param dayRange           the range, in days. Adds (24 * dayRange) hours on each side of the range to search.
     * @param timezone           the timezone to parse. Should come from {@link Airport#getTimeZone()}
     * @return an array of two {@link ZonedDateTime} instances, representing the beginning and end of the given date
     * in the given timezone
     * @throws DateTimeException if departureDateQuery or timezone are invalid
     */
    private ZonedDateTime[] parseDepartureDateQuery(String departureDateQuery, int dayRange, String timezone) throws DateTimeException {
        LocalDate departureDate = LocalDate.parse(departureDateQuery, DateTimeFormatter.ISO_DATE);

        // This method doesn't consider the dayRange argument yet. Modify it so that it does.
        // solved by adding .plusDays(+-dayRange)

        return new ZonedDateTime[]{
                ZonedDateTime.of(departureDate.plusDays(-dayRange), LocalTime.MIN, ZoneId.of(timezone)),
                ZonedDateTime.of(departureDate.plusDays(dayRange), LocalTime.MAX, ZoneId.of(timezone))
        };

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response flightLookupResponse(@QueryParam("origin") String origin,
                                         @QueryParam("destination") String destination,
                                         @QueryParam("departureDate") String departureDate,
                                         @QueryParam("dayRange") @DefaultValue("0") Integer dayRange) {
        if (origin == null || destination == null || dayRange < 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (departureDate != null) {
            try {
                LocalDate departDateParsed = LocalDate.parse(departureDate, DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();
        origin = origin.toLowerCase();
        destination = destination.toLowerCase();
        try {
            // search for just origin and dest
            TypedQuery<Flight> flightsQuery = em.createQuery(
                            "select f from Flight f " +
                            "where (lower(f.origin.name) like concat('%', :origin, '%') or " +
                            "lower(f.origin.code) like concat('%', :origin, '%')) and " +
                            "(lower(f.destination.name) like concat('%', :destination, '%') or " +
                            "lower(f.destination.code) like concat('%', :destination, '%'))" +
                            "order by f.departureTime"
                            , Flight.class)
                    .setParameter("origin", origin)
                    .setParameter("destination", destination);

            //ArrayList<Flight> flightsList = new ArrayList<>(flightsQuery.getResultList());
            List<Flight> flightList = flightsQuery.getResultList();
            List<FlightDTO> flightDTOList = new ArrayList<>();

            if (departureDate == null) {
                for (Flight i : flightList){
                    flightDTOList.add(FlightMapper.toDTO(i));
                }

            } else {
                String timeZone = flightList.get(0).getOrigin().getTimeZone();
                ZonedDateTime[] dateRange = parseDepartureDateQuery(departureDate, dayRange, timeZone);
                for (Flight i : flightList){
                    if (i.getDepartureTime().isAfter(dateRange[0]) && i.getDepartureTime().isBefore(dateRange[1])){
                        flightDTOList.add(FlightMapper.toDTO(i));
                    }
                }
            }
            return Response.ok(flightDTOList).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("{id}/booking-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bookingInfo(@PathParam("id") long id) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            TypedQuery<Flight> flightsQuery = em.createQuery(
                    "select f from Flight f where f.id = :idLink", Flight.class)
                    .setParameter("idLink", id);

            if (flightsQuery.getResultList().size() == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            BookingInfoDTO infoRes = FlightMapper.toBookingInfoDTO(flightsQuery.getSingleResult());

            return Response.ok(infoRes).build();

        } finally {
            em.close();
        }
    }

}
