package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GuestSessionDTO;
import ch.uzh.ifi.hase.soprafs26.service.GuestSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuestSessionController {

  private final GuestSessionService guestSessionService;

  public GuestSessionController(GuestSessionService guestSessionService) {
    this.guestSessionService = guestSessionService;
  }

  @PostMapping("/guest-session")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public GuestSessionDTO createGuestSession() {
    String token = guestSessionService.createSession();
    GuestSessionDTO dto = new GuestSessionDTO();
    dto.setToken(token);
    dto.setUsername("Guest demo");
    return dto;
  }

  @DeleteMapping("/guest-session")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearGuestSession(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    guestSessionService.clearSession(extractToken(authHeader));
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) return null;
    if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
    return authHeader;
  }
}
