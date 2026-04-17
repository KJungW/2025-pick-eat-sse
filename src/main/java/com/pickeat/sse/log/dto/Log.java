package com.pickeat.sse.log.dto;

import java.util.Map;

public interface Log {

    Map<String, Object> fields();

    String summary();
}
