package eapli.aisafe.flightPlan.application;

import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.fleet.domain.Aircraft;
import eapli.aisafe.fleet.domain.AircraftModel;
import eapli.aisafe.fleet.domain.AircraftOperationalStatus;
import eapli.aisafe.fleet.repositories.AircraftModelRepository;
import eapli.aisafe.fleet.repositories.AircraftRepository;
import eapli.aisafe.flightPlan.repositories.FlightPlanRepository;
import eapli.aisafe.flightroute.FlightRoute;
import eapli.aisafe.flightroute.RouteActivationStatus;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.Pilot;
import eapli.aisafe.pilot.PilotStatus;
import eapli.aisafe.pilot.repositories.PilotRepository;

public class FlightPlanFactory {
    private FlightPlanRepository flightPlanRepository;
    private AirTransportCompanyRepository airTransportCompanyRepository;
    private PilotRepository pilotRepository;
    private FlightRouteRepository flightRouteRepository;
    private AircraftRepository aircraftRepository;
    private AircraftModelRepository aircraftModelRepository;

    public FlightPlanFactory(AircraftModelRepository aircraftModelRepository, AircraftRepository aircraftRepository, FlightPlanRepository flightPlanRepository, AirTransportCompanyRepository airTransportCompanyRepository, PilotRepository pilotRepository, FlightRouteRepository flightRouteRepository) {
        this.flightPlanRepository = flightPlanRepository;
        this.airTransportCompanyRepository = airTransportCompanyRepository;
        this.pilotRepository = pilotRepository;
        this.flightRouteRepository = flightRouteRepository;
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;

    }
    public Boolean validatePreconditions(FlightPlanDTO flightPlanDTO) {
        if (flightPlanDTO == null) {
            System.out.println("FLIGHT PLAN VALIDATION FAILED");
            return false;
        }
        if (flightPlanDTO.designator() == null) {
            System.out.println("FLIGHT PLAN VALIDATION FAILED");
            return false;
        }
        if(!designatorGetLetters(flightPlanDTO).equals(getCompanyIataCodeLetters(flightPlanDTO))) {
            System.out.println("Wrong designator format");
            return false;
        }
        if(!validateLegSequence(flightPlanDTO)) {
            System.out.println("Wrong leg sequence");
            return false;
        }
        if(!uniqAirport(flightPlanDTO)) {
            System.out.println("Repeated airport in legs");
            return false;
        }
        if(!routeIsActive(flightPlanDTO)) {
            System.out.println("Route is not active");
            return false;
        }
        if(!routeCompanySamaAsPilotCompany(flightPlanDTO)) {
            System.out.println("Route company is different from pilot company");
            return false;
        }
        if(!validateAircraftState(flightPlanDTO)) {
            System.out.println("Aircraft is not active");
            return false;
        }
        if(!pilotIsActive(flightPlanDTO)) {
            System.out.println("Pilot is not active");
            return false;
        }
        if(!validateTotalFuelLowerThanTotalForModel(flightPlanDTO)) {
            System.out.println("Total fuel exceeds aircraft model capacity");
            return false;
        }
        return true;
    }
    private String designatorGetLetters(FlightPlanDTO flightPlanDTO) {
        String designator = flightPlanDTO.designator();
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < designator.length(); i++) {
            if (Character.isLetter(designator.charAt(i))) {
                res.append(designator.charAt(i));
            } else {
                break;
            }
        }
        return res.toString();
    }
    private String getCompanyIataCodeLetters(FlightPlanDTO flightPlanDTO) {
        StringBuilder IataCodeLetters = new StringBuilder();
        Pilot pilot = pilotRepository.ofIdentity(flightPlanDTO.pilotId()).orElseThrow(() -> new IllegalArgumentException("Pilot not found"));
        String companyIataCode = airTransportCompanyRepository.ofIdentity(pilot.getCompany()).orElseThrow(() -> new IllegalArgumentException("Air transport company not found")).iataCode().value();
        for(int i=0;i<2;i++) {
            IataCodeLetters.append(companyIataCode.charAt(i));
        }
        return IataCodeLetters.toString();
    }
    private boolean validateLegSequence(FlightPlanDTO flightPlanDTO) {
        for(int i =0; i<flightPlanDTO.legs().size();i++) {
            if(!flightPlanDTO.legs().get(i).arrivalAirport().equals(flightPlanDTO.legs().get(i+1).departureAirport())) {
                return false;
            }
        }
        return true;
    }
    private boolean uniqAirport(FlightPlanDTO flightPlanDTO) {
        for(int i =0; i<flightPlanDTO.legs().size();i++) {
            for(int j =i+1; j<flightPlanDTO.legs().size();j++) {
                if(flightPlanDTO.legs().get(i).departureAirport().equals(flightPlanDTO.legs().get(j).departureAirport()) ||
                        flightPlanDTO.legs().get(i).arrivalAirport().equals(flightPlanDTO.legs().get(j).arrivalAirport())) {
                    return false;
                }
            }
        }

        return true;
    }
    private boolean routeIsActive(FlightPlanDTO flightPlanDTO) {
        FlightRoute route = flightRouteRepository.findById(flightPlanDTO.flightRouteId()).orElseThrow(() -> new IllegalArgumentException("Flight route not found"));
        if(route.getStatus() != RouteActivationStatus.ACTIVE) {
            return false;
        }
        return true;
    }
    private boolean routeCompanySamaAsPilotCompany(FlightPlanDTO flightPlanDTO) {
        FlightRoute route = flightRouteRepository.findById(flightPlanDTO.flightRouteId()).orElseThrow(() -> new IllegalArgumentException("Flight route not found"));
        Pilot pilot = pilotRepository.ofIdentity(flightPlanDTO.pilotId()).orElseThrow(() -> new IllegalArgumentException("Pilot not found"));
        if(!route.companyId().equals(pilot.getCompany())) {
            return false;
        }
        return true;
    }
    private boolean validateAircraftState(FlightPlanDTO flightPlanDTO) {
        Aircraft aircraft = aircraftRepository.findByRegistration(flightPlanDTO.aircraftReg()).orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
        if(aircraft.operationalStatus() != AircraftOperationalStatus.ACTIVE) {
            return false;
        }
        return true;
    }
    private boolean pilotIsActive(FlightPlanDTO flightPlanDTO) {
        Pilot pilot = pilotRepository.ofIdentity(flightPlanDTO.pilotId()).orElseThrow(() -> new IllegalArgumentException("Pilot not found"));
        if(pilot.getStatus() != PilotStatus.ACTIVE) {
            return false;
        }
        return true;
    }
    private boolean validateTotalFuelLowerThanTotalForModel(FlightPlanDTO flightPlanDTO) {
        Aircraft aircraft = aircraftRepository.findByRegistration(flightPlanDTO.aircraftReg()).orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));
        double totalFuel = flightPlanDTO.legs().stream().mapToDouble(leg -> leg.fuelPlan().totalFuel()).sum();
        AircraftModel model = aircraftModelRepository.findById(aircraft.aircraftModelId()).orElseThrow(() -> new IllegalArgumentException("Aircraft model not found"));
        if(model.maxFuelCapacityLiters() < totalFuel) {
            return false;
        }
        return true;
    }

}
