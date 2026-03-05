package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class UserGetDTO {
	// This class defines what the API sends out to clients.

	private Long id;
	private String username;
	private String token;
	private String bio;
	private UserStatus status;

	@JsonProperty("creation_date")
	private Instant creationDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getBio() { return bio; }
	public void setBio(String bio) { this.bio = bio; }

	public Instant getCreationDate() { return creationDate; }
	public void setCreationDate(Instant creationDate) { this.creationDate = creationDate; }

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}
}
