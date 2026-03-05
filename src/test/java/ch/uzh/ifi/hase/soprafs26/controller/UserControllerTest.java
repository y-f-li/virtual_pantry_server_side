package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		// given
		User user = new User();
		user.setUsername("firstname@lastname");
		user.setPassword("pw");
		user.setToken("test-token");
		user.setBio("hello");
		user.setStatus(UserStatus.OFFLINE);

		List<User> allUsers = Collections.singletonList(user);

		given(userService.authenticate(Mockito.eq("test-token"))).willReturn(user);

		given(userService.getUsers()).willReturn(allUsers);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users")
			.header("Authorization", "Bearer test-token")
			.contentType(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].username", is(user.getUsername())))
			.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())))
			.andExpect(jsonPath("$[0].bio", is(user.getBio())))
			.andExpect(jsonPath("$[0].token", is(user.getToken())));
	}

	@Test
	public void createUser_validInput_userCreated() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setPassword("pw");       // exists on entity (should NOT be returned)
		user.setBio("hi");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");
		userPostDTO.setPassword("pw");   // NEW: required by v7 DTO
		userPostDTO.setBio("hi");

		// createUser(User newUser) DOES take a User argument in v7
		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id", is(user.getId().intValue())))
			.andExpect(jsonPath("$.username", is(user.getUsername())))
			.andExpect(jsonPath("$.status", is(user.getStatus().toString())))
			.andExpect(jsonPath("$.bio", is(user.getBio())))
			.andExpect(jsonPath("$.token", is(user.getToken())));
	}

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}

	@Test
	public void createUser_duplicateUsername_returns409AndReason() throws Exception {
		// given
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");
		userPostDTO.setPassword("pw");
		userPostDTO.setBio("hi");

		given(userService.createUser(Mockito.any()))
			.willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "username already exists"));

		// when
		MockHttpServletRequestBuilder postRequest = post("/users")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.reason", is("username already exists")));
	}


	@Test
	public void getUser_validId_returns200AndUserJson() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setUsername("testUsername");
		user.setPassword("pw");
		user.setBio("hello");
		user.setToken("test-token");
		user.setStatus(UserStatus.ONLINE);

		// endpoint is protected -> controller calls authenticate(token)
		given(userService.authenticate(Mockito.eq("test-token"))).willReturn(user);

		// controller calls getUserById(userId)
		given(userService.getUserById(1L)).willReturn(user);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users/1")
			.header("Authorization", "Bearer test-token")
			.accept(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(user.getId().intValue())))
			.andExpect(jsonPath("$.username", is(user.getUsername())))
			.andExpect(jsonPath("$.status", is(user.getStatus().toString())))
			.andExpect(jsonPath("$.bio", is(user.getBio())))
			.andExpect(jsonPath("$.token", is(user.getToken())));
	}


	@Test
	public void getUser_unknownId_returns404AndReason() throws Exception {
		// given
		User authUser = new User();
		authUser.setId(1L);
		authUser.setUsername("authUser");
		authUser.setPassword("pw");
		authUser.setToken("test-token");
		authUser.setStatus(UserStatus.ONLINE);

		given(userService.authenticate(Mockito.eq("test-token"))).willReturn(authUser);

		given(userService.getUserById(999L))
			.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

		// when
		MockHttpServletRequestBuilder getRequest = get("/users/999")
			.header("Authorization", "Bearer test-token")
			.accept(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.reason", is("user not found")));
	}


	@Test
	public void updateUser_validId_returns204() throws Exception {
		// given
		UserPutDTO userPutDTO = new UserPutDTO();
		userPutDTO.setOldPassword("oldPw");
		userPutDTO.setPassword("newPw");

		Mockito.doNothing().when(userService)
			.updateUser(Mockito.eq(1L), Mockito.eq("test-token"), Mockito.any(UserPutDTO.class));

		// when
		MockHttpServletRequestBuilder putRequest = put("/users/1")
			.header("Authorization", "Bearer test-token")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPutDTO));

		// then
		mockMvc.perform(putRequest)
			.andExpect(status().isNoContent());
	}

	@Test
	public void updateUser_unknownId_returns404AndReason() throws Exception {
		// given
		UserPutDTO userPutDTO = new UserPutDTO();
		userPutDTO.setOldPassword("oldPw");
		userPutDTO.setPassword("newPw");

		Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"))
			.when(userService)
			.updateUser(Mockito.eq(999L), Mockito.eq("test-token"), Mockito.any(UserPutDTO.class));

		// when
		MockHttpServletRequestBuilder putRequest = put("/users/999")
			.header("Authorization", "Bearer test-token")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPutDTO));

		// then
		mockMvc.perform(putRequest)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.reason", is("user not found")));
	}

}

