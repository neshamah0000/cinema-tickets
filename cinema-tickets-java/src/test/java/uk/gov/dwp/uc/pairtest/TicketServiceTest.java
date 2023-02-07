package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
	
	final long ID = 1L;
	
	TicketService ticketService;
	
	@Mock
	SeatReservationService seatReservationService;
	
	@Mock
	TicketPaymentService ticketPaymentService;

	@BeforeEach
	void setUp() throws Exception {
		ticketService = new TicketServiceImpl(seatReservationService, ticketPaymentService);
	}
	
	@Test
	void noneInput() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID);
		});
	}
	
	@Test
	void ticketsWithNoSeats() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID, 
					new TicketTypeRequest(Type.ADULT,0), 
					new TicketTypeRequest(Type.CHILD,0));
		});
	}
	
	// Only a maximum of 20 tickets that can be purchased at a time.
	@Test
	void over20() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID, 
					new TicketTypeRequest(Type.ADULT,11), 
					new TicketTypeRequest(Type.CHILD,10));
		});
	}
	void just20() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID, 
					new TicketTypeRequest(Type.ADULT,10), 
					new TicketTypeRequest(Type.CHILD,10));
		});
	}
	
	// One Adult's lap should can only one infant to sit
	@Test
	void tooManyInfants() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID, 
					new TicketTypeRequest(Type.ADULT,2), 
					new TicketTypeRequest(Type.INFANT,3));
		});
	}
	@Test
	void infantsCountJustFit() {
		ticketService.purchaseTickets(ID, 
				new TicketTypeRequest(Type.ADULT,2), 
				new TicketTypeRequest(Type.INFANT,2));
	}
	
	// Child and Infant tickets cannot be purchased without purchasing an Adult ticket
	@Test
	void noAdult() {
		Assertions.assertThrows(InvalidPurchaseException.class, () -> {
			ticketService.purchaseTickets(ID, 
					new TicketTypeRequest(Type.CHILD,3));
		});
	}
	
	@Test
	void purchaseTickets() {
		int infant = 2;
		int child = 3;
		int adult = 4;
		ticketService.purchaseTickets(ID,
				new TicketTypeRequest(Type.INFANT, infant),
				new TicketTypeRequest(Type.CHILD, child), 
				new TicketTypeRequest(Type.ADULT, adult));
		
		// total seat reserved = 3+4=7
		Mockito.verify(seatReservationService).reserveSeat(ID, child+adult);
		
		// total ticket price = 2*0 + 3*10 + 4*20 = 110
		Mockito.verify(ticketPaymentService).makePayment(ID, 
				infant*TicketServiceImpl.INFANT_TICKET_PRICE+
				child*TicketServiceImpl.CHILD_TICKET_PRICE+
				adult*TicketServiceImpl.ADULT_TICKET_PRICE);
	}
	
	@Test
	void purchaseTicketsWithMultiRequest() {
		int anotherAdult = 1;
		int infant = 2;
		int child = 3;
		int adult = 4;
		ticketService.purchaseTickets(ID, 
				new TicketTypeRequest(Type.ADULT, anotherAdult),
				new TicketTypeRequest(Type.INFANT, infant),
				new TicketTypeRequest(Type.CHILD, child), 
				new TicketTypeRequest(Type.ADULT, adult));
		
		// total seat reserved = 1+3+4=8
		Mockito.verify(seatReservationService).reserveSeat(ID, anotherAdult+child+adult);
		
		// total ticket price = 1*20 + 2*0 + 3*10 + 4*20 = 130
		Mockito.verify(ticketPaymentService).makePayment(ID,
				anotherAdult*TicketServiceImpl.ADULT_TICKET_PRICE+
				infant*TicketServiceImpl.INFANT_TICKET_PRICE+
				child*TicketServiceImpl.CHILD_TICKET_PRICE+
				adult*TicketServiceImpl.ADULT_TICKET_PRICE);
	}
	
	@Test
	void purchaseTicketsWithSingleRequest() {
		int adult = 4;
		ticketService.purchaseTickets(ID, 
				new TicketTypeRequest(Type.ADULT, adult));
		
		Mockito.verify(seatReservationService).reserveSeat(ID, adult);
		
		// total ticket price = 4*20 = 80
		Mockito.verify(ticketPaymentService).makePayment(ID,
				adult*TicketServiceImpl.ADULT_TICKET_PRICE);
	}
}
