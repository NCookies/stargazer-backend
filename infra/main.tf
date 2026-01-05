# 1. 기본 VPC 데이터 가져오기 (복잡한 네트워크 설정 없이 기본 망 사용)
data "aws_vpc" "default" {
  default = true
}

# 2. 최신 우분투(Ubuntu 22.04) 이미지 정보 가져오기
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical (Ubuntu 공식)

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# 3. [보안그룹] EC2용 (SSH, 8080 포트 오픈)
resource "aws_security_group" "web_sg" {
  name        = "stargazer-web-sg"
  description = "Allow SSH and HTTP 8080"
  vpc_id      = data.aws_vpc.default.id

  # SSH 접속 허용
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 스프링부트(8080) 접속 허용
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 밖으로 나가는 건 모두 허용 (패키지 설치 등)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 4. [보안그룹] RDS용 (오직 EC2에서만 접속 가능하게 설정 - 보안 핵심!)
resource "aws_security_group" "db_sg" {
  name        = "stargazer-db-sg"
  description = "Allow access only from Web Server"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.web_sg.id] # 중요: IP가 아니라 보안그룹 ID로 허용
  }
}

# 5. [EC2] 웹 서버 인스턴스 (프리티어: t3.micro)
resource "aws_instance" "web_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t2.micro"
  key_name      = var.key_pair_name

  vpc_security_group_ids = [aws_security_group.web_sg.id]

  tags = {
    Name = "Stargazer-API-Server"
  }
}

# 6. [RDS] 데이터베이스 (프리티어: db.t3.micro)
resource "aws_db_instance" "database" {
  identifier           = "stargazer-db"
  allocated_storage    = 20 # 프리티어 최대 20GB
  storage_type         = "gp2"
  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = "db.t3.micro" # t2.micro는 구형이라 t3.micro 추천 (프리티어 포함)

  username             = var.db_username
  password             = var.db_password
  parameter_group_name = "default.mysql8.0"

  skip_final_snapshot  = true  # 실습용이라 삭제 시 스냅샷 생성 안함 (빠른 삭제)
  publicly_accessible  = false # 외부에서 직접 접속 불가 (보안)

  vpc_security_group_ids = [aws_security_group.db_sg.id]

  tags = {
    Name = "Stargazer-Database"
  }
}