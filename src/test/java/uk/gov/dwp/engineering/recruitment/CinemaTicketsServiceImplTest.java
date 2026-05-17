package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

	private final static Logger logger = Logger.getLogger(CinemaTicketsServiceImplTest.class.getName());
	
  @Mock
  private PaymentService paymentServiceMock;

  @Mock
  private SeatReservationService reservationServiceMock;

  private CinemaTicketsServiceImpl ticketService;

  @BeforeEach
  void setUp() {
      ticketService = new CinemaTicketsServiceImpl(paymentServiceMock, reservationServiceMock);
  }

  @Test
  void testSuccessfulPurchaseWithAdultAndChild() {
      TicketRequest adult = new TicketRequest(TicketType.ADULT, 2);
      TicketRequest child = new TicketRequest(TicketType.CHILD, 1);

      ticketService.purchaseTickets(1L, adult, child);

      verify(paymentServiceMock).debitAccount(1L, new BigDecimal("65.00"));
      verify(reservationServiceMock).reserveSeats(1L, 3L);
  }

  @Test
  void testInfantDoesNotIncreaseSeatsOrCost() {
      TicketRequest adult = new TicketRequest(TicketType.ADULT, 2);
      TicketRequest infant = new TicketRequest(TicketType.INFANT, 2);

      ticketService.purchaseTickets(2L, adult, infant);

      verify(paymentServiceMock).debitAccount(2L, new BigDecimal("50.00"));
      verify(reservationServiceMock).reserveSeats(2L, 2L);
  }

  @Test
  void testEmptyRequestsThrowsException() {
      assertThrows(InvalidBookingException.class, () -> ticketService.purchaseTickets(1L));

      verifyNoInteractions(paymentServiceMock);
      verifyNoInteractions(reservationServiceMock);
  }
  
  @ParameterizedTest
  @MethodSource("provideInput")
  void testExceptionScenarios(Long accountId, Integer adultCount, Integer childCOunt, Integer infantCount, String exceptionMessage) {
	  logger.log(Level.INFO, "Start of test exception Scenarios");
	  InvalidBookingException exception = null;
	  TicketRequest child = new TicketRequest(TicketType.CHILD, childCOunt);
	  TicketRequest infant = new TicketRequest(TicketType.INFANT, infantCount);
	  
	  if(adultCount != null) {
		  TicketRequest adult = new TicketRequest(TicketType.ADULT, adultCount);
		  exception = assertThrows(InvalidBookingException.class, () -> ticketService.purchaseTickets(accountId, adult, child, infant));
	  } else {
		  exception = assertThrows(InvalidBookingException.class, () -> ticketService.purchaseTickets(accountId, child, infant));
	  }
	  
	  
	  assertEquals(exceptionMessage, exception.getMessage());
	  
	  verifyNoInteractions(paymentServiceMock);
      verifyNoInteractions(reservationServiceMock);
  }
  
  private static Stream<Arguments> provideInput(){
  	return Stream.of(
  			Arguments.of(0L, 1, 1, 1, "Invalid account ID"),
  			Arguments.of(null, 1, 1, 1, "Invalid account ID"),
  			Arguments.of(1L, 0, 0, 0, "Each ticket request must have a positive count"),
  			Arguments.of(1L, 10, 15, 5, "More than 25 tickets cannot be purchased at a time"),
  			Arguments.of(1L, 1, 1, 2, "Number of infants cannot exceed number of adults (1)"),
  			Arguments.of(1L, null, 1, 1, "Child and infant tickets require at least one Adult")
  			);
  }
}
