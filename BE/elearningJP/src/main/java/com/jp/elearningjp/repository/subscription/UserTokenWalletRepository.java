package com.jp.elearningjp.repository.subscription;

import com.jp.elearningjp.entity.subscription.UserTokenWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserTokenWalletRepository extends JpaRepository<UserTokenWallet, Long> {
}

