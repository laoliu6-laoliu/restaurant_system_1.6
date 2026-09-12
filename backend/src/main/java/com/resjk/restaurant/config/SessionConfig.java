package com.resjk.restaurant.config;
import com.resjk.restaurant.service.SessionUser;
import javax.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class SessionConfig implements WebMvcConfigurer {
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        SessionUser user = SessionUser.require(request.getSession(false));
        String path = request.getRequestURI();
        if ("customer".equals(user.role) && (path.startsWith("/api/employees") || path.startsWith("/api/reports") || path.startsWith("/api/dashboard"))) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份没有查看权限");
        }
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
          boolean orderAction = path.equals("/api/orders") || path.startsWith("/api/orders/") || path.startsWith("/api/payments/");
          boolean passwordAction = path.equals("/api/auth/password") && "POST".equals(request.getMethod());
          boolean ownProfile = "PUT".equals(request.getMethod()) &&
              (("customer".equals(user.role) && path.equals("/api/customers/" + user.id)) ||
              ("staff".equals(user.role) && path.equals("/api/employees/" + user.id)));
          boolean staffAction = "staff".equals(user.role) && (path.startsWith("/api/tables/") || path.equals("/api/customers") || path.startsWith("/api/reservations"));
          boolean customerReservation = "customer".equals(user.role) && "POST".equals(request.getMethod()) && path.equals("/api/reservations");
          if (!"admin".equals(user.role) && !orderAction && !ownProfile && !staffAction && !passwordAction && !customerReservation) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前身份没有操作权限");
          }
        }
        return true;
      }
    }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login", "/api/auth/logout");
  }
}
