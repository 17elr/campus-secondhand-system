package com.campus.market.goods;

import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;

@Service
public class GoodsService {
    private final GoodsMapper mapper;
    private final UserMapper userMapper;

    public GoodsService(GoodsMapper mapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    public List<Goods> list(String keyword, String category, Long userId) {
        return mapper.findAll(keyword, category, userId);
    }

    public Goods get(Long id, Long userId) {
        return mapper.findById(id, userId);
    }

    public List<Goods> mine(Long userId) {
        return mapper.findMine(userId);
    }

    public List<Goods> favorites(Long userId) {
        return mapper.findFavorites(userId);
    }

    public PageResult<Goods> page(PageQuery query, Long userId, String scope) {
        return new PageResult<>(mapper.page(query, userId, scope), mapper.pageCount(query, userId, scope),
                query.getPage(), query.getSize());
    }

    public Goods create(Goods goods, Long userId) {
        goods.setSellerName(user(userId).getNickname());
        mapper.insert(goods);
        return goods;
    }

    public Goods update(Goods goods, Long userId) {
        goods.setSellerName(user(userId).getNickname());
        mapper.update(goods);
        return mapper.findById(goods.getId(), userId);
    }

    public Goods updateStatus(Long id, Long userId, String status) {
        mapper.updateStatus(id, user(userId).getNickname(), status);
        return mapper.findById(id, userId);
    }

    public Goods toggleFavorite(Long id, Long userId) {
        if (mapper.favoriteExists(id, userId) > 0) {
            mapper.removeFavorite(id, userId);
            mapper.decreaseFavorite(id);
        } else {
            mapper.addFavorite(id, userId);
            mapper.increaseFavorite(id);
        }
        return mapper.findById(id, userId);
    }

    private User user(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null)
            throw new IllegalArgumentException("用户不存在");
        return user;
    }
}
