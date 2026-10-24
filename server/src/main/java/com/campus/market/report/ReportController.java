package com.campus.market.report;

import com.campus.market.auth.User;
import com.campus.market.auth.UserMapper;
import com.campus.market.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

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
  public ApiResponse<Report> create(
      @RequestBody Report report,
      @RequestAttribute(value = "currentUser", required = false) User currentUser) {
    Long userId = currentUser == null ? 0L : currentUser.getId();
    User user = userMapper.findById(userId);
    if (user == null) throw new IllegalArgumentException("用户不存在");
    report.setId(null);
    report.setReporterId(userId);
    report.setReporterName(user.getNickname());
    return ApiResponse.ok(save(report));
  }

  private Report save(Report report) {
    if (report.getTargetId() == null
        || report.getTargetId() < 1
        || report.getReason() == null
        || report.getReason().isBlank()
        || report.getReason().length() > 500
        || !java.util.Set.of("GOODS", "USER", "WANT")
            .contains(java.util.Objects.toString(report.getTargetType(), "")))
      throw new IllegalArgumentException("举报信息不完整");
    mapper.insert(report);
    return report;
  }
}
