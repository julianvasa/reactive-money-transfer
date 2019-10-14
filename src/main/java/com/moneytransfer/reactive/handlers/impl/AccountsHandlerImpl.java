package com.moneytransfer.reactive.handlers.impl;

import com.moneytransfer.reactive.enums.AccountOperation;
import com.moneytransfer.reactive.handlers.AccountsHandler;
import com.moneytransfer.reactive.model.Account;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.math.BigDecimal;
import java.util.Map;

import static com.moneytransfer.reactive.exception.Exception.error;

public class AccountsHandlerImpl implements AccountsHandler {
    private final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    /**
     * Parse account number (id) before proceeding with any other endpoint which needs an id as a parameter
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     */
    @Override
    public void parseAccountNumber(RoutingContext routingContext) {
        try {
            Integer.parseInt(routingContext.pathParam("id"));
            routingContext.next();
        } catch (NumberFormatException exception) {
            error(routingContext, 400, "Invalid Account Number: " + exception.getCause());
        }
    }

    /**
     * Get all accounts
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    @Override
    public void getAllAccounts(RoutingContext routingContext, Map<Integer, Account> accounts) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(200)
            .end(Json.encodePrettily(accounts.values()));
    }

    /**
     * Get account by Id
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    @Override
    public void getAccount(RoutingContext routingContext, Map<Integer, Account> accounts) {
        final String id = routingContext.request().getParam("id");
        final Integer accountNumber = Integer.valueOf(id);
        Account account = accounts.get(accountNumber);
        if (account == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            sendAccountResponse(routingContext, account, 200);
        }
    }

    /**
     * Add a new account
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    @Override
    public void newAccount(RoutingContext routingContext, Map<Integer, Account> accounts) {
        try {
            final Account account = Json.decodeValue(routingContext.getBodyAsString(), Account.class);

            if (accounts.containsKey(account.getId())) {
                error(routingContext, 409, "Account number already exists in the DB!");
            }
            accounts.put(account.getId(), account);
            sendAccountResponse(routingContext, account, 201);
        } catch (RuntimeException exception) {
            error(routingContext, 415, "Unable to parse Account JSON request body! Cause: " + exception.getCause());
        }
    }

    /**
     * Delete an account
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    @Override
    public void deleteAccount(RoutingContext routingContext, Map<Integer, Account> accounts) {
        String id = routingContext.request().getParam("id");
        if (accounts.get(Integer.valueOf(id)) == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            Integer accountNumber = Integer.valueOf(id);
            accounts.remove(accountNumber);
            error(routingContext, 204, "Account deleted: " + accountNumber);
        }
    }

    /**
     * Deposit or Withdraw
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param operation WITHDRAW or DEPOSIT
     * @param accounts in-memory storage of accounts
     */
    @Override
    public void accountOperation(RoutingContext routingContext, AccountOperation operation, Map<Integer, Account> accounts) {
        final String id = routingContext.pathParam("id");
        final String amountParam = routingContext.pathParam("amount");
        final Integer accountNumber = Integer.valueOf(id);
        final BigDecimal amount = BigDecimal.valueOf(Long.parseLong(amountParam));

        if (accounts.get(accountNumber) == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            Account account = accounts.get(accountNumber);
            if (operation.equals(AccountOperation.DEPOSIT)) {
                account.deposit(amount);
                sendAccountResponse(routingContext, account, 200);
            }
            else if (operation.equals(AccountOperation.WITHDRAW)) {
                if (account.getBalance().compareTo(amount) < 0) {
                    error(routingContext, 403, "Account balance < amount: s" + amount);
                }
                account.withdraw(amount);
                sendAccountResponse(routingContext, account, 200);
            }
        }
    }

    /**
     * Send account details to the client as a HttpServerResponse
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param account account to resturn as a JSON to the client
     * @param statusCode http status
     */
    @Override
    public void sendAccountResponse(RoutingContext routingContext, Account account, int statusCode) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(statusCode)
            .end(Json.encodePrettily(account));
    }

}

