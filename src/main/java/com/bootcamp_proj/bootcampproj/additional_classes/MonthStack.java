package com.bootcamp_proj.bootcampproj.additional_classes;

import java.util.Stack;

/**
 * Класс содержит стек, в котором хранятся временные метки первых секунд каждого месяца 2023-го года
 */
public class MonthStack {
    private Stack<Integer> monthStack = new Stack<>();

    public MonthStack() {}

    public boolean checkTop(int value) {
        if (monthStack.peek() <= value) {
            monthStack.pop();
            return true;
        }
        return false;
    }

    public void push(int value) {
        monthStack.push(value);
    }

    public int peek() {
        return monthStack.peek();
    }
}
