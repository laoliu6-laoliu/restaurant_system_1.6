package com.resjk.restaurant.service;

import com.resjk.restaurant.model.*;
import javax.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordService {
  private final EntityManager em;
  public PasswordService(EntityManager em) { this.em = em; }

  public static class ChangeRequest {
    public String oldPassword;
    public String newPassword;
    public String confirmPassword;
  }

  @Transactional
  public void change(SessionUser user, ChangeRequest request) {
    if (request.oldPassword == null || request.oldPassword.isEmpty()) fail("请输入旧密码");
    if (request.newPassword == null || request.newPassword.length() < 6 || request.newPassword.length() > 20 || request.newPassword.trim().isEmpty()) fail("新密码需为 6 至 20 位");
    if (!request.newPassword.equals(request.confirmPassword)) fail("两次输入的新密码不一致");
    if ("customer".equals(user.role)) {
      Customer account = find(Customer.class, user.id);
      checkOld(account.getCpassword(), request.oldPassword);
      account.setCpassword(request.newPassword);
    } else if ("staff".equals(user.role)) {
      Employee account = find(Employee.class, user.id);
      checkOld(account.getEpassword(), request.oldPassword);
      account.setEpassword(request.newPassword);
    } else if ("admin".equals(user.role)) {
      Administrator account = find(Administrator.class, user.id);
      checkOld(account.getApassword(), request.oldPassword);
      account.setApassword(request.newPassword);
    } else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份不能修改密码");
  }

  private <T> T find(Class<T> type, String id) {
    T account = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在");
    return account;
  }
  private void checkOld(String actual, String old) { if (actual == null || !actual.equals(old)) fail("旧密码不正确"); }
  private void fail(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
