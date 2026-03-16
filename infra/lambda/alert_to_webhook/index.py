"""
SNS 알람 메시지 또는 EventBridge 스케일 이벤트를 Discord/Slack Webhook으로 전송.
- SNS: CloudWatch Alarm 메시지 파싱 후 포맷
- EventBridge: EC2/ASG 이벤트 파싱 후 스케일 인/아웃 메시지 포맷
"""
import json
import os
import socket
import urllib.request
from urllib.error import HTTPError, URLError

WEBHOOK_URL = os.environ.get("WEBHOOK_URL", "")


def post_webhook(body: dict) -> None:
    if not WEBHOOK_URL or not WEBHOOK_URL.startswith("http"):
        return
    req = urllib.request.Request(
        WEBHOOK_URL,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        if resp.status not in (200, 204):
            raise RuntimeError(f"Webhook returned {resp.status}")


def handle_sns(record: dict) -> dict:
    """CloudWatch Alarm 등 SNS 메시지를 Discord/Slack 포맷으로 변환."""
    msg = json.loads(record["Sns"]["Message"])
    alarm_name = msg.get("AlarmName", "Unknown")
    new_state = msg.get("NewStateValue", "UNKNOWN")
    reason = msg.get("NewStateReason", "")
    region = msg.get("Region", "")
    text = f"[{new_state}] **{alarm_name}**\n{reason}\n(Region: {region})"
    return {"content": text} if is_discord_style() else {"text": text}


def is_discord_style() -> bool:
    return "discord" in WEBHOOK_URL.lower()


def handle_eventbridge(detail: dict, detail_type: str = "") -> dict:
    """EventBridge EC2/ASG 이벤트를 스케일 알림 메시지로 변환."""
    instance_id = detail.get("EC2InstanceId", "")
    asg_name = detail.get("AutoScalingGroupName", "")
    if "Launch" in detail_type:
        action = "Scale-Out (인스턴스 기동)"
    elif "Terminate" in detail_type:
        action = "Scale-In (인스턴스 종료)"
    else:
        action = detail_type or "이벤트"
    text = f"**ASG 스케일 이벤트**\n{action}\nASG: {asg_name}\nInstanceId: {instance_id}"
    return {"content": text} if is_discord_style() else {"text": text}


def _is_retryable_error(exc: BaseException) -> bool:
    """재시도 가능(일시적) 오류면 True, 그 외(영구 오류)면 False."""
    if isinstance(exc, HTTPError):
        code = exc.code
        # 5xx, 429(rate limit) -> 재시도 허용
        if code >= 500 or code == 429:
            return True
        # 4xx(클라이언트/인증 등) -> 재시도 안 함
        return False
    if isinstance(exc, URLError):
        reason = exc.reason
        if isinstance(reason, socket.timeout):
            return True
        if isinstance(reason, OSError):
            errno = getattr(reason, "errno", None)
            if errno in (110, 111, 113):  # Connection timed out, refused, No route to host
                return True
        # 잘못된 URL, host not found 등 -> 재시도 안 함
        return False
    # 알 수 없는 예외는 무한 재시도 방지를 위해 재시도 안 함
    return False


def lambda_handler(event, context):
    try:
        if "Records" in event and event["Records"]:
            for record in event["Records"]:
                if "Sns" in record:
                    body = handle_sns(record)
                    post_webhook(body)
        elif "detail" in event:
            body = handle_eventbridge(event["detail"], event.get("detail-type", ""))
            post_webhook(body)
        else:
            post_webhook({"text": f"Unknown event: {json.dumps(event)[:500]}"})
    except (HTTPError, URLError) as e:
        retryable = _is_retryable_error(e)
        details = {"exception": type(e).__name__, "repr": repr(e)}
        if isinstance(e, HTTPError):
            details["status_code"] = e.code
            details["reason"] = str(getattr(e, "reason", ""))
        else:
            details["reason"] = str(getattr(e, "reason", ""))
        print(f"Webhook error (retryable={retryable}): {json.dumps(details)}")
        if retryable:
            raise
        return {"statusCode": 200}
    except Exception as e:
        print(f"Webhook unexpected error (non-retryable): {type(e).__name__}: {e}")
        return {"statusCode": 200}
    return {"statusCode": 200}
