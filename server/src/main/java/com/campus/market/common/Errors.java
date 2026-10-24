package com.campus.market.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Errors {
  private Errors() {}

  public static ResponseStatusException bad(String m) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
  }

  public static ResponseStatusException missing(String m) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
  }

  public static ResponseStatusException forbidden(String m) {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, m);
  }

  public static ResponseStatusException conflict(String m) {
    return new ResponseStatusException(HttpStatus.CONFLICT, m);
  }
}
