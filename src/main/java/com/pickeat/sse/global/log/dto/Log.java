package com.pickeat.sse.global.log.dto;

import java.util.Map;

public interface Log {

    Map<String, Object> fields();

    String summary();
}
