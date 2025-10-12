package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm account theo username với eager loading roles
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        // Chuyển Account thành UserDetails
        return new CustomAccountDetails(account);
    }
}
