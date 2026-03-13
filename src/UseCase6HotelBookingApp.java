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

public class UseCase6HotelBookingApp {

    public static void main(String[] args) {

        RoomInventory inventory = new RoomInventory();
        BookingRequestQueue queue = new BookingRequestQueue();

        // Guest booking requests
        queue.addRequest(new Reservation("Alice", "Single Room"));
        queue.addRequest(new Reservation("Bob", "Double Room"));
        queue.addRequest(new Reservation("Charlie", "Suite Room"));
        queue.addRequest(new Reservation("David", "Suite Room"));

        BookingService bookingService = new BookingService(inventory);

        bookingService.processBookings(queue);
    }
}