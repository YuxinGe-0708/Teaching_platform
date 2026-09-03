def test_microservices_health_and_versions(api, record_case):
    user_health = api.get("user", "/actuator/health")
    learning_health = api.get("learning", "/actuator/health")
    assessment_health = api.get("assessment", "/actuator/health")

    assert user_health.get("status") == "UP"
    assert learning_health.get("status") == "UP"
    assert assessment_health.get("status") == "UP"

    record_case(
        "E2E-ENV-001",
        "微服务健康检查验证",
        "测试环境基线",
        "passed",
        {
            "user": user_health,
            "learning": learning_health,
            "assessment": assessment_health,
        },
    )
