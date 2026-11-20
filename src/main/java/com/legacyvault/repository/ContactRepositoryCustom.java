package com.legacyvault.repository;

import com.legacyvault.entity.Contact;

import java.util.List;

public interface ContactRepositoryCustom {
    List<Contact> searchContacts(String ownerId, String keyword);
}
