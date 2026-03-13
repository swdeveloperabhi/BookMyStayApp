import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private Map<String, Integer> inventory = new HashMap<>();

    public RoomInventory() {
        inventory.put("Single Room", 5);
        inventory.put("Double Room", 3);
        inventory.put("Suite Room", 2);
    }

    public synchronized boolean allocateRoom(String roomType) {

        int available = inventory.getOrDefault(roomType, 0);

        if (available > 0) {
            inventory.put(roomType, available - 1);
            return true;
        }

        return false;
    }

    public synchronized void releaseRoom(String roomType) {

        int count = inventory.getOrDefault(roomType, 0);
        inventory.put(roomType, count + 1);
    }

    public synchronized int getAvailability(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }
}

class BookingRequest {

    String reservationId;
    String roomType;
    String guestName;

    public BookingRequest(String reservationId, String roomType, String guestName) {
        this.reservationId = reservationId;
        this.roomType = roomType;
        this.guestName = guestName;
    }
}

class Reservation {

    private String reservationId;
    private String guestName;
    private String roomType;

    public Reservation(String reservationId, String guestName, String roomType) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomType() {
        return roomType;
    }
}

class BookingHistory {

    private List<Reservation> confirmedBookings = new ArrayList<>();

    public synchronized void addReservation(Reservation reservation) {
        confirmedBookings.add(reservation);
    }

    public List<Reservation> getAllReservations() {
        return confirmedBookings;
    }

    public void markCancelled(String reservationId) {

        for (Reservation r : confirmedBookings) {

            if (r.getReservationId().equals(reservationId)) {
                System.out.println("Booking history updated: "
                        + reservationId + " marked as cancelled.");
            }
        }
    }
}

class BookingProcessor implements Runnable {

    private Queue<BookingRequest> requestQueue;
    private RoomInventory inventory;
    private Set<String> allocatedIds;
    private BookingHistory history;

    public BookingProcessor(
            Queue<BookingRequest> requestQueue,
            RoomInventory inventory,
            Set<String> allocatedIds,
            BookingHistory history) {

        this.requestQueue = requestQueue;
        this.inventory = inventory;
        this.allocatedIds = allocatedIds;
        this.history = history;
    }

    @Override
    public void run() {

        while (true) {

            BookingRequest request = requestQueue.poll();

            if (request == null) {
                break;
            }

            synchronized (allocatedIds) {

                if (!allocatedIds.contains(request.reservationId)) {

                    boolean allocated = inventory.allocateRoom(request.roomType);

                    if (allocated) {

                        allocatedIds.add(request.reservationId);

                        history.addReservation(
                                new Reservation(
                                        request.reservationId,
                                        request.guestName,
                                        request.roomType));

                        System.out.println(
                                Thread.currentThread().getName()
                                        + " booked "
                                        + request.roomType
                                        + " for "
                                        + request.guestName);

                    } else {

                        System.out.println(
                                Thread.currentThread().getName()
                                        + " failed to book "
                                        + request.roomType
                                        + " for "
                                        + request.guestName
                                        + " (Sold Out)");
                    }
                }
            }
        }
    }
}

class RoomSearchService {

    private RoomInventory inventory;

    public RoomSearchService(RoomInventory inventory) {
        this.inventory = inventory;
    }

    public void searchAvailableRooms(List<Room> rooms) {

        System.out.println("\nAvailable Rooms:\n");

        for (Room room : rooms) {

            int available = inventory.getAvailability(room.getRoomType());

            if (available > 0) {

                System.out.println(room.getRoomType());
                System.out.println("Beds: " + room.getBeds());
                System.out.println("Size: " + room.getSize() + " sqft");
                System.out.println("Price per night: ₹" + room.getPrice());
                System.out.println("Available: " + available);
                System.out.println();
            }
        }
    }
}

class BookingReportService {

    public void printAllBookings(BookingHistory history) {

        System.out.println("\nConfirmed Booking History:\n");

        for (Reservation r : history.getAllReservations()) {

            System.out.println(
                    "Reservation ID: " + r.getReservationId()
                            + " | Guest: " + r.getGuestName()
                            + " | Room: " + r.getRoomType());
        }
    }

    public void generateSummary(BookingHistory history) {

        int totalBookings = history.getAllReservations().size();

        System.out.println("\nBooking Summary Report");
        System.out.println("Total Confirmed Bookings: " + totalBookings);
    }
}

class CancellationService {

    private RoomInventory inventory;
    private Set<String> activeReservations;
    private Map<String, String> reservationToRoomType;
    private Stack<String> releasedRoomIds = new Stack<>();
    private BookingHistory history;

    public CancellationService(
            RoomInventory inventory,
            Set<String> activeReservations,
            Map<String, String> reservationToRoomType,
            BookingHistory history) {

        this.inventory = inventory;
        this.activeReservations = activeReservations;
        this.reservationToRoomType = reservationToRoomType;
        this.history = history;
    }

    public void cancelReservation(String reservationId) {

        if (!activeReservations.contains(reservationId)) {

            System.out.println("Cancellation failed: " + reservationId);
            return;
        }

        String roomType = reservationToRoomType.get(reservationId);

        inventory.releaseRoom(roomType);

        releasedRoomIds.push(reservationId);

        activeReservations.remove(reservationId);

        history.markCancelled(reservationId);

        System.out.println("Cancellation successful for "
                + reservationId + " (" + roomType + ")");
    }

    public void showReleasedRoomIds() {

        System.out.println("Recently released Room IDs (LIFO): "
                + releasedRoomIds);
    }
}

public class UseCase11HotelBookingApp {

    public static void main(String[] args) {

        RoomInventory inventory = new RoomInventory();

        BookingHistory history = new BookingHistory();

        Queue<BookingRequest> queue = new ConcurrentLinkedQueue<>();

        Set<String> allocatedIds = Collections.synchronizedSet(new HashSet<>());

        List<Room> rooms = Arrays.asList(
                new SingleRoom(),
                new DoubleRoom(),
                new SuiteRoom()
        );

        RoomSearchService searchService = new RoomSearchService(inventory);

        searchService.searchAvailableRooms(rooms);

        queue.add(new BookingRequest("S-1", "Single Room", "Alice"));
        queue.add(new BookingRequest("S-2", "Single Room", "Bob"));
        queue.add(new BookingRequest("S-3", "Single Room", "Charlie"));
        queue.add(new BookingRequest("S-4", "Single Room", "David"));
        queue.add(new BookingRequest("D-1", "Double Room", "Eve"));
        queue.add(new BookingRequest("SU-1", "Suite Room", "Frank"));

        Thread t1 = new Thread(new BookingProcessor(queue, inventory, allocatedIds, history), "Thread-1");
        Thread t2 = new Thread(new BookingProcessor(queue, inventory, allocatedIds, history), "Thread-2");
        Thread t3 = new Thread(new BookingProcessor(queue, inventory, allocatedIds, history), "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        try {

            t1.join();
            t2.join();
            t3.join();

        } catch (InterruptedException e) {

            e.printStackTrace();
        }

        BookingReportService reportService = new BookingReportService();

        reportService.printAllBookings(history);

        reportService.generateSummary(history);

        System.out.println("\nFinal Inventory:");

        System.out.println("Single Room: "
                + inventory.getAvailability("Single Room"));

        System.out.println("Double Room: "
                + inventory.getAvailability("Double Room"));

        System.out.println("Suite Room: "
                + inventory.getAvailability("Suite Room"));
    }
}