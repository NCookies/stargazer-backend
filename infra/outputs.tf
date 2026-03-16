# EC2 인스턴스 정보
output "ec2_instance_id" {
  description = "EC2 인스턴스 ID"
  value       = aws_instance.web_server.id
}

output "ec2_public_ip" {
  description = "EC2 인스턴스 공용 IP"
  value       = aws_instance.web_server.public_ip
}

output "rds_endpoint" {
  description = "RDS 데이터베이스 엔드포인트"
  value       = aws_db_instance.database.endpoint
}

output "redis_endpoint" {
  description = "ElastiCache Redis 엔드포인트 (환경 변수 REDIS_ENDPOINT로 사용)"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}
