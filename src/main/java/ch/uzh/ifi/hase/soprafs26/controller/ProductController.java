package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.ProductDTO;
import ch.uzh.ifi.hase.soprafs26.service.OpenFoodFactsService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

  private final OpenFoodFactsService openFoodFactsService;
  private final UserService userService;

  public ProductController(OpenFoodFactsService openFoodFactsService, UserService userService) {
    this.openFoodFactsService = openFoodFactsService;
    this.userService = userService;
  }

  @GetMapping("/products/lookup")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ProductDTO lookupByBarcode(
      @RequestParam("barcode") String barcode,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));
    return openFoodFactsService.lookupByBarcode(barcode);
  }

  @GetMapping("/products/search")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<ProductDTO> search(
      @RequestParam("q") String q,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));
    return openFoodFactsService.search(q);
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) return null;
    if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
    return authHeader;
  }
}
