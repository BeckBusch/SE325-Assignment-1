package se325.flights.service;

import se325.flights.domain.User;
import se325.flights.domain.mappers.UserMapper;
import se325.flights.dto.UserDTO;
import se325.flights.util.SecurityUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.*;
import java.net.URI;


/**
 * A JAX-RS resource class which handles requests to create user accounts, log in, and log out.
 */

@Path("/users")
public class UserResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(UserDTO userDto) {

        EntityManager em = PersistenceManager.instance().createEntityManager();
        User userTemp = UserMapper.toDomain(userDto);
        try {
            TypedQuery<User> userQuery = em.createQuery("select u from User u " +
                                                        "where u.username = :username", User.class)
                    .setParameter("username", userTemp.getUsername());
            if (userQuery.getResultList().size() != 0) {
                return Response.status(Response.Status.CONFLICT).build();
            }

            em.getTransaction().begin();
            em.persist(userTemp);
            em.getTransaction().commit();

            return Response.created(URI.create("/users/" + userTemp.getId())).build();
        } finally {
            em.close();
        }
    }

    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userLogin(UserDTO userDto) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User userTemp = UserMapper.toDomain(userDto);

        try {
            TypedQuery<User> userQuery = em.createQuery("select u from User u " +
                                                        "where u.username = :username", User.class)
                    .setParameter("username",  userTemp.getUsername());

            // testing for user exist
            if (userQuery.getResultList().size() == 0) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            em.getTransaction().begin();
            User userAuth = userQuery.getSingleResult();

            // check for auth
            if (!userTemp.getPassHash().equals(userAuth.getPassHash())){
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            NewCookie userCookie = SecurityUtils.generateAuthCookie();
            userAuth.setAuthToken(userCookie.getValue());
            em.getTransaction().commit();

            return Response.noContent().cookie(userCookie).build();
        } finally {
            em.close();
        }


    }

    @GET
    @Path("logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userLogout() {
        NewCookie deadCookie = SecurityUtils.generateDeleteAuthCookie();
        return Response.noContent().cookie(deadCookie).build();
    }
}
