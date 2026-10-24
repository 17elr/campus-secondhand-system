package com.campus.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NoticeController {
  private final NoticeMapper mapper;
  private final ObjectMapper json;

  public NoticeController(NoticeMapper mapper, ObjectMapper json) {
    this.mapper = mapper;
    this.json = json;
  }

  @PutMapping("/internal/notifications/{eventId}")
  @Transactional
  public Map<String, Object> deliver(
      @PathVariable String eventId, @Valid @RequestBody Notification n) throws Exception {
    try {
      UUID.fromString(eventId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event ID");
    }
    String hash =
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(n)));
    mapper.insert(eventId, n, hash);
    Map<String, Object> saved = mapper.event(eventId, n.recipientId());
    if (!hash.equals(saved.get("payload_hash")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Event content differs");
    return Map.of("id", saved.get("id"));
  }

  @GetMapping("/internal/users/{userId}/notifications")
  public Map<String, Object> list(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "9") int size) {
    if (page < 1 || page > 1000000 || size < 1 || size > 100)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
    return Map.of(
        "records",
        mapper.page(userId, size, (long) (page - 1) * size),
        "total",
        mapper.count(userId),
        "unread",
        mapper.unread(userId),
        "page",
        page,
        "size",
        size);
  }

  @PutMapping("/internal/users/{userId}/notifications/{id}/read")
  public Map<String, Object> read(@PathVariable Long userId, @PathVariable Long id) {
    mapper.read(userId, id);
    if (mapper.owns(userId, id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return Map.of("read", true);
  }
}
