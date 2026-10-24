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
