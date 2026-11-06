package com.bankops.identity;

/** Read port other modules may use; keeps JPA types inside the identity module. */
public interface IdentityQuery {

    long countUsers();

    long countDepartments();
}
