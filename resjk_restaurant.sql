create database resjk;
use resjk;

create table employee
  (eno char(9) primary key,
   ename char(20),
   eage smallint,
   esex char(2),
   epassword char(20)
  );

create table customer
  (cno char(9) primary key,
   cname char(20),
   csex char(2),
   cphone char(20),
   cpassword char(20)
  );

create table dining_table
  (tno char(9) primary key,
   seats smallint,
   tstatus char(20)
  );

create table menu
  (mno char(9) primary key,
   mname char(30),
   mtype char(20),
   mprice decimal(8,2),
   stock int default 50,
   image_url varchar(500)
  );

create table administrator
  (ano char(9) primary key,
   aname char(20),
   apassword char(20)
  );

create table orders
  (ono char(9) primary key,
   total_amount decimal(8,2),
   order_time datetime,
   eno char(9),
   cno char(9),
   tno char(9),
   order_date date,
   daily_number int,
   payment_status varchar(20) not null default 'UNPAID',
   order_status varchar(20) not null default 'ACTIVE',
   ended_at datetime,
   unique key uk_orders_date_number(order_date, daily_number),
   foreign key(eno) references employee(eno),
   foreign key(cno) references customer(cno),
   foreign key(tno) references dining_table(tno)
  );

create table order_sequence
  (order_date date primary key,
   last_number int not null
  );

create table payment
  (payment_no varchar(40) primary key,
   ono char(9) not null unique,
   amount decimal(8,2) not null,
   method varchar(30) not null,
   status varchar(30) not null,
   paid_at datetime not null,
   foreign key(ono) references orders(ono)
  );

create table reservation
  (reservation_no varchar(40) primary key,
   customer_name varchar(30) not null,
   customer_phone varchar(20) not null,
   cno char(9),
   tno char(9) not null,
   eno char(9) not null,
    party_size smallint not null,
    reservation_source varchar(20) not null,
    reserved_at datetime not null,
    reserved_until datetime not null,
    arrival_deadline datetime not null,
    deposit_amount decimal(8,2) not null,
   deposit_method varchar(30) not null,
   deposit_status varchar(30) not null,
    deposit_paid_at datetime not null,
    deposit_refunded_at datetime,
    deposit_forfeited_at datetime,
   status varchar(30) not null,
   ono char(9) unique,
   created_at datetime not null,
   foreign key(cno) references customer(cno),
   foreign key(tno) references dining_table(tno),
   foreign key(eno) references employee(eno),
   foreign key(ono) references orders(ono),
   index idx_reservation_table_status(tno,status),
    index idx_reservation_phone(customer_phone),
    index idx_reservation_time(reserved_at),
    index idx_reservation_arrival_deadline(status,arrival_deadline)
  );

create table order_detail
  (dno char(9) primary key,
   ono char(9),
   mno char(9),
   dish_count smallint,
   amount decimal(8,2),
   remark char(100),
   foreign key(ono) references orders(ono),
   foreign key(mno) references menu(mno)
  );

insert into administrator values('A0001','管理员','123456');

insert into employee values('E0001','张三',28,'男','123456');
insert into employee values('E0002','李四',25,'女','123456');
insert into employee values('E0003','王五',31,'男','123456');

insert into customer values('C0001','赵明','男','13800000001','123456');
insert into customer values('C0002','钱丽','女','13800000002','123456');
insert into customer values('C0003','孙强','男','13800000003','123456');
insert into customer values('C0004','周芳','女','13800000004','123456');

insert into dining_table values('T0001',2,'空闲');
insert into dining_table values('T0002',2,'使用中');
insert into dining_table values('T0003',4,'空闲');
insert into dining_table values('T0004',4,'使用中');
insert into dining_table values('T0005',6,'空闲');
insert into dining_table values('T0006',6,'待清理');
insert into dining_table values('T0007',8,'空闲');
insert into dining_table values('T0008',8,'空闲');
insert into dining_table values('T0009',2,'空闲');
insert into dining_table values('T0010',4,'空闲');
insert into dining_table values('T0011',6,'空闲');
insert into dining_table values('T0012',8,'空闲');
insert into dining_table values('T0013',12,'空闲');
insert into dining_table values('T0014',12,'空闲');
insert into dining_table values('T0015',12,'空闲');

