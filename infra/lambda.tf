# -----------------------------------------------------------------------------
# Lambda (SNS -> Discord/Slack Webhook)
# -----------------------------------------------------------------------------
data "archive_file" "lambda_webhook" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/alert_to_webhook"
  output_path = "${path.module}/lambda/alert_to_webhook.zip"
}

resource "aws_lambda_function" "alert_to_webhook" {
  filename         = data.archive_file.lambda_webhook.output_path
  function_name    = "stargazer-alert-to-webhook"
  role             = aws_iam_role.lambda_webhook.arn
  handler          = "index.lambda_handler"
  source_code_hash = data.archive_file.lambda_webhook.output_base64sha256
  runtime          = "python3.12"
  timeout          = 30

  environment {
    variables = { WEBHOOK_URL = var.alert_webhook_url }
  }
}

resource "aws_lambda_permission" "sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.alert_to_webhook.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.alerts.arn
}

resource "aws_sns_topic_subscription" "alerts_to_lambda" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.alert_to_webhook.arn
}
