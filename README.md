# Cinema Tickets Code Test

## What I did

Implemented CinemaTicketsServiceImpl in the existing package — no new packages needed.

1.  Input validation - Account Id > 0, ticketRequests not null, ticketCount not null and greater than 0
2. final count is fetched for adult, child and infant
3. Validation done for max tickets, ticket without adult or infant greater than adult
4. Price calculated and payment service called
5. Seat reservation service called 	

	
Assumptions - 
1. Payment service will not fail and no refund required
2. No idempotency handling — duplicate requests (same account, same tickets) are treated as  separate transactions
3. No authentication validation
4. Account ID can not be 0
