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

package net.impactdev.impactor.core.commands.economy;

import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.configuration.Config;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.impactdev.impactor.api.economy.transactions.EconomyTransferTransaction;
import net.impactdev.impactor.api.economy.transactions.details.EconomyResultType;
import net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType;
import net.impactdev.impactor.api.platform.sources.PlatformSource;
import net.impactdev.impactor.api.services.permissions.PermissionsService;
import net.impactdev.impactor.api.utility.Context;
import net.impactdev.impactor.core.economy.EconomyConfig;
import net.impactdev.impactor.core.economy.ImpactorEconomyService;
import net.impactdev.impactor.core.economy.context.TransactionContext;
import net.impactdev.impactor.core.economy.context.TransferTransactionContext;
import net.impactdev.impactor.core.economy.currencylimit.CurrencyLimitConfig;
import net.impactdev.impactor.core.economy.currencylimit.PlayerCapTracker;
import net.impactdev.impactor.core.translations.internal.ImpactorTranslations;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.ProxiedBy;
import org.incendo.cloud.annotations.processing.CommandContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"DuplicatedCode", "unused"})
@CommandContainer
@Permission("impactor.commands.economy.base")
public final class EconomyCommands {

    @Command("economy|eco balance [currency] [target]")
    @ProxiedBy("balance")
    @Permission("impactor.commands.economy.balance")
    @CommandDescription("Fetches the balance of the source or identified target")
    public void balance(final @NotNull CommandSource source, @Nullable @Argument("target") PlatformSource target, @Nullable @Argument("currency") Currency currency) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = target != null ? target : source.source();

        Account account = service.account(c, focus.uuid()).join();

