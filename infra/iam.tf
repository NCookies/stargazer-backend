# -----------------------------------------------------------------------------
# Lambda 실행 역할 (SNS 호출 수신)
# -----------------------------------------------------------------------------
resource "aws_iam_role" "lambda_webhook" {
  name = "stargazer-lambda-webhook-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = { Service = "lambda.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_webhook.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# -----------------------------------------------------------------------------
# Phase 1c: EC2용 IAM 역할 (CloudWatch Agent 로그/메트릭 전송)
# -----------------------------------------------------------------------------
resource "aws_iam_role" "ec2_cloudwatch" {
  name = "stargazer-ec2-cloudwatch-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_cloudwatch_agent" {
  role       = aws_iam_role.ec2_cloudwatch.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2_cloudwatch" {
  name = "stargazer-ec2-cloudwatch-profile"
  role = aws_iam_role.ec2_cloudwatch.name
}
