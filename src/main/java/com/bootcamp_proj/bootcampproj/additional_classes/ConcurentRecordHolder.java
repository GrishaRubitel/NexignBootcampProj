package com.bootcamp_proj.bootcampproj.additional_classes;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс с хранилищем записей из CDR файла с конкурентым доступом. Нужен для корректной работы с параллельными
 * потоками (избавляемся от гонки)
 */
public class ConcurentRecordHolder {
    private LinkedList<String> recordHolder;
    private Lock lock;

    public ConcurentRecordHolder() {
        recordHolder = new LinkedList<>();
        lock = new ReentrantLock();
    }

    public void add(String item) {
        lock.lock();
        try {
            recordHolder.add(item);
        } finally {
            lock.unlock();
        }
    }

    public void remove(String item) {
        lock.lock();
        try {
            recordHolder.remove(item);
        } finally {
            lock.unlock();
        }
    }

    public String get(int index) {
        lock.lock();
        try {
            if (index >= 0 && index < recordHolder.size()) {
                return recordHolder.get(index);
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            recordHolder.clear();
        } finally {
            lock.unlock();
        }
    }

    public LinkedList<String> getRecordHolder() {
        return recordHolder;
    }

    public int getListLength() {
        return recordHolder.size();
    }
}
