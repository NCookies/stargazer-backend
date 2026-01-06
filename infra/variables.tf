variable "db_username" {
  description = "RDS 데이터베이스 사용자 이름"
  type        = string
  default     = "admin"
}

variable "db_password" {
  description = "RDS 데이터베이스 비밀번호"
  type        = string
  sensitive   = true # 로그에 안 찍히게 설정
}

variable "key_pair_name" {
  description = "EC2 접속에 사용할 키페어 이름 (AWS 콘솔에서 미리 생성 필요)"
  type        = string
}