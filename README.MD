# Reactive Money Transfer Application

A RESTful API that allows transfer money from one Bank Account to another in any currency.

It uses two entities:
* transaction - the money transfer transaction used to initialize the transaction
* account - the bank account which has balance in the specified currency

The API was developed using Java 8 based on Vert.x.

Eclipse Vert.x is a tool-kit for building reactive applications on the JVM.
Eclipse Vert.x is event driven and non blocking. This means your app can handle a lot of concurrency using a small number of kernel threads. Vert.x lets your app scale with minimal hardware.

Some of the reasons why i choose Vert.x for this task are listed below:

- Vert.x is lightweight
- Vert.x is fast
- Vert.x is not an application server. There's no monolithic Vert.x instance into which you deploy applications. You just run your apps wherever you want to.
- Vert.x is modular - when you need more bits just add the bits you need and nothing more.
- Vert.x is simple but not simplistic. Vert.x allows you to create powerful apps, simply.
- Vert.x is an ideal choice for creating light-weight, high-performance, microservices.
- Simple to use concurrency and scalability
- Polyglot. You can use Vert.x with multiple languages including Java, JavaScript, Groovy, Ruby, Ceylon, Scala and Kotlin.

The threading model proposed by the event loop has a huge benefit:
it simplifies concurrency. As there is only one thread, you are always
called by the same thread and never concurrently. 
The single thread event-loop principle guaranties the data consistency in any case, even if it will be a huge amount concurrent users. 


## Requires
* Java 8
* Maven

## How to start

Once the application is fetched from git it can be built with maven

    mvn clean install
    
This will fetch dependencies and run all tests

To run the app execute:

    java -jar /target/moneytransfer.jar

The application will start on the `localhost` and will be listening to the port `8080`

## API Definition

### Account
The bank account entity which has balance in the specified currency and could transfer the money
if there is enough money.

#### Structure
    {
        "id"        : <number>,
        "name"      : <string>,
        "balance"   : <BigDecimal>,
        "currency"  : <Currency>
    }

#### Create Bank Account

The following creates bank account and returns the created entity with `ID` specified

    POST /accounts
    {
        "id"        : 12345678
        "name"      : "Account Name - Description",
        "balance"   : 12.6,
        "currency"  : "EUR"
    }

Example response:

    HTTP 201 CREATED
    POST /accounts
    {
        "id"        : 12345678
        "name"      : "Account Name - Description",
        "balance"   : 12.6,
        "currency"  : "EUR"
    }
    
#### List all Bank Accounts

The following gets all the bank accounts that exist in the system

    GET /accounts

Example response:


    HTTP 200 OK
    [
      {
        "id"        : 3333,
        "name"      : "account 3",
        "balance"   : 300,
        "currency"  : "GBP"
      },
      {
        "id"        : 1111,
        "name"      : "account 1",
        "balance"   : 100,
        "currency"  : "EUR"
      },
      {
        "id"        : 2222,
        "name"      : "account 2",
        "balance"   : 200,
        "currency"  : "USD"
      }
    ]

#### Get Bank Account details

The following gets the particular account if it exists in the system

    GET /accounts/1111

Example response:

    HTTP 200 OK
    {
      "id"      : 1111,
      "name"    : "account 1",
      "balance" : 100,
      "currency": "EUR"
    }

#### Perform a Withdraw or Deposit

The following gets the particular account if it exists in the system

    PUT /accounts/1111/deposit/1000
    PUT /accounts/1111/withdraw/1000

Example response:

    HTTP 200 OK
    {
      "id"      : 1111,
      "name"    : "account 1",
      "balance" : 1116.1,
      "currency": "EUR"
    }

#### Delete Account

Delete an account from the DB

    DELETE /accounts/1111

Example response:

    HTTP 204 No Content
        
-        
        
### Transaction
The money transfer transaction used to initialize the transaction. Once created
will be executed automatically. If transaction can not be created by some reason the Error
will be returned with details in the body.

