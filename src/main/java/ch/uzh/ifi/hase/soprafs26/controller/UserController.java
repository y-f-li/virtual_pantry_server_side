package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
	this.userService = userService;
	}

	// --- Protected: list users ---
	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers(
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

	userService.authenticate(extractToken(authHeader));

	List<User> users = userService.getUsers();
	List<UserGetDTO> userGetDTOs = new ArrayList<>();
	for (User user : users) {
		userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
	}
	return userGetDTOs;
	}

	// --- Open: register user ---
	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {

	User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
	User createdUser = userService.createUser(userInput);
	return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
	}

	// v3.1: login
	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO login(@RequestBody UserPostDTO userPostDTO) {
		User user = userService.login(userPostDTO.getUsername(), userPostDTO.getPassword());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
	}

	// v3: logout
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader(value="Authorization", required=false) String authHeader) {
		String token = extractToken(authHeader);
		userService.logout(token);
	}

	// --- Protected: view profile by id ---
	@GetMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getUser(
		@PathVariable Long userId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

	userService.authenticate(extractToken(authHeader));

	User user = userService.getUserById(userId);
	return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
	}

	// Helper: "Bearer <token>" -> "<token>"
	private String extractToken(String authHeader) {
	if (authHeader == null || authHeader.isBlank()) return null;
	if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
	return authHeader;
	}

	@PutMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateUser(
		@PathVariable Long userId,
		@RequestHeader(value = "Authorization", required = false) String authHeader,
		@RequestBody UserPutDTO userPutDTO) {

	String token = extractToken(authHeader);

	userService.updateUser(userId, token, userPutDTO);
	}
}