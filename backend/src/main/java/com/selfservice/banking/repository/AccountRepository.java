package com.selfservice.banking.repository;

import com.selfservice.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerId(Long ownerId);
}
