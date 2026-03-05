/*

[My Implementation] Error response DTO.

*/

package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ErrorResponseDTO {
  private String reason;

  public ErrorResponseDTO() {}

  public ErrorResponseDTO(String reason) {
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}