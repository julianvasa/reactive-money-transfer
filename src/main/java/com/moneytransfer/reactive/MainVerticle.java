package com.moneytransfer.reactive;

import com.moneytransfer.reactive.enums.AccountOperation;
import com.moneytransfer.reactive.enums.TransactionStatus;
import com.moneytransfer.reactive.handlers.AccountsHandler;
import com.moneytransfer.reactive.handlers.impl.AccountsHandlerImpl;
import com.moneytransfer.reactive.handlers.TransactionsHandler;
import com.moneytransfer.reactive.handlers.impl.TransactionsHandlerImpl;
import com.moneytransfer.reactive.model.Account;
import com.moneytransfer.reactive.model.Transaction;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MainVerticle
 */
public class MainVerticle extends AbstractVerticle {
    private final Map<Integer, Account> accounts = new LinkedHashMap<>();
    private final Map<Integer, Transaction> transactions = new LinkedHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        insertSampleData();

        /* Endpoints are exposed through a Router that will map a route to a handler which is basically the business code */
        Router router = Router.router(vertx);
        /* Enables the reading of the request body for all routes under /accounts */
        router.route("/accounts").handler(BodyHandler.create());
        /* Enables the reading of the request body for all routes under /transactions */
        router.route("/transactions").handler(BodyHandler.create());

        AccountsHandler accountsHandler = new AccountsHandlerImpl();
        TransactionsHandler transactionsHandler = new TransactionsHandlerImpl();

        /* Validate account number (id) before proceeding with any other endpoint which needs an id as a parameter */
        router.route("/accounts/:id").handler(accountsHandler::parseAccountNumber);
        /* Get all accounts */
        router.get("/accounts").handler(routingContext -> accountsHandler.getAllAccounts(routingContext, accounts));
        /* Post a new account */
        router.post("/accounts").handler(routingContext -> accountsHandler.newAccount(routingContext, accounts));
        /* Get account by Id */
        router.get("/accounts/:id").handler(routingContext -> accountsHandler.getAccount(routingContext, accounts));
        /* Delete an account */
        router.delete("/accounts/:id").handler(routingContext -> accountsHandler.deleteAccount(routingContext, accounts));
        /* Deposit or Withdraw */
        router.put("/accounts/:id/deposit/:amount").handler(
            routingContext -> accountsHandler.accountOperation(routingContext, AccountOperation.DEPOSIT, accounts));
        router.put("/accounts/:id/withdraw/:amount").handler(
            routingContext -> accountsHandler.accountOperation(routingContext, AccountOperation.WITHDRAW, accounts));

        /* Get all transactions */
        router.get("/transactions").handler(routingContext -> transactionsHandler.getAllTransactions(routingContext,transactions));
        /* Post a new transaction */
        router.post("/transactions").handler(routingContext -> transactionsHandler.newTransaction(routingContext,accounts,transactions));
        /* Get transaction by Id */
        router.get("/transactions/:id").handler(routingContext -> transactionsHandler.getTransaction(routingContext,transactions));
        /* Get all transactions of a certain account identified with the provided Id */
        router.get("/transactions/account/:id").handler(routingContext -> transactionsHandler.getTransactionOfAccount(routingContext,accounts,transactions));

        /* Just a simple endpoint to check whether the server is responding or not */
        router.get("/health").handler(rc -> rc.response().end("OK"));
        /* Start the HTTP server on port 8080 */
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(
                8080,
                result -> {
                    if (result.succeeded()) {
                        startPromise.complete();
                    }
                    else {
                        startPromise.fail(result.cause());
                    }
                }
            );
    }

    /* Insert some sample data on server start */
    private void insertSampleData() {
        Account account1 = Account.builder()
            .id(1111)
            .name("account 1")
            .balance(BigDecimal.valueOf(100))
            .currency(Currency.getInstance("EUR"))
            .build();
        Account account2 = Account.builder()
            .id(2222)
            .name("account 2")
            .balance(BigDecimal.valueOf(200))
            .currency(Currency.getInstance("USD"))
            .build();
        Account account3 = Account.builder()
            .id(3333)
            .name("account 3")
            .balance(BigDecimal.valueOf(300))
            .currency(Currency.getInstance("GBP"))
            .build();
        accounts.put(1111, account1);
        accounts.put(2222, account2);
        accounts.put(3333, account3);

        Transaction transaction1 = new Transaction(2222, 1111, BigDecimal.valueOf(12), Currency.getInstance("EUR"));
        Transaction transaction2 = new Transaction(3333, 1111, BigDecimal.valueOf(34), Currency.getInstance("USD"));
        transaction1.setStatus(TransactionStatus.SUCCESSFUL);
        transaction2.setStatus(TransactionStatus.SUCCESSFUL);
        transaction1.setDescription("test transaction 1");
        transaction2.setDescription("test transaction 2");
        transactions.put(transaction1.getId(), transaction1);
        transactions.put(transaction2.getId(), transaction2);
    }
}
