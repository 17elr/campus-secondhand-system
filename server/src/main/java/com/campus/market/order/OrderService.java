package com.campus.market.order;

import com.campus.market.goods.Goods;
import com.campus.market.goods.GoodsMapper;
import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;

@Service
public class OrderService {
    private final OrderMapper mapper;
    private final GoodsMapper goodsMapper;
    private final UserMapper userMapper;

    public OrderService(OrderMapper mapper, GoodsMapper goodsMapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.goodsMapper = goodsMapper;
        this.userMapper = userMapper;
    }

    public TradeOrder create(Long goodsId, Long userId) {
        String buyerName = user(userId).getNickname();
        Goods goods = goodsMapper.findById(goodsId, userId);
        if (goods == null || !"ON_SALE".equals(goods.getStatus()))
            throw new IllegalStateException("商品已不可购买");
        if (buyerName.equals(goods.getSellerName()))
            throw new IllegalStateException("不能购买自己的商品");
        TradeOrder order = new TradeOrder();
        order.setGoodsId(goodsId);
        order.setTitle(goods.getTitle());
        order.setPrice(goods.getPrice());
        order.setBuyerName(buyerName);
        order.setSellerName(goods.getSellerName());
        order.setLocation(goods.getLocation());
        mapper.insert(order);
        goodsMapper.reserve(goodsId);
        order.setStatus("PENDING");
        return order;
    }

    public List<TradeOrder> mine(Long userId) {
        return mapper.mine(user(userId).getNickname());
    }

    public PageResult<TradeOrder> page(PageQuery query, Long userId) {
        String buyer = user(userId).getNickname();
        return new PageResult<>(mapper.page(query, buyer), mapper.pageCount(query, buyer), query.getPage(),
                query.getSize());
    }

    public TradeOrder cancel(Long id, Long userId) {
        String buyerName = user(userId).getNickname();
        mapper.updateStatus(id, buyerName, "CANCELLED");
        TradeOrder order = mine(userId).stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
        if (order != null)
            goodsMapper.release(order.getGoodsId());
        return order;
    }

    private User user(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null)
            throw new IllegalArgumentException("用户不存在");
        return user;
    }
}
