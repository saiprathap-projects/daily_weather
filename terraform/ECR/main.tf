provider "aws" {
  region = var.region
}
resource "aws_ecr_repository" "maven_artifacts" {
  name = var.repository_name
}
resource "aws_ecr_repository" "tomcat" {
  name = var.repository_name_1
}