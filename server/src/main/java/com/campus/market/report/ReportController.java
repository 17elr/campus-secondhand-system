package com.campus.market.report;

import com.campus.market.common.ApiResponse;
import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportMapper mapper;
    private final UserMapper userMapper;

    public ReportController(ReportMapper mapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ApiResponse<Report> create(@RequestBody Report report,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        User user = userMapper.findById(userId);
        if (user == null)
            throw new IllegalArgumentException("用户不存在");
        report.setReporterName(user.getNickname());
        return ApiResponse.ok(save(report));
    }

    private Report save(Report report) {
        if (report.getTargetId() == null || report.getReason() == null || report.getReason().isBlank())
            throw new IllegalArgumentException("举报信息不完整");
        mapper.insert(report);
        return report;
    }
}
