package com.bankops.storage;
public interface ObjectStorage {
    void put(String key,byte[] data);
    byte[] get(String key);
    void delete(String key);
}
