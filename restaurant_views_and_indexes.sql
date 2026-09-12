use resjk;

-- 视图 1：订单消费明细视图
create or replace view v_order_detail_info
  (ono, order_time, cno, cname, eno, ename, tno, mno, mname, mtype, dish_count, dish_price, amount, remark)
as
select o.ono,
       o.order_time,
       c.cno,
       c.cname,
       e.eno,
       e.ename,
       t.tno,
       m.mno,
       m.mname,
       m.mtype,
       d.dish_count,
       m.mprice,
       d.amount,
       d.remark
from orders o,
     customer c,
     employee e,
     dining_table t,
     order_detail d,
     menu m
where o.cno = c.cno
  and o.eno = e.eno
  and o.tno = t.tno
  and o.ono = d.ono
  and d.mno = m.mno;

-- 视图 2：菜品销售排行视图
create or replace view v_dish_sales_rank
  (mno, mname, mtype, total_count, total_amount)
as
select m.mno,
       m.mname,
       m.mtype,
       ifnull(sum(d.dish_count), 0),
       ifnull(sum(d.amount), 0)
from menu m
left join order_detail d on m.mno = d.mno
group by m.mno, m.mname, m.mtype;

-- 员工表索引
create index idx_employee_ename on employee(ename);

-- 顾客表索引
create index idx_customer_cname on customer(cname);
create index idx_customer_cphone on customer(cphone);

-- 菜品表索引
create index idx_menu_mname on menu(mname);
create index idx_menu_mtype on menu(mtype);

-- 订单表索引
create index idx_orders_eno on orders(eno);
create index idx_orders_cno on orders(cno);
create index idx_orders_tno on orders(tno);
create index idx_orders_time on orders(order_time);

-- 订单细则表索引
create unique index uk_order_detail_ono_mno on order_detail(ono, mno);
create index idx_order_detail_mno on order_detail(mno);
