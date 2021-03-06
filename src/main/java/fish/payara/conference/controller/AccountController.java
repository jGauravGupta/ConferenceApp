package fish.payara.conference.controller;

import fish.payara.conference.repository.UserRepository;
import fish.payara.conference.domain.User;
import fish.payara.conference.app.security.SecurityUtils;
import fish.payara.conference.app.service.MailService;
import fish.payara.conference.app.service.UserService;
import fish.payara.conference.controller.dto.KeyAndPasswordDTO;
import fish.payara.conference.controller.dto.ManagedUserDTO;
import fish.payara.conference.controller.dto.UserDTO;
import fish.payara.conference.controller.util.HeaderUtil;

import static fish.payara.conference.app.config.Constants.INCORRECT_PASSWORD_MESSAGE;
import java.util.*;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * REST controller for managing the current user's account.
 */
@Path("/api")
public class AccountController {

    @Inject
    private Logger log;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserService userService;

    @Inject
    private MailService mailService;

    @Inject
    private SecurityUtils securityUtils;

    @Context
    private HttpServletRequest request;

    /**
     * POST /register : register the user.
     *
     * @param managedUserDTO the managed user DTO
     * @return the Response with status 201 (Created) if the user is registered
     * or 400 (Bad Request) if the login or e-mail is already in use
     */
    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response registerAccount(@Valid ManagedUserDTO managedUserDTO) {
        if (!checkPasswordLength(managedUserDTO.getPassword())) {
            return Response.status(BAD_REQUEST).entity(INCORRECT_PASSWORD_MESSAGE).build();
        }
        return userRepository.findOneByLogin(managedUserDTO.getLogin().toLowerCase())
                .map(user -> Response.status(BAD_REQUEST).type(TEXT_PLAIN).entity("login already in use").build())
                .orElseGet(() -> userRepository.findOneByEmail(managedUserDTO.getEmail())
                .map(user -> Response.status(BAD_REQUEST).type(TEXT_PLAIN).entity("e-mail address already in use").build())
                .orElseGet(() -> {
                    User user = userService.createUser(managedUserDTO.getLogin(), managedUserDTO.getPassword(),
                            managedUserDTO.getFirstName(), managedUserDTO.getLastName(),
                            managedUserDTO.getEmail().toLowerCase(), managedUserDTO.getLangKey());
                    mailService.sendActivationEmail(user);
                    return Response.status(CREATED).build();
                })
                );
    }

    /**
     * GET /activate : activate the registered user.
     *
     * @param key the activation key
     * @return the Response with status 200 (OK) and the activated user in body,
     * or status 500 (Internal Server Error) if the user couldn't be activated
     */
    @Path("/activate")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response activateAccount(@QueryParam("key") String key) {
        return userService.activateRegistration(key)
                .map(user -> Response.ok().build())
                .orElse(Response.status(INTERNAL_SERVER_ERROR).build());
    }

    /**
     * GET /authenticate : check if the user is authenticated, and return its
     * login.
     *
     * @return the login if the user is authenticated
     */
    @Path("/authenticate")
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String isAuthenticated() {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET /account : get the current user.
     *
     * @return the Response with status 200 (OK) and the current user in body,
     * or status 500 (Internal Server Error) if the user couldn't be returned
     */
    @Path("/account")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
                .map(user -> Response.ok(new UserDTO(user)).build())
                .orElse(Response.status(INTERNAL_SERVER_ERROR).build());
    }

    /**
     * POST /account : update the current user information.
     *
     * @param userDTO the current user information
     * @return the Response with status 200 (OK), or status 400 (Bad Request) or
     * 500 (Internal Server Error) if the user couldn't be updated
     */
    @Path("/account")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveAccount(@Valid UserDTO userDTO) {
        final String userLogin = securityUtils.getCurrentUserLogin();
        Optional<User> existingUser = userRepository.findOneByEmail(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            return HeaderUtil.createFailureAlert(Response.status(BAD_REQUEST), "user-management", "emailexists", "Email already in use").build();
        }
        return userRepository
                .findOneByLogin(userLogin)
                .map(user -> {
                    userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                            userDTO.getLangKey());
                    return Response.ok().build();
                })
                .orElseGet(() -> Response.status(INTERNAL_SERVER_ERROR).build());
    }

    /**
     * POST /account/change-password : changes the current user's password
     *
     * @param password the new password
     * @return the Response with status 200 (OK), or status 400 (Bad Request) if
     * the new password is not strong enough
     */
    @Path("/account/change-password")
    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public Response changePassword(String password) {
        if (!checkPasswordLength(password)) {
            return Response.status(BAD_REQUEST).entity(INCORRECT_PASSWORD_MESSAGE).build();
        }
        userService.changePassword(password);
        return Response.ok().build();
    }

    /**
     * POST /account/reset-password/init : Send an e-mail to reset the password
     * of the user
     *
     * @param mail the mail of the user
     * @return the Response with status 200 (OK) if the e-mail was sent, or
     * status 400 (Bad Request) if the e-mail address is not registred
     */
    @Path("/account/reset-password/init")
    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public Response requestPasswordReset(String mail) {
        return userService.requestPasswordReset(mail)
                .map(user -> {
                    mailService.sendPasswordResetMail(user);
                    return Response.ok("email was sent").build();
                }).orElse(Response.status(BAD_REQUEST).entity("email address not registered").build());
    }

    /**
     * POST /account/reset-password/finish : Finish to reset the password of the
     * user
     *
     * @param keyAndPassword the generated key and the new password
     * @return the Response with status 200 (OK) if the password has been reset,
     * or status 400 (Bad Request) or 500 (Internal Server Error) if the
     * password could not be reset
     */
    @Path("/account/reset-password/finish")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.TEXT_PLAIN})
    public Response finishPasswordReset(KeyAndPasswordDTO keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            return Response.status(BAD_REQUEST).entity(INCORRECT_PASSWORD_MESSAGE).build();
        }
        return userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
                .map(user -> Response.ok().build())
                .orElse(Response.status(INTERNAL_SERVER_ERROR).build());
    }

    private boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password)
                && password.length() >= ManagedUserDTO.PASSWORD_MIN_LENGTH
                && password.length() <= ManagedUserDTO.PASSWORD_MAX_LENGTH;
    }
}
