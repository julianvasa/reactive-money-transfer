package com.moneytransfer.reactive.handlers;

import com.moneytransfer.reactive.enums.AccountOperation;
import com.moneytransfer.reactive.model.Account;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

public interface AccountsHandler {
    /**
     * Parse account number (id) before proceeding with any other endpoint which needs an id as a parameter
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     */
    void parseAccountNumber(RoutingContext routingContext);

    /**
     * Get all accounts
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    void getAllAccounts(RoutingContext routingContext, Map<Integer, Account> accounts);

    /**
     * Get account by Id
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    void getAccount(RoutingContext routingContext, Map<Integer, Account> accounts);

    /**
     * Add a new account
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    void newAccount(RoutingContext routingContext, Map<Integer, Account> accounts);

    /**
     * Delete an account
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     */
    void deleteAccount(RoutingContext routingContext, Map<Integer, Account> accounts);

    /**
     * Deposit or Withdraw
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param operation WITHDRAW or DEPOSIT
     * @param accounts in-memory storage of accounts
     */
    void accountOperation(RoutingContext routingContext, AccountOperation operation, Map<Integer, Account> accounts);

    /**
     * Send account details to the client as a HttpServerResponse
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param account account to resturn as a JSON to the client
     * @param statusCode http status
     */
    void sendAccountResponse(RoutingContext routingContext, Account account, int statusCode);
}