#### Structure
    {
        "id"            : <number>,
        "fromAccount"   : <number>,
        "toAccount"     : <number>,
        "amount"        : <BigDecimal>,
        "currency"      : <Currency>,
        "status"        : <string - one from "WRONG_DATA", "SUCCESSFUL", "FAILED", "PROCESSING">,
        "description"   : <string>
    }
    
#### Create a transaction

The following creates a new transaction if possible (valid Bank Accounts and parameters should be provided).
Once `id`, `creationDate`, `updateDate` or `status` provided they  will be ignored. 
You can obtain the generated values of these fields in the response of this call. 

    POST /transactions
    {
        "fromAccount"   : 222222222,
        "toAccount"     : 1111,
        "amount"        : 16.1,
        "currency"      : "EUR"
    }
    
Example response:

    HTTP 200 OK
    {
      "id"              : 3,
      "fromAccount"     : 2222,
      "toAccount"       : 1111,
      "amount"          : 16.1,
      "currency"        : "EUR",
      "description"     : "",
      "status"          : "SUCCESSFUL"
    }

#### Get all transactions

    GET /transactions

Example response:

    HTTP 200 OK    
    [
      {
        "id"              : 0,
        "fromAccount"     : 2222,
        "toAccount"       : 1111,
        "amount"          : 12,
        "currency"        : "EUR",
        "description"     : "test transaction 1",
        "status"          : "SUCCESSFUL"
      },
      {
        "id"              : 1,
        "fromAccount"     : 3333,
        "toAccount"       : 1111,
        "amount"          : 34,
        "currency"        : "USD",
        "description"     : "test transaction 2",
        "status"          : "SUCCESSFUL"
      }
      {
        "id"              : 3,
        "fromAccount"     : 2222,
        "toAccount"       : 1111,
        "amount"          : 16.1,
        "currency"        : "EUR",
        "description"     : "",
        "status"          : "SUCCESSFUL"
      }
    ]
    
#### Get a specific transaction by its ID

    GET /transactions/1

Example response:

    HTTP 200 OK    
    {
      "id"              : 1,
      "fromAccount"     : 3333,
      "toAccount"       : 1111,
      "amount"          : 34,
      "currency"        : "USD",
      "description"     : "test transaction 2",
      "status"          : "SUCCESSFUL"
    }
 
#### Get transactions for a certain account

Check transactions where the account number provided is the source account or the destination account. 

    GET /transactions/account/2222

Example response:

    HTTP 200 OK    
    [
      {
        "id"            : 0,
        "fromAccount"   : 2222,
        "toAccount"     : 1111,
        "amount"        : 12,
        "currency"      : "EUR",
        "description"   : "test transaction 1",
        "status"        : "SUCCESSFUL"
      },
      {
        "id"            : 3,
        "fromAccount"   : 2222,
        "toAccount"     : 1111,
        "amount"        : 16.1,
        "currency"      : "EUR",
        "description"   : "",
        "status"        : "SUCCESSFUL"
      }
    ]   
    
### Exception Handing
    If any error will be thrown by some reason the Error will be returned with details in the body.

Example response:

    HTTP 404 Error
    {
      "error"   : "Transaction not found in the DB: 122",
      "code"    : 404,
      "path"        : "/transactions/122"
    }

## To do

 - Call https://api.exchangeratesapi.io to get the latest currency convertions rates when initiating a Transaction
 - Accounts and Transactions are stored in 2 LinkedHashMaps to keep it simple. Possible future integration with a NoSQL database (ex. MongoDb, Couchbase, Cassandra). https://vertx.io/docs/#data_access
 - Add an audit data store (or database table) to keep track all operations performed on accounts / transactions
 - Split the main Verticle in 2 Verticles: Accounts and Transactions...communicate through a Service Bus
 - or ...maybe change completely to a microservices architecture based on Verticles
 
