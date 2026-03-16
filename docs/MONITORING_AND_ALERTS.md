# 모니터링 및 알림 가이드 (Phase 1)

Stargazer 인프라의 CloudWatch 모니터링, 알림(Discord/Slack), EC2 Agent 설정을 정리합니다.

---

## 1. CloudWatch 로그 그룹

- **`/stargazer/app`** — Spring Boot 앱 로그 (`/home/ubuntu/app-logs/application.log`). 배포 시 Docker 볼륨으로 해당 경로에 기록되며, Agent가 수집해 CloudWatch로 전송. **SSH 없이 콘솔/CLI/Grafana에서 확인 가능.**
- **`/stargazer/nginx`** — nginx 액세스/에러 로그

## 2. CloudWatch 알람 (알림 트리거)

다음 알람이 Terraform으로 정의되어 있으며, **알람 발생 시** SNS → Lambda → Webhook으로 알림이 전송됩니다.

| 알람 이름 | 조건 | 설명 |
|-----------|------|------|
| `stargazer-cpu-high` | CPU 사용률 > 80% (2회 연속, 1분 간격) | EC2 CPU 과부하 |
| `stargazer-status-check-failed` | 상태 검사 실패 ≥ 1 (2회 연속) | 인스턴스/시스템 장애 |
| `stargazer-memory-high` | 메모리 사용률 > 85% (2회 연속) | **CloudWatch Agent 설치 후** CWAgent 메트릭 수집 시에만 동작 |

알람이 **OK**로 복구될 때도 동일한 Webhook으로 복구 알림을 보냅니다.

## 3. SNS 알림 (Webhook)

- Terraform 변수 `alert_webhook_url`에 Discord 또는 Slack Webhook URL을 넣으면, 위 알람이 발생/복구될 때 Lambda를 통해 해당 URL로 전송됩니다.
- Webhook URL은 `terraform.tfvars`에 넣거나 `-var="alert_webhook_url=..."` 로 전달 (저장소에 커밋하지 말 것).
- 비우면 알람은 동작하지만 Webhook 전송은 하지 않습니다.

## 4. EC2에 CloudWatch Agent 설치

EC2에서 로그/메트릭을 CloudWatch로 보내려면 Agent 설치가 필요합니다.

1. EC2에 SSH 접속.
2. Agent 설치 (Ubuntu 예시):

```bash
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb
```

3. 프로젝트 `infra/cloudwatch-agent-config.json` 내용을 EC2에 복사한 뒤:

```bash
sudo cp cloudwatch-agent-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

설정 파일에는 앱 로그(`/stargazer/app`), nginx 로그(`/stargazer/nginx`) 수집이 이미 포함되어 있습니다. **이미 Agent를 설치한 경우** 프로젝트의 `infra/cloudwatch-agent-config.json`을 EC2에 다시 복사한 뒤 위 `amazon-cloudwatch-agent-ctl` 명령으로 설정을 반영하면 앱 로그가 수집됩니다.

---

## 5. 앱 로그 확인 (SSH 없이)

Spring Boot 로그는 CloudWatch 로그 그룹 **`/stargazer/app`** 에 수집됩니다.

- **AWS 콘솔**: CloudWatch → 로그 → 로그 그룹 → `/stargazer/app` → 로그 스트림 선택.
- **CLI 실시간**: `aws logs tail /stargazer/app --follow --region ap-northeast-2`
- **Grafana**: CloudWatch Logs 데이터 소스 연결 후 Explore에서 `/stargazer/app` 조회.
