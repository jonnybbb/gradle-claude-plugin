package com.example.core;

import com.example.common.StringUtils;

public class Greeter {
    public String greet(String name) {
        return StringUtils.repeat("Hello ", 1) + name + "!";
    }
}
