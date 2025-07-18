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

        parkingSpot.setAvailable(false);
        parkingSpotDAO.updateParking(parkingSpot);

        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 hour ago
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, true));
        ticket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(ticket);

        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        parkingService.processExitingVehicle();

        ticket = ticketDAO.getTicket("ABCDEF");
        assertTrue(ticket.getPrice() > 0.0, "Ticket price is not ok");
        assertNotNull(ticket.getInTime(), "In time is null");
        assertNotNull(ticket.getOutTime(), "Out time is null");
        System.out.println("Out time: " + ticket.getOutTime());

        System.out.println(parkingSpot.isAvailable());

        ParkingSpot updatedSpot = parkingSpotDAO.getParkingSpot(parkingSpot.getId());
        assertTrue(updatedSpot.isAvailable(),"Parking Spot is not available");
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // GIVEN : simulation utilisateur récurrent
        String vehicleRegNumber = "ABCDEF";

        // Premiére entrée
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processIncomingVehicle();  // 1° passage
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant
        ticket.setInTime(inTime);
        ticketDAO.updateTicket(ticket);

        // Sortie pour creer une hystorique
        parkingService.processExitingVehicle();

        // changement d'heure
        inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant
        Date outTime = new Date();

        // Nouvelle entrée - vehicule recurrent
        parkingService.processIncomingVehicle();
        ticket = ticketDAO.getTicket(vehicleRegNumber);
        ticket.setInTime(inTime);

        // Simulation sortie
        parkingService.processExitingVehicle();
        Ticket updatedTicket = ticketDAO.getTicket(vehicleRegNumber);

        // WHEN : calcul du prix
        double duration = (updatedTicket.getOutTime().getTime() - updatedTicket.getInTime().getTime()) / (1000 * 60 * 60);
        double expectedPriceWithoutDiscount = duration * Fare.CAR_RATE_PER_HOUR;
        double expectedPriceWithDiscount = expectedPriceWithoutDiscount * 0.95;

        // THEN : Vérifier la remise
        assertEquals(expectedPriceWithDiscount, updatedTicket.getPrice(), 0.01);
    }


}