insert into menu (mno,mname,mtype,mprice,stock,image_url) values
('M0001','招牌牛排','主菜',88.00,50,'/images/menu/M0001.png'),
('M0002','蒜香虾仁意面','主食',68.00,50,'/images/menu/M0002.png'),
('M0003','香煎三文鱼','主菜',92.00,50,'/images/menu/M0003.png'),
('M0004','法式蘑菇汤','汤品',38.00,50,'/images/menu/M0004.png'),
('M0005','凯撒沙拉','凉菜',32.00,50,'/images/menu/M0005.png'),
('M0006','柠檬红茶','饮品',18.00,50,'/images/menu/M0006.png'),
('M0007','黑椒烤鸡','主菜',58.00,50,'/images/menu/M0007.png'),
('M0008','玛格丽特披萨','主食',56.00,40,'/images/menu/M0008.png'),
('M0009','番茄海鲜烩饭','主食',72.00,40,'/images/menu/M0009.png'),
('M0010','南瓜浓汤','汤品',28.00,50,'/images/menu/M0010.png'),
('M0011','草莓芝士蛋糕','甜品',36.00,30,'/images/menu/M0011.png'),
('M0012','芒果酸奶','饮品',24.00,50,'/images/menu/M0012.png'),
('M0013','蜜汁烤鸭胸','主菜',78.00,50,'/images/menu/M0013.png'),
('M0014','香草羊排','主菜',98.00,50,'/images/menu/M0014.png'),
('M0015','松露奶油宽面','主食',76.00,40,'/images/menu/M0015.png'),
('M0016','日式照烧鳗鱼饭','主食',82.00,40,'/images/menu/M0016.png'),
('M0017','泰式冬阴功汤','汤品',42.00,50,'/images/menu/M0017.png'),
('M0018','玉米奶油浓汤','汤品',30.00,50,'/images/menu/M0018.png'),
('M0019','牛油果鲜虾沙拉','凉菜',46.00,40,'/images/menu/M0019.png'),
('M0020','脆皮春卷','小吃',32.00,50,'/images/menu/M0020.png'),
('M0021','提拉米苏','甜品',38.00,30,'/images/menu/M0021.png'),
('M0022','熔岩巧克力蛋糕','甜品',42.00,30,'/images/menu/M0022.png'),
('M0023','百香果气泡水','饮品',22.00,50,'/images/menu/M0023.png'),
('M0024','抹茶拿铁','饮品',26.00,50,'/images/menu/M0024.png');

insert into orders (ono,total_amount,order_time,eno,cno,tno) values('O0001',224.00,'2026-06-17 12:20:00','E0001','C0001','T0002');
insert into orders (ono,total_amount,order_time,eno,cno,tno) values('O0002',218.00,'2026-06-17 18:35:00','E0002','C0002','T0004');

insert into order_detail values('D0001','O0001','M0001',1,88.00,'七分熟');
insert into order_detail values('D0002','O0001','M0002',1,68.00,'少辣');
insert into order_detail values('D0003','O0001','M0006',2,36.00,'正常冰');
insert into order_detail values('D0004','O0001','M0005',1,32.00,'少酱');
insert into order_detail values('D0005','O0002','M0003',1,92.00,'正常');
insert into order_detail values('D0006','O0002','M0004',2,76.00,'餐前上');
insert into order_detail values('D0007','O0002','M0006',1,18.00,'去冰');
insert into order_detail values('D0008','O0002','M0005',1,32.00,'分装');

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

create index idx_employee_ename on employee(ename);
create index idx_customer_cname on customer(cname);
create index idx_customer_cphone on customer(cphone);
create index idx_menu_mname on menu(mname);
create index idx_menu_mtype on menu(mtype);
create index idx_orders_eno on orders(eno);
create index idx_orders_cno on orders(cno);
create index idx_orders_tno on orders(tno);
create index idx_orders_time on orders(order_time);
create unique index uk_order_detail_ono_mno on order_detail(ono, mno);
create index idx_order_detail_mno on order_detail(mno);
