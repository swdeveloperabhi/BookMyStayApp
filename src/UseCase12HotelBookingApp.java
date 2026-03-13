import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/* -------------------- ROOM CLASSES -------------------- */

abstract class Room implements Serializable {

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
        super(1, 250, 1500);
    }

    public String getRoomType() {
        return "Single Room";
    }
}

class DoubleRoom extends Room {

    public DoubleRoom() {
        super(2, 400, 2500);
    }

    public String getRoomType() {
        return "Double Room";
    }
}

class SuiteRoom extends Room {

    public SuiteRoom() {
        super(3, 750, 5000);
    }

    public String getRoomType() {
        return "Suite Room";
    }
}

/* -------------------- INVENTORY -------------------- */

class RoomInventory implements Serializable {

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

/* -------------------- BOOKING REQUEST -------------------- */

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

/* -------------------- RESERVATION -------------------- */

class Reservation implements Serializable {

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

/* -------------------- BOOKING HISTORY -------------------- */

class BookingHistory implements Serializable {

    private List<Reservation> reservations = new ArrayList<>();

    public synchronized void addReservation(Reservation r) {
        reservations.add(r);
    }

    public List<Reservation> getAllReservations() {
        return reservations;
    }
}

/* -------------------- BOOKING PROCESSOR (THREAD) -------------------- */

class BookingProcessor implements Runnable {

    private Queue<BookingRequest> queue;
    private RoomInventory inventory;
    private BookingHistory history;
    private Set<String> processedIds;

    public BookingProcessor(
            Queue<BookingRequest> queue,
            RoomInventory inventory,
            BookingHistory history,
            Set<String> processedIds) {

        this.queue = queue;
        this.inventory = inventory;
        this.history = history;
        this.processedIds = processedIds;
    }

    public void run() {

        while (true) {

            BookingRequest request = queue.poll();

            if (request == null)
                break;

            synchronized (processedIds) {

                if (processedIds.contains(request.reservationId))
                    continue;

                boolean allocated = inventory.allocateRoom(request.roomType);

                if (allocated) {

                    processedIds.add(request.reservationId);

                    Reservation r = new Reservation(
                            request.reservationId,
                            request.guestName,
                            request.roomType);

                    history.addReservation(r);

                    System.out.println(Thread.currentThread().getName()
                            + " booked "
                            + request.roomType
                            + " for "
                            + request.guestName);

                } else {

                    System.out.println(Thread.currentThread().getName()
                            + " FAILED booking for "
                            + request.guestName
                            + " (Sold Out)");
                }
            }
        }
    }
}

/* -------------------- PERSISTENCE STATE -------------------- */

class SystemState implements Serializable {

    public RoomInventory inventory;
    public BookingHistory history;

    public SystemState(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
    }
}

/* -------------------- PERSISTENCE SERVICE -------------------- */

class PersistenceService {

    private static final String FILE_NAME = "hotel_state.ser";

    public void save(RoomInventory inventory, BookingHistory history) {

        try {

            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(FILE_NAME));

            SystemState state = new SystemState(inventory, history);

            out.writeObject(state);

            out.close();

            System.out.println("\nSystem state saved to file.");

        } catch (Exception e) {

            System.out.println("Error saving system state.");
        }
    }

    public SystemState load() {

        try {

            File file = new File(FILE_NAME);

            if (!file.exists()) {

                System.out.println("No previous state found. Starting fresh.");
                return null;
            }

            ObjectInputStream in =
                    new ObjectInputStream(new FileInputStream(FILE_NAME));

            SystemState state = (SystemState) in.readObject();

            in.close();

            System.out.println("System state loaded from file.");

            return state;

        } catch (Exception e) {

            System.out.println("State file corrupted. Starting fresh.");

            return null;
        }
    }
}

/* -------------------- MAIN APPLICATION -------------------- */

public class UseCase12HotelBookingApp {

    public static void main(String[] args) {

        PersistenceService persistence = new PersistenceService();

        RoomInventory inventory;
        BookingHistory history;

        SystemState state = persistence.load();

        if (state != null) {

            inventory = state.inventory;
            history = state.history;

        } else {

            inventory = new RoomInventory();
            history = new BookingHistory();
        }

        Queue<BookingRequest> queue = new ConcurrentLinkedQueue<>();

        Set<String> processedIds =
                Collections.synchronizedSet(new HashSet<>());

        /* Sample booking requests */

        queue.add(new BookingRequest("R1", "Single Room", "Alice"));
        queue.add(new BookingRequest("R2", "Single Room", "Bob"));
        queue.add(new BookingRequest("R3", "Double Room", "Charlie"));
        queue.add(new BookingRequest("R4", "Suite Room", "David"));
        queue.add(new BookingRequest("R5", "Single Room", "Eve"));

        Thread t1 = new Thread(
                new BookingProcessor(queue, inventory, history, processedIds),
                "Thread-1");

        Thread t2 = new Thread(
                new BookingProcessor(queue, inventory, history, processedIds),
                "Thread-2");

        Thread t3 = new Thread(
                new BookingProcessor(queue, inventory, history, processedIds),
                "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        try {

            t1.join();
            t2.join();
            t3.join();

        } catch (Exception e) {
        }

        /* Print booking history */

        System.out.println("\nBooking History:");

        for (Reservation r : history.getAllReservations()) {

            System.out.println(
                    r.getReservationId()
                            + " | "
                            + r.getGuestName()
                            + " | "
                            + r.getRoomType());
        }

        /* Print inventory */

        System.out.println("\nRemaining Inventory:");

        System.out.println("Single Room: "
                + inventory.getAvailability("Single Room"));

        System.out.println("Double Room: "
                + inventory.getAvailability("Double Room"));

        System.out.println("Suite Room: "
                + inventory.getAvailability("Suite Room"));

        /* Save system state */

        persistence.save(inventory, history);
    }
}