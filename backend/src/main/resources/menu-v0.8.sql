-- Re-runnable: add the v0.8 dishes without replacing existing menu edits.
INSERT INTO menu (mno,mname,mtype,mprice,stock,image_url) VALUES
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
ON DUPLICATE KEY UPDATE mno = menu.mno;

UPDATE menu SET image_url=CONCAT('/images/menu/',mno,'.png')
WHERE mno BETWEEN 'M0013' AND 'M0024' AND (image_url IS NULL OR image_url='');
