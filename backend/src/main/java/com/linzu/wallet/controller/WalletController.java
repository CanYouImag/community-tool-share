package com.linzu.wallet.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.wallet.dto.FreezeRequest;
import com.linzu.wallet.dto.WalletResponse;
import com.linzu.wallet.dto.WalletTransactionResponse;
import com.linzu.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "钱包")
@RestController
@RequestMapping({ "/api/v1/wallet", "/api/wallet" })
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "获取钱包")
    @GetMapping
    public Result<WalletResponse> getWallet() {
        return Result.ok(walletService.getWallet(AuthContext.requireUserId()));
    }

    @Operation(summary = "分页查询资金流水")
    @GetMapping("/transactions")
    public Result<PageResult<WalletTransactionResponse>> transactions(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String type
    ) {
        return Result.ok(
            walletService.getTransactions(AuthContext.requireUserId(), page, size, type)
        );
    }

    /**
     * 兼容宋嘉幸现有钱包页的 /wallet/flows 接口形状：{ list, total }。
     */
    @Operation(summary = "资金流水（兼容旧前端）")
    @GetMapping("/flows")
    public Result<Map<String, Object>> flows(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String type
    ) {
        PageResult<WalletTransactionResponse> result = walletService.getTransactions(
            AuthContext.requireUserId(),
            page,
            size,
            type
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.ok(data);
    }

    /**
     * 兼容宋嘉幸现有钱包页的 /wallet/deposits 接口。
     * 押金明细需要订单域数据，订单接口完成前先返回空列表。
     */
    @Operation(summary = "押金明细（兼容旧前端）")
    @GetMapping("/deposits")
    public Result<List<Map<String, Object>>> deposits() {
        return Result.ok(List.of());
    }

    @Operation(summary = "冻结余额")
    @PostMapping("/freeze")
    public Result<WalletResponse> freeze(@Valid @RequestBody FreezeRequest request) {
        return Result.ok(
            walletService.freeze(
                AuthContext.requireUserId(),
                request.amount(),
                request.refType(),
                request.refId(),
                request.remark()
            )
        );
    }

    @Operation(summary = "解冻余额")
    @PostMapping("/unfreeze")
    public Result<WalletResponse> unfreeze(@Valid @RequestBody FreezeRequest request) {
        return Result.ok(
            walletService.unfreeze(
                AuthContext.requireUserId(),
                request.amount(),
                request.refType(),
                request.refId(),
                request.remark()
            )
        );
    }
}
