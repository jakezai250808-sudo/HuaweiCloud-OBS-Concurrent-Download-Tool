package com.obsdl.master.controller;

import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.dto.account.AccountCreateRequest;
import com.obsdl.master.dto.account.AccountResponse;
import com.obsdl.master.dto.account.AccountUpdateRequest;
import com.obsdl.master.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "创建账户")
    public ApiResponse<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        return ApiResponse.success(accountService.create(request));
    }

    @GetMapping
    @Operation(summary = "查询账户列表")
    public ApiResponse<List<AccountResponse>> list() {
        return ApiResponse.success(accountService.list());
    }

    @PutMapping
    @Operation(summary = "更新账户")
    public ApiResponse<AccountResponse> update(@Valid @RequestBody AccountUpdateRequest request) {
        return ApiResponse.success(accountService.update(request));
    }

    @DeleteMapping
    @Operation(summary = "删除账户")
    public ApiResponse<Void> delete(@RequestParam @NotNull Long id) {
        accountService.delete(id);
        return ApiResponse.success();
    }
}
