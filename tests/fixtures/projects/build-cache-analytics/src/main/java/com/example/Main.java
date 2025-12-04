package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Build Cache Analytics Demo");
        DataProcessor processor = new DataProcessor();
        System.out.println("Processed: " + processor.process("test input"));
    }
}
