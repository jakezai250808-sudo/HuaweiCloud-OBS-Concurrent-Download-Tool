package com.obsdl.master.service;

import com.obsdl.master.dto.account.AccountCreateRequest;
import com.obsdl.master.dto.account.AccountResponse;
import com.obsdl.master.dto.account.AccountUpdateRequest;
import com.obsdl.master.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AccountService {

    private final AtomicLong accountIdGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, AccountResponse> accounts = new ConcurrentHashMap<>();

    public AccountResponse create(AccountCreateRequest request) {
        long id = accountIdGenerator.getAndIncrement();
        AccountResponse account = new AccountResponse(id, request.name(), request.accessKey(), request.endpoint(), request.bucket());
        accounts.put(id, account);
        return account;
    }

    public List<AccountResponse> list() {
        return accounts.values().stream().sorted(Comparator.comparing(AccountResponse::id)).toList();
    }

    public AccountResponse update(AccountUpdateRequest request) {
        if (!accounts.containsKey(request.id())) {
            throw new BizException(40401, "账户不存在");
        }
        AccountResponse updated = new AccountResponse(request.id(), request.name(), request.accessKey(), request.endpoint(), request.bucket());
        accounts.put(request.id(), updated);
        return updated;
    }

    public void delete(Long id) {
        if (accounts.remove(id) == null) {
            throw new BizException(40401, "账户不存在");
        }
    }
}
