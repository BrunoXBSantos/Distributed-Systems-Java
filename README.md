# Distributed-Systems-Java
Server that allows to collect estimates of the proportion of infected in the pandemic - University of Minho

## Description
This project was proposed within the scope of the distributed systems course taught at the University of Minho.
The project consists in the development of a server that allows to collect estimates of the proportion of infected in a pandemic. Communication is line oriented. Each client must register (step only once per client) and authenticate to connect to the server. Then, the server waits for the client to indicate how many cases of illness he knows in his contacts (integer value> = 0). Whenever a client provides information to the server, the server sends a new estimate of the average proportion to all connected clients. (For this calculation it is assumed that each customer knows 150 contacts, so a customer that indicates 15 cases, is indicating the proportion of 0.1. The server reports the arithmetic average of the proportions received). Customers can indicate values ​​of cases of illness as many times as they want, until they close the connection.

It is important to note that notifications are asynchronous and clients should receive these messages even without making requests to the server. In other words, it should not be assumed that the server only sends global estimates in response to a new remote operation from the client.

In addition, the work must consider the authentication and registration of users, given their name and password. Whenever a user wishes to interact with the service, he must establish a connection and be authenticated by the server.

As a client program you can use "nc" or, if you prefer, develop your own program.

### Notes:

- Each client machine knows the server's IP and Port.
- Must use programming primitives with sockets, concurrent programming and control of local competition whenever necessary.
- The Java programming language must be used.
