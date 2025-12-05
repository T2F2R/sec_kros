package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (Boolean.TRUE.equals(employee.getIsAdmin())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            }

            return new User(
                    employee.getEmail(),
                    employee.getPasswordHash(),
                    authorities
            );
        }

        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));

            return new User(
                    client.getEmail(),
                    client.getPasswordHash(),
                    authorities
            );
        }

        throw new UsernameNotFoundException("Пользователь с email " + email + " не найден");
    }
}