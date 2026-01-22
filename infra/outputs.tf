# EC2 인스턴스 정보
output "ec2_instance_id" {
  description = "EC2 인스턴스 ID"
  value       = aws_instance.web_server.id
}

output "ec2_public_ip" {
  description = "EC2 인스턴스 공용 IP"
  value       = aws_instance.web_server.public_ip
}

# RDS 엔드포인트
output "rds_endpoint" {
  description = "RDS 데이터베이스 엔드포인트"
  value       = aws_db_instance.database.endpoint
}

# ElastiCache Redis 엔드포인트
# 단일 노드 모드에서는 primary_endpoint_address 사용 (포트 포함하지 않음)
output "redis_endpoint" {
  description = "ElastiCache Redis 엔드포인트 (환경 변수 REDIS_ENDPOINT로 사용)"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}
