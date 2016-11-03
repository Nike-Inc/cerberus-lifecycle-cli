package com.nike.cerberus.service;

import java.util.List;
import java.util.Optional;

/**
 * Interface for common operations on storage services.
 */
public interface StoreService {

    void put(String path, String value);

    Optional<String> get(String path);

    List listKeysInPartialPath(String path);

    void deleteAllKeysOnPartialPath(String path);
}
