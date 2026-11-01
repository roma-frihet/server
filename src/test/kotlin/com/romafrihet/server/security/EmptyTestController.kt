package com.romafrihet.server.security

import org.springframework.web.bind.annotation.RestController

/** 보안 필터만 검증하는 웹 슬라이스 테스트가 실제 컨트롤러와 서비스를 올리지 않도록 지정한다. */
@RestController
class EmptyTestController
