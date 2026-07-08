package com.demo.upimesh.repository;

import com.demo.upimesh.model.Account;
import org.Springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
