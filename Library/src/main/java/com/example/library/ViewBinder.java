package com.example.library;

public interface ViewBinder<T> {
    //初始化控件
    void bind(T target);
}
