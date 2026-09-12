package com.resjk.restaurant;

import com.fasterxml.jackson.databind.*;
import com.resjk.restaurant.service.CheckoutService;
import javax.persistence.EntityManager;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000",
  "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
  "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.show-sql=false", "app.upgrade-enabled=false"
})
@AutoConfigureMockMvc
class CheckoutIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired com.resjk.restaurant.service.DailyOrderSequence orderSequence;

  MockHttpSession customer;
  MockHttpSession staff;

  @BeforeEach
  void seed() throws Exception {
    for (String table : new String[]{"payment", "reservation", "order_detail", "orders", "order_sequence", "menu", "dining_table", "customer", "employee", "administrator"}) jdbc.update("DELETE FROM " + table);
    jdbc.update("INSERT INTO customer(cno,cname,cphone,cpassword) VALUES ('C0001','Alice','13800000001','123456'),('C0002','Bob','13800000002','123456')");
    jdbc.update("INSERT INTO employee(eno,ename,epassword) VALUES ('E0001','Staff','123456')");
    jdbc.update("INSERT INTO administrator(ano,aname,apassword) VALUES ('A0001','Admin','123456')");
    jdbc.update("INSERT INTO dining_table(tno,seats,tstatus) VALUES ('T0001',2,'空闲'),('T0002',4,'空闲'),('T0008',8,'空闲'),('T0013',12,'空闲')");
    jdbc.update("INSERT INTO menu(mno,mname,mtype,mprice,stock) VALUES ('M0001','Steak','主菜',88.00,5),('M0002','Pasta','主食',68.00,1)");
    customer = login("customer", "C0001"); staff = login("staff", "E0001");
  }

  MockHttpSession login(String role, String id) throws Exception {
    return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType("application/json")
        .content("{\"role\":\"" + role + "\",\"id\":\"" + id + "\",\"password\":\"123456\"}"))
        .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
  }

  String checkout() throws Exception {
    String body = mvc.perform(post("/api/orders/checkout").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"cno\":\"C0002\",\"totalAmount\":0.01,\"paymentStatus\":\"PAID\",\"items\":[{\"mno\":\"M0001\",\"dishCount\":2}]}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(176.00))
        .andExpect(jsonPath("$.cno").value("C0001")).andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
        .andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("ono").asText();
  }

  ResultActions pay(String ono, MockHttpSession session, String method) throws Exception {
    return mvc.perform(post("/api/payments/" + ono).session(session).contentType("application/json").content("{\"method\":\"" + method + "\",\"amount\":0.01}"));
  }

  @Test void checkoutCalculatesAmountAndPaymentPersistsExactlyOnce() throws Exception {
    String ono = checkout();
    assertThat(jdbc.queryForObject("SELECT stock FROM menu WHERE mno='M0001'", Integer.class)).isEqualTo(3);
    String first = pay(ono, customer, "DEMO_WECHAT").andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(176)).andReturn().getResponse().getContentAsString();
    String second = pay(ono, customer, "DEMO_WECHAT").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertThat(json.readTree(second).get("paymentNo")).isEqualTo(json.readTree(first).get("paymentNo"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE ono=?", String.class, ono)).isEqualTo("PAID");
    mvc.perform(delete("/api/orders/" + ono).session(staff)).andExpect(status().isConflict());
  }

  @Test void insufficientStockRollsBackAllWrites() throws Exception {
    mvc.perform(post("/api/orders/checkout").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"items\":[{\"mno\":\"M0001\",\"dishCount\":2},{\"mno\":\"M0002\",\"dishCount\":2}]}"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT stock FROM menu WHERE mno='M0001'", Integer.class)).isEqualTo(5);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_detail", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("空闲");
  }

  @Test void refusesInvalidQuantityAndDuplicateDishes() throws Exception {
    for (String items : new String[]{"[]", "[{\"mno\":\"M0001\",\"dishCount\":0}]", "[{\"mno\":\"M0001\",\"dishCount\":1},{\"mno\":\"M0001\",\"dishCount\":1}]"}) {
      mvc.perform(post("/api/orders/checkout").session(customer).contentType("application/json").content("{\"tno\":\"T0001\",\"items\":" + items + "}"))
          .andExpect(status().isBadRequest());
    }
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
  }

  @Test void ownershipSessionAndCashPermissionsAreEnforced() throws Exception {
    String ono = checkout();
    mvc.perform(post("/api/payments/" + ono).contentType("application/json").content("{\"method\":\"DEMO_WECHAT\"}")).andExpect(status().isUnauthorized());
    MockHttpSession other = login("customer", "C0002");
    pay(ono, other, "DEMO_WECHAT").andExpect(status().isForbidden());
    mvc.perform(get("/api/orders").session(other)).andExpect(jsonPath("$.content.length()").value(0));
    mvc.perform(get("/api/order-details").session(other).param("ono", ono)).andExpect(status().isForbidden());
    pay(ono, customer, "DEMO_CASH").andExpect(status().isForbidden());
    pay(ono, customer, "WECHAT").andExpect(status().isBadRequest());
    pay(ono, staff, "DEMO_CASH").andExpect(status().isOk());
    mvc.perform(get("/api/payments").session(other)).andExpect(jsonPath("$.content.length()").value(0));
    mvc.perform(put("/api/menu/M0001").session(customer).contentType("application/json").content("{\"mprice\":0.01}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/auth/logout").session(customer)).andExpect(status().isOk());
    mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
  }

  @Test void staffEndsAnUnpaidOrderBeforeDeletingItAndRestoresStock() throws Exception {
    String ono = checkout();
    mvc.perform(get("/api/orders").session(customer)).andExpect(jsonPath("$.content[0].paymentStatus").value("UNPAID"));
    mvc.perform(delete("/api/orders/" + ono).session(staff)).andExpect(status().isConflict());
    mvc.perform(post("/api/orders/" + ono + "/end").session(customer)).andExpect(status().isForbidden());
    mvc.perform(post("/api/orders/" + ono + "/end").session(staff)).andExpect(status().isOk())
        .andExpect(jsonPath("$.orderStatus").value("ENDED")).andExpect(jsonPath("$.endedAt").isNotEmpty());
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("待清理");
    String orderDate = jdbc.queryForObject("SELECT CAST(order_date AS VARCHAR) FROM orders WHERE ono=?", String.class, ono);
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content("{\"orderDate\":\"" + orderDate + "\",\"orderNumber\":\"001\",\"partySize\":5,\"targetTno\":\"T0008\"}"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT tno FROM orders WHERE ono=?", String.class, ono)).isEqualTo("T0001");
    mvc.perform(delete("/api/orders/" + ono).session(staff)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_detail", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT stock FROM menu WHERE mno='M0001'", Integer.class)).isEqualTo(5);
  }

  @Test void concurrentPaymentsReturnTheSameReceipt() throws Exception {
    String ono = checkout();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<String> task = () -> { start.await(); return pay(ono, customer, "DEMO_ALIPAY").andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); };
    try {
      Future<String> a = pool.submit(task); Future<String> b = pool.submit(task); start.countDown();
      assertThat(json.readTree(a.get(15, TimeUnit.SECONDS)).get("paymentNo")).isEqualTo(json.readTree(b.get(15, TimeUnit.SECONDS)).get("paymentNo"));
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class)).isEqualTo(1);
    } finally { pool.shutdownNow(); }
  }

  @Test void inconsistentLegacyAmountAndDirectBillEditsAreRejected() throws Exception {
    String ono = checkout();
    jdbc.update("UPDATE orders SET total_amount=1 WHERE ono=?", ono);
    pay(ono, customer, "DEMO_WECHAT").andExpect(status().isBadRequest());
    mvc.perform(put("/api/orders/" + ono).session(staff).contentType("application/json").content("{}")).andExpect(status().isMethodNotAllowed());
    mvc.perform(post("/api/order-details").session(staff).contentType("application/json").content("{}")).andExpect(status().isMethodNotAllowed());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class)).isZero();
  }

  @Test void demoSwitchCannotFallThroughToSuccessfulPayment() {
    CheckoutService disabled = new CheckoutService(mock(EntityManager.class), false, java.time.Clock.systemUTC(), mock(com.resjk.restaurant.service.DailyOrderSequence.class), mock(com.resjk.restaurant.service.ReservationService.class));
    assertThatThrownBy(() -> disabled.pay("O1", "DEMO_WECHAT", null)).hasMessageContaining("503");
  }

  @Test void accountResponsesHidePasswordsAndBlankProfilePasswordKeepsLoginWorking() throws Exception {
    mvc.perform(get("/api/customers").session(customer)).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].cpassword").doesNotExist());
    mvc.perform(get("/api/employees").session(staff)).andExpect(jsonPath("$[0].epassword").doesNotExist());
    mvc.perform(put("/api/customers/C0001").session(customer).contentType("application/json").content("{\"cname\":\"Updated\",\"cpassword\":\"\"}")).andExpect(status().isOk());
    login("customer", "C0001");
    mvc.perform(post("/api/orders/checkout").session(customer).contentType("application/json").content("{\"tno\":\"T0001\",\"items\":[{\"mno\":\"M0001\",\"dishCount\":1.5}]}")).andExpect(status().isBadRequest());
  }

  @Test void passwordChangeRequiresOldPasswordAndMatchingConfirmation() throws Exception {
    mvc.perform(post("/api/auth/password").session(customer).contentType("application/json")
        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"abcdef\",\"confirmPassword\":\"abcdef\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/password").session(customer).contentType("application/json")
        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"abcdef\",\"confirmPassword\":\"abcdeg\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/password").session(customer).contentType("application/json")
        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"abcdef\",\"confirmPassword\":\"abcdef\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"role\":\"customer\",\"id\":\"C0001\",\"password\":\"123456\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"role\":\"customer\",\"id\":\"C0001\",\"password\":\"abcdef\"}"))
        .andExpect(status().isOk());
    mvc.perform(put("/api/customers/C0001").session(customer).contentType("application/json")
        .content("{\"cname\":\"Alice\",\"cpassword\":\"bypass1\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test void dailyNumbersRestartAndDateQueriesArePaged() throws Exception {
    assertThat(orderSequence.next(java.time.LocalDate.of(2027, 1, 1))).isEqualTo(1);
    assertThat(orderSequence.next(java.time.LocalDate.of(2027, 1, 1))).isEqualTo(2);
    assertThat(orderSequence.next(java.time.LocalDate.of(2027, 1, 2))).isEqualTo(1);
    jdbc.update("UPDATE order_sequence SET last_number=998 WHERE order_date='2027-01-02'");
    assertThat(orderSequence.next(java.time.LocalDate.of(2027, 1, 2))).isEqualTo(999);
    assertThatThrownBy(() -> orderSequence.next(java.time.LocalDate.of(2027, 1, 2))).hasMessageContaining("999");
    String ono = checkout();
    String today = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).toString();
    mvc.perform(get("/api/orders").session(customer).param("startDate", today).param("endDate", today).param("size", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].ono").value(ono)).andExpect(jsonPath("$.content[0].orderNumber").value("001"))
        .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1));
    mvc.perform(get("/api/orders").session(customer).param("startDate", "2020-01-01").param("endDate", "2020-01-01"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    mvc.perform(get("/api/orders").session(customer).param("startDate", "2026-09-10").param("endDate", "2026-09-09"))
        .andExpect(status().isBadRequest());
  }

  @Test void monthlyRevenueAndDishChampionUseTheSelectedMonth() throws Exception {
    String ono = checkout();
    pay(ono, staff, "DEMO_CASH").andExpect(status().isOk());
    String month = java.time.YearMonth.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).toString();
    mvc.perform(get("/api/reports/order-summary").session(staff).param("month", month))
        .andExpect(status().isOk()).andExpect(jsonPath("$.month").value(month))
        .andExpect(jsonPath("$.totalRevenue").value(176)).andExpect(jsonPath("$.orderCount").value(1));
    mvc.perform(get("/api/reports/dish-sales-rank").session(staff).param("month", month))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].mname").value("Steak"))
        .andExpect(jsonPath("$[0].totalCount").value(2)).andExpect(jsonPath("$[0].totalAmount").value(176));
    mvc.perform(get("/api/reports/order-summary").session(staff).param("month", "2020-01"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(0)).andExpect(jsonPath("$.orderCount").value(0));
    mvc.perform(get("/api/reports/dish-sales-rank").session(staff).param("month", "2020-01"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/reports/order-summary").session(staff).param("month", "2026-13"))
        .andExpect(status().isBadRequest());
  }

  @Test void staffTransfersUnpaidAndPaidOrdersThenAdminEndsAndDeletesThePaidOrder() throws Exception {
    String ono = checkout();
    String orderDate = jdbc.queryForObject("SELECT CAST(order_date AS VARCHAR) FROM orders WHERE ono=?", String.class, ono);
    String requestPrefix = "{\"orderDate\":\"" + orderDate + "\",\"orderNumber\":\"001\",";

    mvc.perform(post("/api/orders/transfer").session(customer).contentType("application/json")
        .content(requestPrefix + "\"partySize\":5,\"targetTno\":\"T0008\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content(requestPrefix + "\"partySize\":2,\"targetTno\":\"T0008\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content(requestPrefix + "\"partySize\":5,\"targetTno\":\"T0002\"}"))
        .andExpect(status().isConflict());

    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content(requestPrefix + "\"partySize\":5,\"targetTno\":\"T0008\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.orderNumber").value("001"))
        .andExpect(jsonPath("$.oldTno").value("T0001")).andExpect(jsonPath("$.newTno").value("T0008"))
        .andExpect(jsonPath("$.targetSeats").value(8)).andExpect(jsonPath("$.partySize").value(5));
    assertThat(jdbc.queryForObject("SELECT tno FROM orders WHERE ono=?", String.class, ono)).isEqualTo("T0008");
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("待清理");
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0008'", String.class)).isEqualTo("使用中");

    pay(ono, staff, "DEMO_CASH").andExpect(status().isOk());
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content(requestPrefix + "\"partySize\":9,\"targetTno\":\"T0013\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.orderNumber").value("001"))
        .andExpect(jsonPath("$.oldTno").value("T0008")).andExpect(jsonPath("$.newTno").value("T0013"))
        .andExpect(jsonPath("$.targetSeats").value(12)).andExpect(jsonPath("$.partySize").value(9));
    assertThat(jdbc.queryForObject("SELECT tno FROM orders WHERE ono=?", String.class, ono)).isEqualTo("T0013");
    assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE ono=?", String.class, ono)).isEqualTo("PAID");
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0008'", String.class)).isEqualTo("待清理");
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0013'", String.class)).isEqualTo("使用中");

    MockHttpSession admin = login("admin", "A0001");
    mvc.perform(post("/api/orders/" + ono + "/end").session(admin)).andExpect(status().isOk())
        .andExpect(jsonPath("$.orderStatus").value("ENDED")).andExpect(jsonPath("$.paymentStatus").value("PAID"));
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content(requestPrefix + "\"partySize\":13,\"targetTno\":\"T0002\"}"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT tno FROM orders WHERE ono=?", String.class, ono)).isEqualTo("T0013");
    mvc.perform(delete("/api/orders/" + ono).session(admin)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE ono=?", Integer.class, ono)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment WHERE ono=?", Integer.class, ono)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_detail WHERE ono=?", Integer.class, ono)).isZero();
    assertThat(jdbc.queryForObject("SELECT stock FROM menu WHERE mno='M0001'", Integer.class)).isEqualTo(3);
  }

  @Test void phoneReservationCollectsSeatBasedDepositAndRefundsItAfterBillPayment() throws Exception {
    String reservedAt = java.time.LocalDateTime.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).plusDays(1).withNano(0).toString();
    String reservationJson = mvc.perform(post("/api/reservations").session(staff).contentType("application/json")
        .content("{\"customerName\":\"Alice\",\"customerPhone\":\"13800000001\",\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + reservedAt + "\",\"depositMethod\":\"DEMO_CASH\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.depositAmount").value(20))
        .andExpect(jsonPath("$.reservationSource").value("PHONE"))
        .andExpect(jsonPath("$.depositStatus").value("PAID")).andExpect(jsonPath("$.status").value("RESERVED"))
        .andExpect(jsonPath("$.cno").value("C0001")).andReturn().getResponse().getContentAsString();
    String reservationNo = json.readTree(reservationJson).get("reservationNo").asText();
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("空闲");
    mvc.perform(put("/api/tables/T0002").session(staff).contentType("application/json")
        .content("{\"tno\":\"T0002\",\"seats\":4,\"tstatus\":\"已预订\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/reservations").session(customer)).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].reservationNo").value(reservationNo));

    String orderJson = mvc.perform(post("/api/orders/checkout").session(staff).contentType("application/json")
        .content("{\"cno\":\"C0001\",\"tno\":\"T0001\",\"reservationNo\":\"" + reservationNo + "\",\"items\":[{\"mno\":\"M0001\",\"dishCount\":1}]}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String ono = json.readTree(orderJson).get("ono").asText();
    assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("SEATED");
    assertThat(jdbc.queryForObject("SELECT ono FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo(ono);
    String orderDate = jdbc.queryForObject("SELECT CAST(order_date AS VARCHAR) FROM orders WHERE ono=?", String.class, ono);
    mvc.perform(post("/api/orders/transfer").session(staff).contentType("application/json")
        .content("{\"orderDate\":\"" + orderDate + "\",\"orderNumber\":\"001\",\"partySize\":5,\"targetTno\":\"T0008\"}"))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT tno FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("T0008");
    pay(ono, staff, "DEMO_CASH").andExpect(status().isOk())
        .andExpect(jsonPath("$.depositRefund.amount").value(20))
        .andExpect(jsonPath("$.depositRefund.status").value("REFUNDED"));
    assertThat(jdbc.queryForObject("SELECT deposit_status FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("REFUNDED");
    assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("COMPLETED");

    String second = mvc.perform(post("/api/reservations").session(staff).contentType("application/json")
        .content("{\"customerName\":\"Bob\",\"customerPhone\":\"13800000002\",\"tno\":\"T0002\",\"partySize\":4,\"reservedAt\":\"" + reservedAt + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.depositAmount").value(30))
        .andReturn().getResponse().getContentAsString();
    String secondNo = json.readTree(second).get("reservationNo").asText();
    mvc.perform(post("/api/reservations/" + secondNo + "/cancel").session(staff))
        .andExpect(status().isOk()).andExpect(jsonPath("$.depositStatus").value("REFUNDED"))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0002'", String.class)).isEqualTo("空闲");
  }

  @Test void customerReservesAnAvailableTimeSlotAndNoShowForfeitsDeposit() throws Exception {
    String reservedAt = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).plusDays(1).atTime(11, 0).toString();
    mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"customerName\":\"Fake\",\"customerPhone\":\"000000\",\"tno\":\"T0013\",\"partySize\":12,\"reservedAt\":\"" + reservedAt + "\",\"depositMethod\":\"DEMO_CASH\"}"))
        .andExpect(status().isBadRequest());
    String response = mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"customerName\":\"Fake\",\"customerPhone\":\"000000\",\"tno\":\"T0013\",\"partySize\":12,\"reservedAt\":\"" + reservedAt + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.customerName").value("Alice"))
        .andExpect(jsonPath("$.customerPhone").value("13800000001")).andExpect(jsonPath("$.cno").value("C0001"))
        .andExpect(jsonPath("$.reservationSource").value("ONLINE"))
        .andExpect(jsonPath("$.depositAmount").value(70)).andExpect(jsonPath("$.depositStatus").value("PAID"))
        .andExpect(jsonPath("$.reservedUntil").value(reservedAt.substring(0, 11) + "13:00:00"))
        .andExpect(jsonPath("$.arrivalDeadline").value(reservedAt.substring(0, 11) + "11:30:00"))
        .andReturn().getResponse().getContentAsString();
    String reservationNo = json.readTree(response).get("reservationNo").asText();
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0013'", String.class)).isEqualTo("空闲");
    mvc.perform(get("/api/reservations").session(staff)).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reservationSource").value("ONLINE"))
        .andExpect(jsonPath("$[0].tno").value("T0013"))
        .andExpect(jsonPath("$[0].reservedAt").value(reservedAt + ":00"));

    MockHttpSession other = login("customer", "C0002");
    mvc.perform(get("/api/reservations").session(other)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    jdbc.update("UPDATE reservation SET arrival_deadline=? WHERE reservation_no=?", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(1)), reservationNo);
    mvc.perform(get("/api/tables").session(customer)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("NO_SHOW");
    assertThat(jdbc.queryForObject("SELECT deposit_status FROM reservation WHERE reservation_no=?", String.class, reservationNo)).isEqualTo("FORFEITED");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE reservation_no=? AND deposit_forfeited_at IS NOT NULL", Integer.class, reservationNo)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0013'", String.class)).isEqualTo("空闲");
  }

  @Test void sameTableCanBeBookedOnDifferentDatesAndNonOverlappingSlots() throws Exception {
    java.time.LocalDate day = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).plusDays(2);
    String morning = day.atTime(11, 0).toString();
    String afternoon = day.atTime(13, 30).toString();
    String nextDay = day.plusDays(1).atTime(11, 0).toString();
    String firstJson = mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + morning + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String firstNo = json.readTree(firstJson).get("reservationNo").asText();

    String secondJson = mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + afternoon + "\",\"depositMethod\":\"DEMO_ALIPAY\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String secondNo = json.readTree(secondJson).get("reservationNo").asText();
    mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + morning + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + nextDay + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/reservations/availability").param("date", day.toString()).session(customer))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].tno").value("T0001")).andExpect(jsonPath("$[0].reservedAt").value(morning + ":00"))
        .andExpect(jsonPath("$[1].reservedAt").value(afternoon + ":00"));
    mvc.perform(post("/api/reservations/" + firstNo + "/cancel").session(staff)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("空闲");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE tno='T0001' AND status='RESERVED'", Integer.class)).isEqualTo(2);
    java.time.LocalDate today = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai")));
    jdbc.update("UPDATE reservation SET reserved_at=?,reserved_until=?,arrival_deadline=? WHERE tno='T0001' AND status='RESERVED' AND reservation_no<>?",
        java.sql.Timestamp.valueOf(today.atTime(12, 0)), java.sql.Timestamp.valueOf(today.atTime(14, 0)),
        java.sql.Timestamp.valueOf(today.plusDays(1).atTime(12, 30)), secondNo);
    mvc.perform(get("/api/tables").session(staff)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("已预订");
    jdbc.update("UPDATE reservation SET reserved_at=?,reserved_until=?,arrival_deadline=? WHERE tno='T0001' AND status='RESERVED' AND reservation_no<>?",
        java.sql.Timestamp.valueOf(day.plusDays(1).atTime(11, 0)), java.sql.Timestamp.valueOf(day.plusDays(1).atTime(13, 0)),
        java.sql.Timestamp.valueOf(day.plusDays(1).atTime(11, 30)), secondNo);
    mvc.perform(get("/api/tables").session(staff)).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("空闲");
    mvc.perform(post("/api/orders/checkout").session(staff).contentType("application/json")
        .content("{\"cno\":\"C0001\",\"tno\":\"T0001\",\"reservationNo\":\"" + secondNo + "\",\"items\":[{\"mno\":\"M0001\",\"dishCount\":1}]}"))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_no=?", String.class, secondNo)).isEqualTo("SEATED");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE tno='T0001' AND status='RESERVED'", Integer.class)).isEqualTo(1);
  }

  @Test void staffCancelsStartedNoShowWithoutRefundingDeposit() throws Exception {
    String reservedAt = java.time.LocalDate.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai"))).plusDays(1).atTime(11, 0).toString();
    String response = mvc.perform(post("/api/reservations").session(customer).contentType("application/json")
        .content("{\"tno\":\"T0001\",\"partySize\":2,\"reservedAt\":\"" + reservedAt + "\",\"depositMethod\":\"DEMO_WECHAT\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    String reservationNo = json.readTree(response).get("reservationNo").asText();

    mvc.perform(post("/api/reservations/" + reservationNo + "/no-show").session(staff))
        .andExpect(status().isBadRequest());
    jdbc.update("UPDATE reservation SET reserved_at=?, arrival_deadline=? WHERE reservation_no=?",
        java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(1)),
        java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusMinutes(29)), reservationNo);
    mvc.perform(post("/api/reservations/" + reservationNo + "/no-show").session(customer))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/reservations/" + reservationNo + "/no-show").session(staff))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_SHOW"))
        .andExpect(jsonPath("$.depositStatus").value("FORFEITED"))
        .andExpect(jsonPath("$.depositRefundedAt").doesNotExist())
        .andExpect(jsonPath("$.depositForfeitedAt").isNotEmpty());
    assertThat(jdbc.queryForObject("SELECT tstatus FROM dining_table WHERE tno='T0001'", String.class)).isEqualTo("空闲");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
  }
}
