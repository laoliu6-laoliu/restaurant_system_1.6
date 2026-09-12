-- Re-runnable: preserve existing prices, stock, names and custom image URLs.
UPDATE menu SET image_url = '/images/menu/M0001.png' WHERE mno = 'M0001' AND mname = '招牌牛排' AND (image_url IS NULL OR image_url = '');
UPDATE menu SET image_url = '/images/menu/M0002.png' WHERE mno = 'M0002' AND mname = '蒜香虾仁意面' AND (image_url IS NULL OR image_url = '');
UPDATE menu SET image_url = '/images/menu/M0003.png' WHERE mno = 'M0003' AND mname = '香煎三文鱼' AND (image_url IS NULL OR image_url = '');
UPDATE menu SET image_url = '/images/menu/M0004.png' WHERE mno = 'M0004' AND mname = '法式蘑菇汤' AND (image_url IS NULL OR image_url = '');
UPDATE menu SET image_url = '/images/menu/M0005.png' WHERE mno = 'M0005' AND mname = '凯撒沙拉' AND (image_url IS NULL OR image_url = '');
UPDATE menu SET image_url = '/images/menu/M0006.png' WHERE mno = 'M0006' AND mname = '柠檬红茶' AND (image_url IS NULL OR image_url = '');
INSERT INTO menu (mno,mname,mtype,mprice,stock,image_url) VALUES
('M0007','黑椒烤鸡','主菜',58.00,50,'/images/menu/M0007.png'),
('M0008','玛格丽特披萨','主食',56.00,40,'/images/menu/M0008.png'),
('M0009','番茄海鲜烩饭','主食',72.00,40,'/images/menu/M0009.png'),
('M0010','南瓜浓汤','汤品',28.00,50,'/images/menu/M0010.png'),
('M0011','草莓芝士蛋糕','甜品',36.00,30,'/images/menu/M0011.png'),
('M0012','芒果酸奶','饮品',24.00,50,'/images/menu/M0012.png')
ON DUPLICATE KEY UPDATE mno = menu.mno;
