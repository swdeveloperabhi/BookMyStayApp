import java.util.*;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

abstract class Room {

    private int beds;
    private int size;
    private double price;

    public Room(int beds, int size, double price) {
        this.beds = beds;
        this.size = size;
        this.price = price;
    }

    public int getBeds() {
        return beds;
    }

    public int getSize() {
        return size;
    }

    public double getPrice() {
        return price;
    }

    public abstract String getRoomType();
}
class SingleRoom extends Room {

    public SingleRoom() {
        super(1, 250, 1500.0);
    }

    public String getRoomType() {
        return "Single Room";
    }
}
class DoubleRoom extends Room {

    public DoubleRoom() {
        super(2, 400, 2500.0);
    }

    public String getRoomType() {
        return "Double Room";
    }
}
class SuiteRoom extends Room {

    public SuiteRoom() {
        super(3, 750, 5000.0);
    }

    public String getRoomType() {
        return "Suite Room";
    }
}

class RoomInventory {

    private HashMap<String, Integer> inventory = new HashMap<>();

    public RoomInventory() {
        inventory.put("Single Room", 5);
        inventory.put("Double Room", 3);
        inventory.put("Suite Room", 2);
    }

    public int getAvailability(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }

    public void decrementRoom(String roomType) {
        int count = inventory.get(roomType);
        inventory.put(roomType, count - 1);
    }
}


class RoomSearchService {

    private RoomInventory inventory;

    public RoomSearchService(RoomInventory inventory) {
        this.inventory = inventory;
    }

    public void searchAvailableRooms(List<Room> rooms) {

        System.out.println("Available Rooms:\n");

        for (Room room : rooms) {

            int available = inventory.getAvailability(room.getRoomType());

            // Defensive check
            if (available > 0) {

                System.out.println(room.getRoomType() + ":");
                System.out.println("Beds: " + room.getBeds());
                System.out.println("Size: " + room.getSize() + " sqft");
                System.out.println("Price per night: " + room.getPrice());
                System.out.println("Available: " + available);
                System.out.println();
            }
        }
    }
}
class Reservation {

    private String guestName;
    private String roomType;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomType() {
        return roomType;
    }
}



class BookingRequestQueue {

    private Queue<Reservation> queue = new LinkedList<>();

    public void addRequest(Reservation reservation) {
        queue.offer(reservation);
    }

    public Reservation getNextRequest() {
        return queue.poll();
    }

    public boolean hasRequests() {
        return !queue.isEmpty();
    }
}
class BookingService {

    private RoomInventory inventory;

    private HashMap<String, Set<String>> allocatedRooms = new HashMap<>();
    private Set<String> allRoomIds = new HashSet<>();

    public BookingService(RoomInventory inventory) {
        this.inventory = inventory;
    }

    public void processBookings(BookingRequestQueue queue) {

        while (queue.hasRequests()) {

            Reservation request = queue.getNextRequest();
            String roomType = request.getRoomType();

            int available = inventory.getAvailability(roomType);

            if (available > 0) {

                String roomId = generateRoomId(roomType);

                // prevent duplicates
                if (!allRoomIds.contains(roomId)) {

                    allRoomIds.add(roomId);

                    allocatedRooms
                            .computeIfAbsent(roomType, k -> new HashSet<>())
                            .add(roomId);

                    inventory.decrementRoom(roomType);

                    System.out.println("Reservation confirmed for "
                            + request.getGuestName()
                            + " → " + roomType
                            + " (Room ID: " + roomId + ")");
                }

            } else {

                System.out.println("Reservation failed for "
                        + request.getGuestName()
                        + " → No " + roomType + " available");
            }
        }
    }

    private String generateRoomId(String roomType) {

        String prefix;

        switch (roomType) {
            case "Single Room": prefix = "S"; break;
            case "Double Room": prefix = "D"; break;
            default: prefix = "SU";
        }

        return prefix + "-" + (allRoomIds.size() + 1);
    }
}

class AddOnService {

    private String serviceName;
    private double price;

    public AddOnService(String serviceName, double price) {
        this.serviceName = serviceName;
        this.price = price;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getPrice() {
        return price;
    }
}

class AddOnServiceManager {

    private Map<String, List<AddOnService>> reservationServices = new HashMap<>();

    public void addService(String reservationId, AddOnService service) {

        reservationServices
                .computeIfAbsent(reservationId, k -> new ArrayList<>())
                .add(service);

        System.out.println(service.getServiceName()
                + " added to reservation " + reservationId);
    }

    public void showServices(String reservationId) {

        List<AddOnService> services = reservationServices.get(reservationId);

        if (services == null) {
            System.out.println("No services selected.");
            return;
        }

        System.out.println("\nServices for reservation " + reservationId);

        for (AddOnService s : services) {
            System.out.println("- " + s.getServiceName()
                    + " : ₹" + s.getPrice());
        }
    }

    public double calculateServiceCost(String reservationId) {

        List<AddOnService> services = reservationServices.get(reservationId);

        if (services == null) {
            return 0;
        }

        double total = 0;

        for (AddOnService s : services) {
            total += s.getPrice();
        }

        return total;
    }
}

public class UseCase7HotelBookingApp {

    public static void main(String[] args) {

        String reservationId = "S-101";

        AddOnService breakfast = new AddOnService("Breakfast", 500);
        AddOnService airportPickup = new AddOnService("Airport Pickup", 1200);
        AddOnService spa = new AddOnService("Spa Access", 2000);

        AddOnServiceManager manager = new AddOnServiceManager();

        manager.addService(reservationId, breakfast);
        manager.addService(reservationId, airportPickup);
        manager.addService(reservationId, spa);

        manager.showServices(reservationId);

        double totalCost = manager.calculateServiceCost(reservationId);

        System.out.println("\nTotal Add-On Cost: ₹" + totalCost);
    }
}