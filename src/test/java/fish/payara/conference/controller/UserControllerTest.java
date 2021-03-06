package fish.payara.conference.controller;

import fish.payara.conference.app.config.Constants;
import fish.payara.conference.app.security.AuthoritiesConstants;
import fish.payara.conference.app.service.UserService;
import fish.payara.conference.controller.dto.ManagedUserDTO;
import fish.payara.conference.domain.User;
import java.util.Arrays;
import static java.util.Collections.singletonMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import static javax.ws.rs.client.Entity.json;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasHeader;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasStatus;
import fish.payara.conference.repository.UserRepository;

/**
 * Test class for the UserController REST controller.
 *
 */
@RunWith(Arquillian.class)
public class UserControllerTest extends ApplicationTest {

    @Inject
    private UserRepository userRepository;

    @Deployment
    public static WebArchive createDeployment() {
        return buildApplication().addClass(UserController.class);
    }

    @Test
    public void testCreateUser() throws Exception {

        ManagedUserDTO validUser = new ManagedUserDTO(
                null, // id
                "joe", // login
                "password", // password
                "Joe", // firstName
                "Shmoe", // lastName
                "joe@example.com", // e-mail
                true, // activated
                "en", // langKey
                null, // createdBy
                null, // createdDate
                null, // lastModifiedBy
                null, // lastModifiedDate
                new HashSet<>(Arrays.asList(AuthoritiesConstants.USER))
        );

        Response response = target("api/users").post(json(validUser));
        assertThat(response, hasStatus(CREATED));

        response = target("api/users/joe").get();
        assertThat(response, hasStatus(Response.Status.OK));
    }

    @Test
    public void testCreateUserDuplicateLogin() throws Exception {

        // Good
        ManagedUserDTO validUser = new ManagedUserDTO(
                null, // id
                "alice", // login
                "password", // password
                "Alice", // firstName
                "Something", // lastName
                "alice@example.com", // e-mail
                true, // activated
                "en", // langKey
                null, // createdBy
                null, // createdDate
                null, // lastModifiedBy
                null, // lastModifiedDate
                new HashSet<>(Arrays.asList(AuthoritiesConstants.USER))
        );

        // Duplicate login, different e-mail
        ManagedUserDTO duplicatedUser = new ManagedUserDTO(
                validUser.getId(), validUser.getLogin(), validUser.getPassword(),
                validUser.getFirstName(), validUser.getLastName(),
                "alicejr@example.com", validUser.isActivated(), validUser.getLangKey(),
                validUser.getCreatedBy(), validUser.getCreatedDate(),
                validUser.getLastModifiedBy(), validUser.getLastModifiedDate(),
                validUser.getAuthorities()
        );

        // Good user
        Response response = target("api/users").post(json(validUser));
        assertThat(response, hasStatus(CREATED));

        // Duplicate login
        Response errorResponse = target("api/users").post(json(duplicatedUser));
        assertThat(errorResponse, hasStatus(BAD_REQUEST));

        Optional<User> userDup = userRepository.findOneByEmail("alicejr@example.com");
        assertFalse(userDup.isPresent());
    }

    @Test
    public void testCreateUserDuplicateEmail() throws Exception {
        // Good
        ManagedUserDTO validUser = new ManagedUserDTO(
                null, // id
                "john", // login
                "password", // password
                "John", // firstName
                "Doe", // lastName
                "john@example.com", // e-mail
                true, // activated
                "en", // langKey
                null, // createdBy
                null, // createdDate
                null, // lastModifiedBy
                null, // lastModifiedDate
                new HashSet<>(Arrays.asList(AuthoritiesConstants.USER))
        );

        // Duplicate e-mail, different login
        ManagedUserDTO duplicatedUser = new ManagedUserDTO(
                validUser.getId(), "johnjr", validUser.getPassword(),
                validUser.getFirstName(), validUser.getLastName(),
                validUser.getEmail(), validUser.isActivated(), validUser.getLangKey(),
                validUser.getCreatedBy(), validUser.getCreatedDate(),
                validUser.getLastModifiedBy(), validUser.getLastModifiedDate(),
                validUser.getAuthorities()
        );

        // Good user
        Response response = target("api/users").post(json(validUser));
        assertThat(response, hasStatus(CREATED));

        // Duplicate  e-mail
        Response errorResponse = target("api/users").post(json(duplicatedUser));
        assertThat(errorResponse, hasStatus(BAD_REQUEST));

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertFalse(userDup.isPresent());
    }

    @Test
    public void testGetExistingUser() throws Exception {
        Response response = target("api/users/admin").get();
        assertThat(response, hasStatus(Response.Status.OK));
        ManagedUserDTO user = response.readEntity(ManagedUserDTO.class);
        assertThat(user.getEmail(), is("admin@example.com"));
    }

    @Test
    public void testGetAllUser() throws Exception {
        Response response = target("api/users", singletonMap("size", 5)).get();
        assertThat(response, hasStatus(Response.Status.OK));
        List<ManagedUserDTO> users = response.readEntity(List.class);
        assertThat(users.size(), is(userRepository.findAll().size()));
    }

    @Test
    public void testGetUnknownUser() throws Exception {
        Response response = target("api/users/unknown").get();
        assertThat(response, hasStatus(Response.Status.NOT_FOUND));
    }

    @Test
    public void testUpdateUser() throws Exception {
        Response response = target("api/users/user").get();
        assertThat(response, hasStatus(Response.Status.OK));
        ManagedUserDTO user = response.readEntity(ManagedUserDTO.class);
        user.setLastName("Gupta");

        response = target("api/users").put(json(user));
        assertThat(response, hasStatus(OK));
    }

    @Test
    public void testDeleteUser() throws Exception {
        Response response = target("api/users/{login}", singletonMap("login", "user")).delete();
        assertThat(response, hasStatus(OK));
    }

    @Test
    public void testValidLogin() throws Exception {
        Response response = login(USERNAME, PASSWORD);
        assertThat(response, hasStatus(Response.Status.OK));
        assertThat(response, hasHeader(Constants.AUTHORIZATION_HEADER));
        String token = response.getHeaderString(Constants.AUTHORIZATION_HEADER);
        assertNotNull(token);
    }

    @Test
    public void testInvalidLogin() throws Exception {
        Response response = login(USERNAME, INVALID_PASSWORD);
        assertThat(response, hasStatus(Response.Status.UNAUTHORIZED));
    }

}
