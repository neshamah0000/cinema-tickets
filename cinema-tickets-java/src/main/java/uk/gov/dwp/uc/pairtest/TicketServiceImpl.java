package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
	
	private final SeatReservationService seatReservationService;
	private final TicketPaymentService ticketPaymentService;
	
	static int INFANT_TICKET_PRICE = 0;
	static int CHILD_TICKET_PRICE = 10;
	static int ADULT_TICKET_PRICE = 20;
	

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService) {
		super();
		this.seatReservationService = seatReservationService;
		this.ticketPaymentService = ticketPaymentService;
	}

	@Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
    	if (ticketTypeRequests==null||ticketTypeRequests.length==0) throw new InvalidPurchaseException();
		
		int totalInfant = 0;
    	int totalChild = 0;
    	int totalAdult = 0;
    	int totalAmountToPay = 0;
    	for (TicketTypeRequest ticketTypeRequest: Arrays.asList(ticketTypeRequests)) {
    		int noOfTickets = ticketTypeRequest.getNoOfTickets();
    		switch (ticketTypeRequest.getTicketType()) {
    		case INFANT -> totalInfant += noOfTickets; // Infants are not allocated a seat
    		case CHILD -> totalChild += noOfTickets;
    		case ADULT -> totalAdult += noOfTickets;
    		}
    		totalAmountToPay+=ticketPrice(ticketTypeRequest);
    	}
    	
    	// There should be at least one seat to be purchased.
    	// Only a maximum of 20 tickets that can be purchased at a time.
    	int total = totalInfant+totalChild+totalAdult;
    	if (total==0 || total>20) throw new InvalidPurchaseException();
    	
    	// Infants will be sitting on an Adult's lap. i.e. One Adult's lap should can only one infant to sit
    	if (totalInfant>totalAdult) throw new InvalidPurchaseException();
    	
    	// Child and Infant tickets cannot be purchased without purchasing an Adult ticket.
    	if (totalAdult==0) throw new InvalidPurchaseException();
    	
    	seatReservationService.reserveSeat(accountId, totalChild+totalAdult);
    	ticketPaymentService.makePayment(accountId, totalAmountToPay);
    }
    
    // The ticketPrice should be fetched from Database in real case. Here we just use pseudo method to simulate it 
    private int ticketPrice(TicketTypeRequest ticketTypeRequest) {
    	int price = 0;
    	switch (ticketTypeRequest.getTicketType()) {
    		case INFANT -> price = INFANT_TICKET_PRICE;
    		case CHILD -> price = CHILD_TICKET_PRICE;
    		case ADULT -> price = ADULT_TICKET_PRICE;
    	}
    	return price*ticketTypeRequest.getNoOfTickets();
    }

}
