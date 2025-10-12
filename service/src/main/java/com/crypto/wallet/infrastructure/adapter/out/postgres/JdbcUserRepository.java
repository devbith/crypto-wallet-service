package com.crypto.wallet.infrastructure.adapter.out.postgres;

import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.WalletId;
import com.crypto.wallet.application.port.out.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUserRepository implements UserRepository {
    
    private final JdbcClient jdbcClient;
    
    public JdbcUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
    
    @Override
    @Transactional
    public void save(User user) {
        jdbcClient.sql("""
            INSERT INTO users (email, wallet_id, created_at) 
            VALUES (?, ?, ?)
            """)
            .param(user.email().value())
            .param(user.walletId().value())
            .param(user.createdAt())
            .update();
    }
    
    @Override
    @Transactional(readOnly = true)  
    public Optional<User> findByEmail(Email email) {
        return jdbcClient.sql("""
            SELECT email, wallet_id, created_at 
            FROM users 
            WHERE email = ?
            """)
            .param(email.value())
            .query(this::mapUser)
            .optional();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByWalletId(WalletId walletId) {
        return jdbcClient.sql("""
            SELECT email, wallet_id, created_at 
            FROM users 
            WHERE wallet_id = ?
            """)
            .param(walletId.value())
            .query(this::mapUser)
            .optional();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        Integer count = jdbcClient.sql("""
            SELECT COUNT(*) 
            FROM users 
            WHERE email = ?
            """)
            .param(email.value())
            .query(Integer.class)
            .single();
        
        return count > 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return jdbcClient.sql("""
            SELECT email, wallet_id, created_at 
            FROM users 
            ORDER BY created_at DESC
            """)
            .query(this::mapUser)
            .list();
    }
    
    private User mapUser(ResultSet rs, int rowNum) throws SQLException {
        return User.of(
            Email.of(rs.getString("email")),
            WalletId.of(rs.getString("wallet_id")),
            rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