        Context context = Context.empty();
        context.append(Currency.class, c);
        context.append(Account.class, account);
        ImpactorTranslations.ECONOMY_BALANCE.send(source, context);
    }

    @Command("economy|eco withdraw <amount> [currency] [target]")
    @ProxiedBy("withdraw")
    @Permission("impactor.commands.economy.withdraw")
    public void withdraw(
            final @NotNull CommandSource source,
            @Argument("amount") double amount,
            @Nullable @Argument("target") PlatformSource target,
            @Nullable @Argument("currency") Currency currency
    ) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = target != null ? target : source.source();

        service.account(c, focus.uuid()).thenAccept(account -> {
            BigDecimal before = account.balance();

            EconomyTransaction transaction = account.withdraw(new BigDecimal(amount));
            Context context = Context.empty();
            context.append(TransactionContext.class, new TransactionContext(EconomyTransactionType.WITHDRAW, before, account.balance(), transaction.result()));
            context.append(Currency.class, c);
            context.append(Account.class, account);

            if(!transaction.successful()) {
                ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.send(source, context);
            } else {
                ImpactorTranslations.ECONOMY_TRANSACTION.send(source, context);
            }
        });

    }

    @Command("economy|eco deposit <amount> [currency] [target]")
    @ProxiedBy("deposit")
    @Permission("impactor.commands.economy.deposit")
    public void deposit(
            final @NotNull CommandSource source,
            @Argument("amount") double amount,
            @Nullable @Argument("target") PlatformSource target,
            @Nullable @Argument("currency") Currency currency
    ) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = target != null ? target : source.source();

        service.account(c, focus.uuid()).thenAccept(account -> {
            BigDecimal before = account.balance();

            EconomyTransaction transaction = account.deposit(new BigDecimal(amount));
            Context context = Context.empty();
            context.append(TransactionContext.class, new TransactionContext(EconomyTransactionType.DEPOSIT, before, account.balance(), transaction.result()));
            context.append(Currency.class, c);
            context.append(Account.class, account);

            if(!transaction.successful()) {
                ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.send(source, context);
            } else {
                ImpactorTranslations.ECONOMY_TRANSACTION.send(source, context);
            }
        });
    }

    @Command("economy|eco set <amount> [currency] [target]")
    @Permission("impactor.commands.economy.set")
    public void set(
            final @NotNull CommandSource source,
            @Argument("amount") double amount,
            @Nullable @Argument("target") PlatformSource target,
            @Nullable @Argument("currency") Currency currency
    ) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = target != null ? target : source.source();

        service.account(c, focus.uuid()).thenAccept(account -> {
            BigDecimal before = account.balance();

            EconomyTransaction transaction = account.set(new BigDecimal(amount));
            Context context = Context.empty();
            context.append(TransactionContext.class, new TransactionContext(EconomyTransactionType.SET, before, account.balance(), transaction.result()));
            context.append(Currency.class, c);
            context.append(Account.class, account);

            if(!transaction.successful()) {
                ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.send(source, context);
            } else {
                ImpactorTranslations.ECONOMY_TRANSACTION.send(source, context);
            }
        });

    }

    @Command("economy|eco reset [currency] [target]")
    @Permission("impactor.commands.economy.reset")
    public void reset(
            final @NotNull CommandSource source,
            @Nullable @Argument("target") PlatformSource target,
            @Nullable @Argument("currency") Currency currency
    ) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = target != null ? target : source.source();

        service.account(c, focus.uuid()).thenAccept(account -> {
            BigDecimal before = account.balance();

            EconomyTransaction transaction = account.reset();
            Context context = Context.empty();
            context.append(TransactionContext.class, new TransactionContext(EconomyTransactionType.RESET, before, account.balance(), transaction.result()));
            context.append(Currency.class, c);
            context.append(Account.class, account);

            if(!transaction.successful()) {
                ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.send(source, context);
            } else {
                ImpactorTranslations.ECONOMY_TRANSACTION.send(source, context);
            }
        });
    }

    @Command("economy|eco pay <amount> <target> [currency] [source]")
    @ProxiedBy("pay")
    @Permission("impactor.commands.economy.pay.base")
    public void transfer(
            final @NotNull CommandSource sender,
            @Argument("amount") double amount,
            @Argument("target") PlatformSource target,
            @Nullable @Argument("currency") Currency currency,
            @Nullable @Argument("source") PlatformSource from
    ) {
        EconomyService service = EconomyService.instance();
        Currency c = currency != null ? currency : service.currencies().primary();
        PlatformSource focus = from != null ? from : sender.source();

        Context context = Context.empty();
        context.append(Currency.class, c);

        if(from != null) {
            PermissionsService permissions = Impactor.instance().services().provide(PermissionsService.class);
            if(!permissions.hasPermission(sender.source(), "impactor.commands.economy.pay.other")) {
                ImpactorTranslations.NO_PERMISSION.send(sender, context);
                return;
            }
        } else {
            if(target.uuid().equals(sender.uuid())) {
                ImpactorTranslations.ECONOMY_CANT_PAY_SELF.send(sender, context);
                return;
            }
        }

        if(c.transferable() == TriState.FALSE) {
            ImpactorTranslations.ECONOMY_TRANSFER_NOT_ALLOWED.send(sender, context);
            return;
        } else {
            Config config = ((ImpactorEconomyService) service).config();
            if(c.transferable() == TriState.NOT_SET && !config.get(EconomyConfig.ALLOW_TRANSFER_ON_NOT_SET)) {
                ImpactorTranslations.ECONOMY_TRANSFER_NOT_ALLOWED.send(sender, context);
                return;
            }
        }

        service.account(c, focus.uuid()).thenAccept(s -> {
            Account to = service.account(c, target.uuid()).join();

            BigDecimal sb = s.balance();
            BigDecimal tb = to.balance();
            BigDecimal total = new BigDecimal(amount);

            EconomyTransferTransaction transaction = EconomyTransferTransaction.compose()
                    .to(to)
                    .from(s)
                    .amount(total)
                    .message(EconomyResultType.NOT_ENOUGH_FUNDS, () -> ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.resolve(sender.locale(), context))
                    .message(EconomyResultType.INVALID, () -> ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.resolve(sender.locale(), context))
                    .message(EconomyResultType.FAILED, () -> ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.resolve(sender.locale(), context))
                    .message(EconomyResultType.SUCCESS, () -> ImpactorTranslations.ECONOMY_TRANSFER.resolve(sender.locale(), context))
                    .build();

            context.append(TransferTransactionContext.class, new TransferTransactionContext(
                    new TransactionContext(EconomyTransactionType.WITHDRAW, sb, s.balance(), transaction.result()),
                    new TransactionContext(EconomyTransactionType.DEPOSIT, tb, to.balance(), transaction.result()),
                    transaction.result()
            ));

            transaction.inform(sender);
            if(!target.equals(focus) && transaction.successful()) {
                context.append(PlatformSource.class, sender.source());
                context.append(BigDecimal.class, total);

                ImpactorTranslations.ECONOMY_RECEIVE_PAYMENT.send(target, context);
            }
        });
    }

    @Command("economy|eco baltop [page]")
    @ProxiedBy("baltop")
    @Permission("impactor.commands.economy.baltop")
    public void baltop(
            final CommandSource source,
            @Nullable @Argument("page") Integer page,
            @Nullable @Flag("currency") Currency currency,
            @Flag("extended") boolean nonPlayers
    ) {
        EconomyService service = EconomyService.instance();
        Currency target = currency != null ? currency : service.currencies().primary();

        AtomicInteger max = new AtomicInteger(10);
        if(service instanceof ImpactorEconomyService) {
            Config config = ((ImpactorEconomyService) service).config();
            max.set(config.get(EconomyConfig.MAX_BALTOP_ENTRIES));
        }

        int targetPage = (page != null && page > 0) ? page : 1;
        int pageSize = max.get();
        long skip = (long) (targetPage - 1) * pageSize;

        ImpactorTranslations.ECONOMY_BALTOP_CALCULATING.send(source, Context.empty());
        service.accounts(target).thenAccept(accounts -> {
            Context context = Context.empty().append(Currency.class, target);
            ImpactorTranslations.ECONOMY_BALTOP_HEADER.send(source, context);

            AtomicInteger ranking = new AtomicInteger((int) skip + 1);
            accounts.stream()
                    .sorted(Comparator.<Account, BigDecimal>comparing(Account::balance).reversed())
                    .filter(account -> !account.virtual() || nonPlayers)
                    .skip(skip)
                    .limit(pageSize)
                    .forEach(account -> {
                        Context relative = Context.empty().with(context)
                                .append(Account.class, account)
                                .append(Integer.class, ranking.getAndIncrement());
                        ImpactorTranslations.ECONOMY_BALTOP_ENTRY.send(source, relative);
                    });

            ImpactorTranslations.ECONOMY_BALTOP_FOOTER.send(source, context);
        });
    }

    @Command("capdeposit <player> add <currency> <amount>")
    @Permission("impactor.commands.economy.capdeposit")
    @CommandDescription("Grants currency to a player, limited by how much they can still receive this period; excess is discarded")
    public void capDepositAdd(
            final @NotNull CommandSource source,
            @Argument("player") PlatformSource target,
            @Argument("currency") Currency currency,
            @Argument("amount") double amount
    ) {
        String currencyKey = currency.key().toString();
        BigDecimal periodCap = CurrencyLimitConfig.getCap(currencyKey);
        BigDecimal alreadyGained = PlayerCapTracker.getGained(target.uuid(), currencyKey);
        BigDecimal room = periodCap.subtract(alreadyGained).max(BigDecimal.ZERO);
        BigDecimal requested = BigDecimal.valueOf(amount);
        BigDecimal applied = requested.min(room);
        BigDecimal discarded = requested.subtract(applied);

        EconomyService service = EconomyService.instance();

        if (applied.signum() > 0) {
            service.account(currency, target.uuid()).thenAccept(account -> {
                BigDecimal before = account.balance();
                EconomyTransaction transaction = account.deposit(applied);
                PlayerCapTracker.addGained(target.uuid(), currencyKey, applied);

                Context context = Context.empty();
                context.append(TransactionContext.class, new TransactionContext(EconomyTransactionType.DEPOSIT, before, account.balance(), transaction.result()));
                context.append(Currency.class, currency);
                context.append(Account.class, account);

                if (!transaction.successful()) {
                    ImpactorTranslations.ECONOMY_TRANSACTION_FAILED.send(source, context);
                } else {
                    ImpactorTranslations.ECONOMY_TRANSACTION.send(source, context);
                }

                if (discarded.signum() > 0) {
                    source.source().sendMessage(net.kyori.adventure.text.Component.text(
                            "Excess of " + formatAmount(discarded) + " " + currencyKey + " was discarded (period cap of " + formatAmount(periodCap) + " reached)."
                    ).color(NamedTextColor.GRAY));
                }
            });
        } else {
            source.source().sendMessage(net.kyori.adventure.text.Component.text(
                    "Nothing granted — player has already received the full period cap of " + formatAmount(periodCap) + " " + currencyKey + "."
            ).color(NamedTextColor.GRAY));
        }
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
    @Command("capdeposit set <currency> <amount>")
    @Permission("impactor.commands.economy.capdeposit")
    @CommandDescription("Sets the global period cap for a currency (applies to future capdeposit add calls)")
    public void capDepositSet(
            final @NotNull CommandSource source,
            @Argument("currency") Currency currency,
            @Argument("amount") double amount
    ) {
        BigDecimal newCap = BigDecimal.valueOf(amount);
        CurrencyLimitConfig.setCap(currency.key().toString(), newCap);

        source.source().sendMessage(net.kyori.adventure.text.Component.text(
                "Cap for " + currency.key() + " set to " + formatAmount(newCap) + " (applies to future deposits)."
        ).color(NamedTextColor.GRAY));
    }

    @Command("resetcapdeposit")
    @Permission("impactor.commands.economy.resetcapdeposit")
    @CommandDescription("Resets period gain counters for all capped currencies")
    public void resetCapDepositAll(final @NotNull CommandSource source) {
        resetAllCapped(source);
    }

    @Command("resetcapdeposit <currency>")
    @Permission("impactor.commands.economy.resetcapdeposit")
    @CommandDescription("Resets the period gain counter for a single currency")
    public void resetCapDepositSingle(
            final @NotNull CommandSource source,
            @Argument("currency") Currency currency
    ) {
        String currencyKey = currency.key().toString();
        int count = PlayerCapTracker.resetAll(currencyKey);

        source.source().sendMessage(net.kyori.adventure.text.Component.text(
                "Reset period gain counters for " + count + " player(s) across " + currencyKey + "."
        ).color(NamedTextColor.GRAY));
    }

    private void resetAllCapped(CommandSource source) {
        Set<String> cappedCurrencyKeys = CurrencyLimitConfig.cappedCurrencyKeys();
        int count = PlayerCapTracker.resetAllCurrencies(cappedCurrencyKeys);

        source.source().sendMessage(net.kyori.adventure.text.Component.text(
                "Reset period gain counters for " + count + " player(s) across all capped currencies."
        ).color(NamedTextColor.GRAY));
    }

    @Command("capdeposit unset <currency>")
    @Permission("impactor.commands.economy.capdeposit")
    @CommandDescription("Removes the period cap for a currency, making it uncapped again")
    public void capDepositUnset(
            final @NotNull CommandSource source,
            @Argument("currency") Currency currency
    ) {
        String currencyKey = currency.key().toString();
        CurrencyLimitConfig.unsetCap(currencyKey);

        source.source().sendMessage(net.kyori.adventure.text.Component.text(
                "Cap removed for " + currencyKey + " — deposits are now unlimited via capdeposit."
        ).color(NamedTextColor.GRAY));
    }

    @Command("capdeposit reload")
    @Permission("impactor.commands.economy.capdeposit")
    @CommandDescription("Reloads capdeposit configuration from disk")
    public void capDepositReload(final @NotNull CommandSource source) {
        CurrencyLimitConfig.load();

        source.source().sendMessage(net.kyori.adventure.text.Component.text(
                "Reloaded capdeposit configuration from disk."
        ).color(NamedTextColor.GRAY));
    }
}