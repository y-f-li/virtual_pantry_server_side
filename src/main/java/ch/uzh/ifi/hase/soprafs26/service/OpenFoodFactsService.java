package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.ProductDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenFoodFactsService
 *
 * A tiny client for the OpenFoodFacts public API.
 * We keep it intentionally small for the prototype.
 */
@Service
public class OpenFoodFactsService {

  private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsService.class);

  private static final String OFF_BASE = "https://world.openfoodfacts.org";

  // IMPORTANT: OpenFoodFacts asks clients to send a descriptive User-Agent.
  // Update this string with your group/project name.
  private static final String USER_AGENT = "sopra-fs26-virtual-pantry-prototype/0.1";

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private enum BasisUnit { G, ML }

  /**
   * OFF often provides energy per 100g AND/OR per 100ml.
   * We store both and pick the one that matches the package unit.
   */
  private static final class KcalPer100 {
    final Double per100g;
    final Double per100ml;

    private KcalPer100(Double per100g, Double per100ml) {
      this.per100g = per100g;
      this.per100ml = per100ml;
    }
  }

  private static final class KcalChoice {
    final Double value;
    final BasisUnit basis;

    private KcalChoice(Double value, BasisUnit basis) {
      this.value = value;
      this.basis = basis;
    }
  }

  /** amount is normalized to grams or milliliters */
  private static final class NormalizedQuantity {
    final Double amount;
    final BasisUnit unit;

    private NormalizedQuantity(Double amount, BasisUnit unit) {
      this.amount = amount;
      this.unit = unit;
    }
  }

  public ProductDTO lookupByBarcode(String barcode) {
    if (barcode == null || barcode.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "barcode must not be empty");
    }

    // Use the v2 product endpoint and request only the fields we need.
    // NOTE: many products do NOT have structured product_quantity fields; "quantity" is often the only one.
    String url = OFF_BASE + "/api/v2/product/" + barcode + ".json" +
        "?fields=code,product_name,brands,nutriments,product_quantity,product_quantity_unit,quantity,serving_size";

    String body = getWithUserAgent(url);

    try {
      JsonNode root = objectMapper.readTree(body);
      int status = root.path("status").asInt(0);
      if (status != 1) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "OpenFoodFacts: product not found for barcode " + barcode);
      }

      JsonNode product = root.path("product");
      ProductDTO dto = new ProductDTO();
      dto.setBarcode(product.path("code").asText(barcode));
      dto.setName(product.path("product_name").asText("Unknown product"));
      dto.setBrand(nullIfBlank(product.path("brands").asText(null)));

      NormalizedQuantity q = extractPackageQuantity(product);
      if (q != null) {
        dto.setPackageQuantity(q.amount);
        dto.setPackageUnit(q.unit == BasisUnit.G ? "g" : "ml");
      }

      KcalPer100 kcal = extractKcalPer100(product.path("nutriments"));
      KcalChoice chosen = chooseKcal(kcal, q);
      dto.setKcalPer100(chosen.value);
      dto.setKcalPerPackage(estimateKcalPerPackage(chosen, q));

      return dto;
    }
    catch (ResponseStatusException e) {
      throw e;
    }
    catch (Exception e) {
      log.error("Failed to parse OpenFoodFacts response", e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Failed to parse OpenFoodFacts response", e);
    }
  }

  /**
   * Simple keyword search (rate-limited more strictly by OFF). Keep it explicit (button-click), not typeahead.
   */
  public List<ProductDTO> search(String query) {
    if (query == null || query.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must not be empty");
    }

    // Using the legacy search endpoint is fine for a prototype.
    // We keep page_size small to reduce payload.
    String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
    String url = OFF_BASE + "/cgi/search.pl?search_terms=" + q +
        "&search_simple=1&action=process&json=1&page_size=10";

    String body = getWithUserAgent(url);
    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode products = root.path("products");
      List<ProductDTO> results = new ArrayList<>();

      if (products.isArray()) {
        for (JsonNode p : products) {
          ProductDTO dto = new ProductDTO();
          dto.setBarcode(nullIfBlank(p.path("code").asText(null)));
          dto.setName(p.path("product_name").asText("Unknown product"));
          dto.setBrand(nullIfBlank(p.path("brands").asText(null)));

          NormalizedQuantity nq = extractPackageQuantity(p);
          if (nq != null) {
            dto.setPackageQuantity(nq.amount);
            dto.setPackageUnit(nq.unit == BasisUnit.G ? "g" : "ml");
          }

          KcalPer100 kcal = extractKcalPer100(p.path("nutriments"));
          KcalChoice chosen = chooseKcal(kcal, nq);
          dto.setKcalPer100(chosen.value);
          dto.setKcalPerPackage(estimateKcalPerPackage(chosen, nq));

          // Only keep results that have a barcode; otherwise you can't add via barcode.
          if (dto.getBarcode() != null) {
            results.add(dto);
          }
        }
      }
      return results;
    }
    catch (Exception e) {
      log.error("Failed to parse OpenFoodFacts search response", e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Failed to parse OpenFoodFacts search response", e);
    }
  }

  private String getWithUserAgent(String url) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "OpenFoodFacts request failed: " + res.getStatusCode());
      }
      return res.getBody();
    }
    catch (ResponseStatusException e) {
      throw e;
    }
    catch (RestClientException e) {
      log.error("OpenFoodFacts request failed", e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "OpenFoodFacts request failed", e);
    }
  }

  private static KcalPer100 extractKcalPer100(JsonNode nutriments) {
    if (nutriments == null || nutriments.isMissingNode() || nutriments.isNull()) {
      return new KcalPer100(null, null);
    }

    // Some products contain BOTH keys; we keep both.
    Double per100g = asDoubleOrNull(nutriments.path("energy-kcal_100g"));
    Double per100ml = asDoubleOrNull(nutriments.path("energy-kcal_100ml"));

    return new KcalPer100(per100g, per100ml);
  }

  private static KcalChoice chooseKcal(KcalPer100 kcal, NormalizedQuantity q) {
    if (kcal == null) return new KcalChoice(null, null);

    // If we know the package unit, pick matching basis first.
    if (q != null) {
      if (q.unit == BasisUnit.G && kcal.per100g != null) {
        return new KcalChoice(kcal.per100g, BasisUnit.G);
      }
      if (q.unit == BasisUnit.ML && kcal.per100ml != null) {
        return new KcalChoice(kcal.per100ml, BasisUnit.ML);
      }
    }

    // Fallback: prefer 100g, else 100ml.
    if (kcal.per100g != null) return new KcalChoice(kcal.per100g, BasisUnit.G);
    if (kcal.per100ml != null) return new KcalChoice(kcal.per100ml, BasisUnit.ML);

    return new KcalChoice(null, null);
  }

  /**
   * Estimate kcal per package.
   */
  private static Double estimateKcalPerPackage(KcalChoice kcal, NormalizedQuantity q) {
    if (kcal == null || kcal.value == null || kcal.basis == null || q == null) {
      return null;
    }

    // Most of the time, basis matches unit. If it doesn't, we still compute as a rough estimate.
    // (Many drinks store kcal/100g but package in ml; for water-like density the estimate is fine.)
    return kcal.value * (q.amount / 100.0);
  }

  /**
   * Tries multiple ways to obtain a package quantity.
   * OFF is messy; many products only have a human string like "6 x 330 ml" in "quantity".
   */
  private static NormalizedQuantity extractPackageQuantity(JsonNode product) {
    if (product == null || product.isMissingNode() || product.isNull()) {
      return null;
    }

    // 1) Prefer structured fields: product_quantity + product_quantity_unit
    Double qty = null;
    JsonNode pq = product.path("product_quantity");
    if (pq.isNumber()) {
      qty = pq.asDouble();
    } else if (pq.isTextual()) {
      qty = parseDoubleLoose(pq.asText());
    }

    String unit = nullIfBlank(product.path("product_quantity_unit").asText(null));
    NormalizedQuantity structured = normalizeQuantity(qty, unit);
    if (structured != null) {
      return structured;
    }

    // 2) Fallback: parse the human-friendly "quantity" string
    String quantityText = nullIfBlank(product.path("quantity").asText(null));
    NormalizedQuantity parsed = parseQuantityString(quantityText);
    if (parsed != null) {
      return parsed;
    }

    // 3) Another fallback: serving_size sometimes contains "30 g" etc.
    String servingSize = nullIfBlank(product.path("serving_size").asText(null));
    return parseQuantityString(servingSize);
  }

  private static NormalizedQuantity normalizeQuantity(Double amount, String unitRaw) {
    if (amount == null || unitRaw == null) return null;

    String u = unitRaw.trim().toLowerCase();
    if (u.equals("gr") || u.equals("gram") || u.equals("grams")) u = "g";
    if (u.equals("milliliter") || u.equals("milliliters")) u = "ml";
    if (u.equals("liter") || u.equals("liters")) u = "l";

    switch (u) {
      case "g":
        return new NormalizedQuantity(amount, BasisUnit.G);
      case "kg":
        return new NormalizedQuantity(amount * 1000.0, BasisUnit.G);
      case "mg":
        return new NormalizedQuantity(amount / 1000.0, BasisUnit.G);
      case "ml":
        return new NormalizedQuantity(amount, BasisUnit.ML);
      case "l":
        return new NormalizedQuantity(amount * 1000.0, BasisUnit.ML);
      case "cl":
        return new NormalizedQuantity(amount * 10.0, BasisUnit.ML);
      case "dl":
        return new NormalizedQuantity(amount * 100.0, BasisUnit.ML);
      default:
        return null;
    }
  }

  /**
   * Parses strings like:
   * - "500 g" / "1kg"
   * - "6 x 330 ml" / "6x330ml" / "6×330ml" / "6*330ml"
   */
  private static NormalizedQuantity parseQuantityString(String s) {
    if (s == null) return null;
    String t = s.trim().toLowerCase();
    if (t.isEmpty()) return null;

    // comma decimals
    t = t.replace(',', '.');

    Pattern multi = Pattern.compile("(\\d+)\\s*[x×*]\\s*(\\d+(?:\\.\\d+)?)\\s*(kg|g|gr|mg|l|ml|cl|dl)");
    Matcher m = multi.matcher(t);
    if (m.find()) {
      double n = Double.parseDouble(m.group(1));
      double each = Double.parseDouble(m.group(2));
      String unit = m.group(3);
      return normalizeQuantity(n * each, unit);
    }

    Pattern single = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|g|gr|mg|l|ml|cl|dl)");
    m = single.matcher(t);
    if (m.find()) {
      double val = Double.parseDouble(m.group(1));
      String unit = m.group(2);
      return normalizeQuantity(val, unit);
    }

    return null;
  }

  private static Double parseDoubleLoose(String s) {
    if (s == null) return null;
    String t = s.trim().replace(',', '.');
    if (t.isEmpty()) return null;
    try {
      return Double.parseDouble(t);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static Double asDoubleOrNull(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.asDouble();
    }
    if (node.isTextual()) {
      try {
        return Double.parseDouble(node.asText());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private static String nullIfBlank(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
