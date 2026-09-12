use resjk;

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
