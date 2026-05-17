package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

	private static final int MAX_TICKETS = 25;
	private static final BigDecimal ADULT_PRICE = new BigDecimal("25.00");
	private static final BigDecimal CHILD_PRICE = new BigDecimal("15.00");

	private final PaymentService paymentService;
	private final SeatReservationService seatReservationService;

	public CinemaTicketsServiceImpl(PaymentService paymentService,
			SeatReservationService seatReservationService) {
		this.paymentService = paymentService;
		this.seatReservationService = seatReservationService;
	}

	@Override
	public String purchaseTickets(Long accountId, TicketRequest... ticketRequests)
			throws InvalidBookingException {

		if (accountId == null || accountId <= 0) {
			throw new InvalidBookingException("Invalid account ID");
		}

		if (ticketRequests == null || ticketRequests.length == 0) {
			throw new InvalidBookingException("No tickets requested");
		}

		for (TicketRequest req : ticketRequests) {
			if (req == null || req.ticketCount() <= 0) {
				throw new InvalidBookingException("Each ticket request must have a positive count");
			}
		}

		Map<TicketType, Integer> counts = Arrays.stream(ticketRequests)
				.collect(Collectors.toMap(TicketRequest::type, TicketRequest::ticketCount, Integer::sum));

		int adults   = counts.getOrDefault(TicketType.ADULT,  0);
		int children = counts.getOrDefault(TicketType.CHILD,  0);
		int infants  = counts.getOrDefault(TicketType.INFANT, 0);

		if (adults + children + infants > MAX_TICKETS) {
			throw new InvalidBookingException("More than 25 tickets cannot be purchased at a time");
		}

		if (adults == 0) {
			throw new InvalidBookingException("Child and infant tickets require at least one Adult");
		}

		if (infants > adults) {
			throw new InvalidBookingException(
					"Number of infants cannot exceed number of adults (" + adults + ")");
		}

		BigDecimal total = ADULT_PRICE.multiply(BigDecimal.valueOf(adults))
				.add(CHILD_PRICE.multiply(BigDecimal.valueOf(children)));

		// infants don't get a seat
		long seats = adults + children;

		paymentService.debitAccount(accountId, total);
		seatReservationService.reserveSeats(accountId, seats);

		return String.format("Booked: %d adult(s), %d child(ren), %d infant(s) — £%.2f charged, %d seat(s) reserved",
				adults, children, infants, total, seats);
	}
}