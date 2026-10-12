create table sys_user (
 id bigint auto_increment primary key,
 username varchar(40) not null unique,
 password_hash varchar(100) not null,
 nickname varchar(40) not null,
 role varchar(20) not null default 'USER',
 status int not null default 1,
 created_at timestamp not null
);
create table report (
 id bigint auto_increment primary key,
 reporter_name varchar(40) not null,
 target_type varchar(20) not null,
 target_id bigint not null,
 reason varchar(500) not null,
 status varchar(20) not null,
 created_at timestamp not null
);
create table want_post (
 id bigint auto_increment primary key,
 user_id bigint not null,
 requester_name varchar(40) not null,
 title varchar(120) not null,
 category varchar(40),
 budget decimal(10,2),
 description varchar(500),
 status varchar(20) not null,
 created_at timestamp not null
);
create table goods_message (
 id bigint auto_increment primary key,
 goods_id bigint not null,
 user_id bigint not null,
 user_name varchar(40) not null,
 content varchar(500) not null,
 reply_content varchar(500),
 reply_name varchar(40),
 status varchar(20) not null,
 created_at timestamp not null,
 replied_at timestamp
);
create table goods (
 id bigint auto_increment primary key,
 title varchar(120) not null,
 price decimal(10,2) not null,
 category varchar(40),
 condition_level varchar(30),
 location varchar(80),
 description varchar(1000),
 image_url varchar(500),
 seller_name varchar(40),
 status varchar(20) not null,
 favorite_count int not null default 0,
 created_at timestamp not null
);
create table favorite (user_id bigint not null, goods_id bigint not null, created_at timestamp not null, primary key(user_id, goods_id));
create table trade_order (
 id bigint auto_increment primary key,
 goods_id bigint not null,
 title varchar(120) not null,
 price decimal(10,2) not null,
 buyer_name varchar(40) not null,
 seller_name varchar(40) not null,
 location varchar(80),
 status varchar(20) not null,
 created_at timestamp not null
);
insert into goods(title,price,category,condition_level,location,description,image_url,seller_name,status,favorite_count,created_at) values
('九成新高等数学教材',28.00,'教材书籍','九成新','校本部 1 栋','无划线，适合大一高数复习。','/images/goods/001.webp','周同学','ON_SALE',12,CURRENT_TIMESTAMP),
('机械键盘 RGB 背光',89.00,'数码产品','八成新','生活区北门','声音清脆，配 USB 线，毕业清仓。','/images/goods/002.webp','陈同学','ON_SALE',8,CURRENT_TIMESTAMP),
('宿舍落地台灯',35.00,'生活用品','九成新','校本部 3 栋','三档调光，光线柔和，使用不到半年。','/images/goods/003.webp','周同学','ON_SALE',5,CURRENT_TIMESTAMP),
('自行车通勤车',168.00,'运动户外','七成新','东校门','骑行正常，适合校园通勤。','/images/goods/004.webp','王同学','ON_SALE',17,CURRENT_TIMESTAMP),
('宿舍收纳置物架',24.90,'生活用品','全新','校本部 5 栋','全新未拆，尺寸适合书桌和衣柜。','/images/goods/005.webp','阿杰','ON_SALE',4,CURRENT_TIMESTAMP),
('摄影三脚架',120.00,'数码产品','九成新','图书馆旁','稳定性好，含收纳包。','/images/goods/006.webp','小赵','ON_SALE',9,CURRENT_TIMESTAMP);

