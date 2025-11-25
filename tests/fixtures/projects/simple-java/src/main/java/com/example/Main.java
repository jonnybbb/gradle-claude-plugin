package com.example;

import com.google.common.base.Strings;

public class Main {
    public static void main(String[] args) {
        String message = Strings.repeat("Hello ", 3);
        System.out.println(message + "World!");
    }
}
