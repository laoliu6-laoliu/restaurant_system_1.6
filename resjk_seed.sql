use resjk;

insert into administrator values('A0001','管理员','123456')
on duplicate key update aname=values(aname), apassword=values(apassword);

insert into employee values('E0001','张三',28,'男','123456')
on duplicate key update ename=values(ename), eage=values(eage), esex=values(esex), epassword=values(epassword);
insert into employee values('E0002','李四',25,'女','123456')
on duplicate key update ename=values(ename), eage=values(eage), esex=values(esex), epassword=values(epassword);
insert into employee values('E0003','王五',31,'男','123456')
on duplicate key update ename=values(ename), eage=values(eage), esex=values(esex), epassword=values(epassword);

insert into customer values('C0001','赵明','男','13800000001','123456')
on duplicate key update cname=values(cname), csex=values(csex), cphone=values(cphone), cpassword=values(cpassword);
insert into customer values('C0002','钱丽','女','13800000002','123456')
on duplicate key update cname=values(cname), csex=values(csex), cphone=values(cphone), cpassword=values(cpassword);
insert into customer values('C0003','孙强','男','13800000003','123456')
on duplicate key update cname=values(cname), csex=values(csex), cphone=values(cphone), cpassword=values(cpassword);
insert into customer values('C0004','周芳','女','13800000004','123456')
on duplicate key update cname=values(cname), csex=values(csex), cphone=values(cphone), cpassword=values(cpassword);

insert into dining_table values('T0001',2,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0002',2,'使用中')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0003',4,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0004',4,'使用中')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0005',6,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0006',6,'待清理')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0007',8,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0008',8,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0009',2,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0010',4,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0011',6,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0012',8,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0013',12,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0014',12,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);
insert into dining_table values('T0015',12,'空闲')
on duplicate key update seats=values(seats), tstatus=values(tstatus);

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
('M0024','抹茶拿铁','饮品',26.00,50,'/images/menu/M0024.png')
on duplicate key update mname=values(mname), mtype=values(mtype), mprice=values(mprice), stock=values(stock), image_url=values(image_url);

insert into orders (ono,total_amount,order_time,eno,cno,tno) values('O0001',224.00,'2026-06-17 12:20:00','E0001','C0001','T0002')
on duplicate key update total_amount=values(total_amount), order_time=values(order_time), eno=values(eno), cno=values(cno), tno=values(tno);
insert into orders (ono,total_amount,order_time,eno,cno,tno) values('O0002',218.00,'2026-06-17 18:35:00','E0002','C0002','T0004')
on duplicate key update total_amount=values(total_amount), order_time=values(order_time), eno=values(eno), cno=values(cno), tno=values(tno);

insert into order_detail values('D0001','O0001','M0001',1,88.00,'七分熟')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0002','O0001','M0002',1,68.00,'少辣')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0003','O0001','M0006',2,36.00,'正常冰')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0004','O0001','M0005',1,32.00,'少酱')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0005','O0002','M0003',1,92.00,'正常')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0006','O0002','M0004',2,76.00,'餐前上')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0007','O0002','M0006',1,18.00,'去冰')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
insert into order_detail values('D0008','O0002','M0005',1,32.00,'分装')
on duplicate key update ono=values(ono), mno=values(mno), dish_count=values(dish_count), amount=values(amount), remark=values(remark);
