package com.obsdl.master.service;

import com.obsdl.master.dto.account.AccountCreateRequest;
import com.obsdl.master.dto.account.AccountResponse;
import com.obsdl.master.dto.account.AccountUpdateRequest;
import com.obsdl.master.entity.ObsAccountEntity;
import com.obsdl.master.exception.BizException;
import com.obsdl.master.service.crud.ObsAccountCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final ObsAccountCrudService obsAccountCrudService;

    public AccountService(ObsAccountCrudService obsAccountCrudService) {
        this.obsAccountCrudService = obsAccountCrudService;
    }

    public AccountResponse create(AccountCreateRequest request) {
        ObsAccountEntity entity = new ObsAccountEntity();
        entity.setAccountName(request.name());
        entity.setAccessKey(request.accessKey());
        entity.setSecretKey(request.secretKey());
        entity.setEndpoint(request.endpoint());
        entity.setBucket(request.bucket());
        obsAccountCrudService.save(entity);
        return toResponse(entity);
    }

    public List<AccountResponse> list() {
        return obsAccountCrudService.lambdaQuery()
                .orderByAsc(ObsAccountEntity::getId)
                .list()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse update(AccountUpdateRequest request) {
        ObsAccountEntity entity = obsAccountCrudService.getById(request.id());
        if (entity == null) {
            throw new BizException(40401, "账户不存在");
        }
        entity.setAccountName(request.name());
        entity.setAccessKey(request.accessKey());
        entity.setSecretKey(request.secretKey());
        entity.setEndpoint(request.endpoint());
        entity.setBucket(request.bucket());
        obsAccountCrudService.updateById(entity);
        return toResponse(entity);
    }

    public void delete(Long id) {
        if (!obsAccountCrudService.removeById(id)) {
            throw new BizException(40401, "账户不存在");
        }
    }

    private AccountResponse toResponse(ObsAccountEntity entity) {
        return new AccountResponse(
                entity.getId(),
                entity.getAccountName(),
                entity.getAccessKey(),
                entity.getEndpoint(),
                entity.getBucket()
        );
    }
}
