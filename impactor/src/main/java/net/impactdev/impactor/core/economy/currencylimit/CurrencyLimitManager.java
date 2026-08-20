/*
 * This file is part of Impactor, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2022 NickImpact
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package net.impactdev.impactor.core.economy.currencylimit;

import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class CurrencyLimitManager {

    public static void init() {
        CurrencyLimitConfig.load();
    }


    public static Optional<Currency> resolveCurrency(String input) {
        EconomyService service = EconomyService.instance();
        String normalized = input.toLowerCase();
        String withNamespace = normalized.contains(":") ? normalized : "impactor:" + normalized;

        return service.currencies().registered().stream()
                .filter(c -> {
                    String keyStr = c.key().toString().toLowerCase();
                    return keyStr.equals(normalized) || keyStr.equals(withNamespace);
                })
                .findFirst();
    }


    public static BigDecimal addCurrency(UUID uuid, Currency currency, String typedCurrencyKey, BigDecimal amount) {
        if (amount.signum() <= 0) return BigDecimal.ZERO;

        EconomyService service = EconomyService.instance();
        Account account = service.account(currency, uuid).join();

        BigDecimal cap = CurrencyLimitConfig.getCap(typedCurrencyKey);
        BigDecimal current = account.balance();
        BigDecimal room = cap.subtract(current).max(BigDecimal.ZERO);
        BigDecimal applied = amount.min(room);

        if (applied.signum() > 0) {
            account.deposit(applied);
        }

        return applied;
    }

    public static void setCap(String typedCurrencyKey, BigDecimal newCap) {
        CurrencyLimitConfig.setCap(typedCurrencyKey, newCap);
    }

    public static BigDecimal getCap(String typedCurrencyKey) {
        return CurrencyLimitConfig.getCap(typedCurrencyKey);
    }
}