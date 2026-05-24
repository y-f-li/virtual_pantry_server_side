package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GuestSessionService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

  private final UserService userService;
  private final GuestSessionService guestSessionService;

  UserController(UserService userService, GuestSessionService guestSessionService) {
    this.userService = userService;
    this.guestSessionService = guestSessionService;
  }

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

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // INTENTIONAL ROLLBACK TEST PATCH:
    // This endpoint normally registers a new user. For the rollback smoke test,
    // it delegates to login instead so the deployed version is visibly different.
    User loggedInUser = userService.login(userPostDTO.getUsername(), userPostDTO.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO login(@RequestBody UserPostDTO userPostDTO) {
    // INTENTIONAL ROLLBACK TEST PATCH:
    // This endpoint normally logs in an existing user. For the rollback smoke test,
    // it delegates to registration instead so login/register behavior is swapped.
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User createdUser = userService.createUser(userInput);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);
    if (guestSessionService.isGuestToken(token)) {
      guestSessionService.clearSession(token);
      return;
    }
    userService.logout(token);
  }

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

  @PutMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUser(
      @PathVariable Long userId,
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody UserPutDTO userPutDTO) {

    String token = extractToken(authHeader);
    userService.updateUser(userId, token, userPutDTO);
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) return null;
    if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
    return authHeader;
  }
}
