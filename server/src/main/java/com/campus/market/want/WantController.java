package com.campus.market.want;

import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import com.campus.market.common.ApiResponse;
import com.campus.market.common.PageQuery;
import com.campus.market.common.PageResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/wants")
public class WantController {
    private final WantMapper mapper;
    private final UserMapper userMapper;

    public WantController(WantMapper mapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<PageResult<Want>> list(@Valid PageQuery query,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.ok(new PageResult<>(mapper.page(query, userId), mapper.pageCount(query, userId),
                query.getPage(), query.getSize()));
    }

    @PostMapping
    public ApiResponse<Want> create(@Valid @RequestBody Want want, @RequestHeader("X-User-Id") Long userId) {
        User user = activeUser(userId);
        want.setUserId(userId);
        want.setRequesterName(user.getNickname());
        want.setTitle(want.getTitle().trim());
        want.setDescription(want.getDescription().trim());
        mapper.insert(want);
        return ApiResponse.ok(mapper.findById(want.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Want> edit(@PathVariable Long id, @Valid @RequestBody Want want,
            @RequestHeader("X-User-Id") Long userId) {
        activeUser(userId);
        own(id, userId);
        want.setId(id);
        want.setUserId(userId);
        want.setTitle(want.getTitle().trim());
        want.setDescription(want.getDescription().trim());
        if (mapper.updateOwn(want) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前求购状态不可编辑");
        return ApiResponse.ok(mapper.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Want> status(@PathVariable Long id, @RequestParam String value,
            @RequestHeader("X-User-Id") Long userId) {
        activeUser(userId);
        own(id, userId);
        if (!List.of("FOUND", "WITHDRAWN").contains(value))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的求购状态");
        if (mapper.updateOwnStatus(id, userId, value) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前求购状态不可操作");
        return ApiResponse.ok(mapper.findById(id));
    }

    private User activeUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号不可用");
        return user;
    }

    private void own(Long id, Long userId) {
        Want want = mapper.findById(id);
        if (want == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "求购不存在");
        if (!userId.equals(want.getUserId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能管理自己的求购");
    }
}
