package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;

import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		// backend check for username password not null
		if (newUser.getUsername() == null || newUser.getUsername().isBlank()
			|| newUser.getPassword() == null || newUser.getPassword().isBlank()) {
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password must not be empty");
		}

		// normalize whitespace-only bio to null
		if (newUser.getBio() != null && newUser.getBio().isBlank()) {
			newUser.setBio(null);
		}

		// check for duplicate users.
		checkIfUserExists(newUser);
		
		// generate token and log the user in.
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.ONLINE);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */

	/*
	
	[my implementation] 
	Change the status code from 400 (Bad Request) TO 409 (conflict) In order to follow the specification (page 5 on specification) 
	
	*/

	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		// User userByName = userRepository.findByName(userToBeCreated.getName());

		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
		} 
	}

	public User authenticate(String token) {
	if (token == null || token.isBlank()) {
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing token");
	}

	User user = userRepository.findByToken(token);
	if (user == null) {
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
	}

	return user;
	}

	public User getUserById(Long userId) {
	return userRepository.findById(userId)
		.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
	}

	// v3.1 login 
	public User login(String username, String password) {
	if (username == null || username.isBlank() || password == null || password.isBlank()) {
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password must not be empty");
	}

	User user = userRepository.findByUsername(username);
	if (user == null || !user.getPassword().equals(password)) {
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
	}

	// Issue a fresh token on each login (clean + secure enough for this project)
	user.setToken(UUID.randomUUID().toString());
	user.setStatus(UserStatus.ONLINE);
	return userRepository.saveAndFlush(user);
	}

	// v3 logout
	public void logout(String token) {
		User user = authenticate(token); // will throw 401 if missing/invalid
		user.setStatus(UserStatus.OFFLINE);
		user.setToken(UUID.randomUUID().toString()); // invalidate old token
		userRepository.saveAndFlush(user);
	}

	// v6 change password
	public void updateUser(Long targetUserId, String token, UserPutDTO dto) {
	User authUser = authenticate(token); // 401 if missing/invalid token

	// Only allow updating your own user
	// 1) ensure userId exists
	userRepository.findById(targetUserId)
	.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

	// 2) hide “not self” as 404 to align with spec
	if (!authUser.getId().equals(targetUserId)) {
	throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
	}

	// If password is provided, interpret as "change password"
	if (dto.getPassword() != null) {
		String oldPw = dto.getOldPassword();
		String newPw = dto.getPassword();

		if (oldPw == null || oldPw.isBlank() || newPw == null || newPw.isBlank()) {
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oldPassword and password must not be empty");
		}

		if (!authUser.getPassword().equals(oldPw)) {
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "old password is incorrect");
		}

		authUser.setPassword(newPw);

		// Force logout by invalidating token + going offline
		authUser.setStatus(UserStatus.OFFLINE);
		authUser.setToken(UUID.randomUUID().toString());
	}

	userRepository.saveAndFlush(authUser);
	}

}
