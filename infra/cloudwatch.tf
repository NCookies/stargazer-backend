# -----------------------------------------------------------------------------
# CloudWatch Log Groups (앱 로그, nginx) + SNS Topic (알람용)
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "app" {
  name              = "/stargazer/app"
  retention_in_days  = 14
  tags              = { Name = "stargazer-app-logs" }
}

resource "aws_cloudwatch_log_group" "nginx" {
  name              = "/stargazer/nginx"
  retention_in_days  = 14
  tags              = { Name = "stargazer-nginx-logs" }
}

resource "aws_sns_topic" "alerts" {
  name = "stargazer-alerts"
  tags = { Name = "stargazer-alerts" }
}

# -----------------------------------------------------------------------------
# CloudWatch Alarms (알람 시 SNS → Lambda → Discord/Slack Webhook)
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name          = "stargazer-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "EC2 CPU 사용률 80% 초과"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions         = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.web_server.id
  }
}

resource "aws_cloudwatch_metric_alarm" "status_check_failed" {
  alarm_name          = "stargazer-status-check-failed"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 1
  alarm_description   = "EC2 인스턴스 상태 검사 실패 (시스템/인스턴스 장애)"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions         = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.web_server.id
  }
}

# 메모리 알람: CloudWatch Agent 설치 후 CWAgent 메트릭이 수집되면 동작
resource "aws_cloudwatch_metric_alarm" "memory_high" {
  alarm_name          = "stargazer-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "CWAgent"
  period              = 60
  statistic           = "Average"
  threshold           = 85
  alarm_description   = "EC2 메모리 사용률 85% 초과 (CloudWatch Agent 필요)"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions         = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"

  dimensions = {
    InstanceId = aws_instance.web_server.id
  }
}
