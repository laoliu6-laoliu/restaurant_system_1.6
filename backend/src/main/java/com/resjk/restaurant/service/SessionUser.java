package com.resjk.restaurant.service;

import javax.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SessionUser {
  public final String id;
  public final String role;
  private SessionUser(String id, String role) { this.id = id; this.role = role; }

  public static SessionUser require(HttpSession session) {
    if (session == null || session.getAttribute("userId") == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
    }
    return new SessionUser((String) session.getAttribute("userId"), (String) session.getAttribute("role"));
  }

  public void checkCustomer(String cno) {
    if ("customer".equals(role) && !id.equals(cno)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能操作其他顾客的订单");
    }
  }
}
