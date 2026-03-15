"""
SNS 알람 메시지 또는 EventBridge 스케일 이벤트를 Discord/Slack Webhook으로 전송.
- SNS: CloudWatch Alarm 메시지 파싱 후 포맷
- EventBridge: EC2/ASG 이벤트 파싱 후 스케일 인/아웃 메시지 포맷
"""
import json
import os
import urllib.request

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
    except Exception as e:
        print(f"Webhook error: {e}")
        raise
    return {"statusCode": 200}
