package com.moneytransfer.reactive.handlers;

import com.moneytransfer.reactive.model.Account;
import com.moneytransfer.reactive.model.Transaction;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

public interface TransactionsHandler {
    /**
     * Get the transactions for a certain account (source or destination account)
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     * @param transactions  in-memory storage of transactions
     */
    void getTransactionOfAccount(RoutingContext routingContext, Map<Integer, Account> accounts, Map<Integer, Transaction> transactions);

    /**
     * Get transaction by Id
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param transactions  in-memory storage of transactions
     */
    void getTransaction(RoutingContext routingContext, Map<Integer, Transaction> transactions);

    /**
     * Create a new transaction
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param accounts in-memory storage of accounts
     * @param transactions  in-memory storage of transactions
     */
    void newTransaction(RoutingContext routingContext, Map<Integer, Account> accounts, Map<Integer, Transaction> transactions);

    /**
     * Get all transactions
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param transactions  in-memory storage of transactions
     */
    void getAllTransactions(RoutingContext routingContext, Map<Integer, Transaction> transactions);

    /**
     * Send transaction details to the client as a HttpServerResponse
     * @param routingContext Represents the context for the handling of a request in Vert.x-Web
     * @param transaction  the transaction to return as a JSON to the client
     * @param statusCode http status code
     */
    void sendTransactionResponse(RoutingContext routingContext, Transaction transaction, int statusCode);
}
