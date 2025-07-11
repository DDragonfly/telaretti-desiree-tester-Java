package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        // GIVEN
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
        assertNotNull(ticket, "Ticket is null");
        assertNotNull(ticket.getInTime(), "In time is null");
        assertNull(ticket.getOutTime(), "Out time is null");
        System.out.println("Out time: " + ticket.getOutTime());

        int parkingSpotId = ticket.getParkingSpot().getId();
        int nextAvailableSlot = parkingSpotDAO.getNextAvailableSlot(ticket.getParkingSpot().getParkingType());
        assertNotEquals(parkingSpotId, nextAvailableSlot);
        assertFalse(ticket.getParkingSpot().isAvailable(), "Parking Spot is available");
    }

    @Test
    public void testParkingLotExit() throws Exception {
        // GIVEN
        int parkingNumber = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        ParkingSpot parkingSpot = parkingSpotDAO.getParkingSpot(parkingNumber);
        //ParkingSpot parkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        parkingSpot.setAvailable(false);
        parkingSpotDAO.updateParking(parkingSpot);
        //testParkingACar();
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 hour ago
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, true));
        ticket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(ticket);

        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        //Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //assertNotNull(ticket, "Ticket is null");

        //ticket.setOutTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
        //Date outTime = new Date();
        //ticket.setOutTime(outTime);
        //ticketDAO.updateTicket(ticket);
        //ticketDAO.saveTicket(ticket);
        parkingService.processExitingVehicle();
        //ticketDAO.saveTicket(ticket);

        ticket = ticketDAO.getTicket("ABCDEF");
        assertTrue(ticket.getPrice() > 0.0, "Ticket price is not ok");
        assertNotNull(ticket.getInTime(), "In time is null");
        assertNotNull(ticket.getOutTime(), "Out time is null");
        System.out.println("Out time: " + ticket.getOutTime());

        //ParkingSpot parkingSpot = ticket.getParkingSpot();
        System.out.println(parkingSpot.isAvailable());
        //ParkingSpot parkingSpot = updatedTicket.getParkingSpot();
        //parkingSpot = parkingSpotDAO.getNextAvailableSlot(parkingSpot.getId());
      //  assertTrue(parkingSpot.isAvailable(), "Parking Spot is available");
        ParkingSpot updatedSpot = parkingSpotDAO.getParkingSpot(parkingSpot.getId());
        assertTrue(updatedSpot.isAvailable(),"Parking Spot is not available");

       // ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));

       // ticketDAO.saveTicket(ticket);
        //ticketDAO.updateTicket(ticket);

        // WHEN
        //parkingService.processExitingVehicle();

        // THEN
       // Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
/*
        System.out.println("IN-TIME: " + updatedTicket.getInTime());
        System.out.println("OUT-TIME: " + updatedTicket.getOutTime());
        System.out.println("PRICE: " + updatedTicket.getPrice());

        assertNotNull(updatedTicket.getOutTime());
        assertTrue(updatedTicket.getPrice() > 0);
        //assertTrue(updatedTicket.getParkingSpot().isAvailable(), "Parking spot not updated to available"); */
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // GIVEN : Simulo un utilisateur récurrent
        String vehicleRegNumber = "ABCDEF";

        // Prima entrata per renderlo ricorrente
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processIncomingVehicle();  // 1° passaggio
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 ora fa
        ticket.setInTime(inTime);
        ticketDAO.updateTicket(ticket);
        //ticketDAO.saveTicket(ticket);

        // Faccio uscire subito per creare lo storico
        parkingService.processExitingVehicle();

        // Aspetto un po’ (o setto inTime manuale)
        inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 ora fa
        Date outTime = new Date();

        // Nuova entrata => ora è récurrent
        parkingService.processIncomingVehicle();
        ticket = ticketDAO.getTicket(vehicleRegNumber);
        ticket.setInTime(inTime);

        // Simulo uscita
        parkingService.processExitingVehicle();
        Ticket updatedTicket = ticketDAO.getTicket(vehicleRegNumber);

        // WHEN : Calcolo du prix
        double duration = (updatedTicket.getOutTime().getTime() - updatedTicket.getInTime().getTime()) / (1000 * 60 * 60);
        double expectedPriceWithoutDiscount = duration * Fare.CAR_RATE_PER_HOUR;
        double expectedPriceWithDiscount = expectedPriceWithoutDiscount * 0.95;

        // THEN : Vérifier la remise
        assertEquals(expectedPriceWithDiscount, updatedTicket.getPrice(), 0.01);
    }


}
