package com.campus.market.report;

public class Report {
  private Long id;
  private Long reporterId;

  public Long getReporterId() {
    return reporterId;
  }

  public void setReporterId(Long v) {
    reporterId = v;
  }

  private String reporterName, targetType, reason, status;
  private Long targetId;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public String getReporterName() {
    return reporterName;
  }

  public void setReporterName(String v) {
    reporterName = v;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String v) {
    targetType = v;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long v) {
    targetId = v;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String v) {
    reason = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    status = v;
  }
}
