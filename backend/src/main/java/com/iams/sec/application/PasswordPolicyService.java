package com.iams.sec.application;

import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.sec.domain.PasswordPolicy;
import com.iams.sec.domain.PasswordPolicyRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-SEC-05: read and update the org-wide password policy (a single seeded row - see PasswordPolicy). */
@Service
public class PasswordPolicyService {

    private final PasswordPolicyRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public PasswordPolicyService(PasswordPolicyRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PasswordPolicy get() {
        return repository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: password_policy has no row"));
    }

    @Transactional
    public PasswordPolicy update(Integer minLength, Boolean requireUppercase, Boolean requireLowercase,
                                  Boolean requireDigit, Boolean requireSpecial, long expectedVersion) {
        PasswordPolicy policy = get();
        if (policy.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, policy.getVersion(), policy);
        }
        if (minLength != null) {
            policy.setMinLength(minLength);
        }
        if (requireUppercase != null) {
            policy.setRequireUppercase(requireUppercase);
        }
        if (requireLowercase != null) {
            policy.setRequireLowercase(requireLowercase);
        }
        if (requireDigit != null) {
            policy.setRequireDigit(requireDigit);
        }
        if (requireSpecial != null) {
            policy.setRequireSpecial(requireSpecial);
        }
        policy.setUpdatedBy(currentUserProvider.current().id());
        try {
            return repository.saveAndFlush(policy);
        } catch (OptimisticLockingFailureException e) {
            PasswordPolicy current = get();
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }
}
