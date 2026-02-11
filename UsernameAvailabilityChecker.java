import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameAvailabilityChecker {


    private final ConcurrentHashMap<String, String> usernameToUserId;



    private final ConcurrentHashMap<String, AtomicInteger> attemptFrequency;


    public UsernameAvailabilityChecker() {
        usernameToUserId = new ConcurrentHashMap<>();

        attemptFrequency = new ConcurrentHashMap<>();
    }


    public boolean registerUser(String username, String userId) {
        if (usernameToUserId.putIfAbsent(username, userId) == null) {
            return true;
        }

        return false;

    }


    public boolean checkAvailability(String username) {

               attemptFrequency
                .computeIfAbsent(username, k -> new AtomicInteger(0))

                .incrementAndGet();

        return !usernameToUserId.containsKey(username);

    }


    public List<String> suggestAlternatives(String username) {
                    List<String>suggestions=new ArrayList<>();

                      int counter = 1;


        while (suggestions.size() < 3) {

            String suggestion=username+counter;

            if (!usernameToUserId.containsKey(suggestion)) {

                suggestions.add(suggestion);
            }
            counter++;
        }


        if (username.contains("_")) {

            String modified = username.replace("_", ".");
            if (!usernameToUserId.containsKey(modified)) {
                suggestions.add(modified);
            }
        }


        return suggestions;
    }


    public String getMostAttempted() {
        String mostAttempted=null;
        int max=0;


        for (Map.Entry<String,AtomicInteger>entry:attemptFrequency.entrySet()) {
            int count=entry.getValue().get();
            if (count>max) {
                max=count;
                mostAttempted=entry.getKey();
            }
        }

        return mostAttempted;
    }


    public static void main(String[] args) {
        UsernameAvailabilityChecker checker=new UsernameAvailabilityChecker();

        checker.registerUser("john_doe", "101");

        checker.registerUser("admin", "1");



        System.out.println(checker.checkAvailability("john_doe"));

        System.out.println(checker.checkAvailability("jane_smith"));

        System.out.println(checker.suggestAlternatives("john_doe") );


        System.out.println(checker.getMostAttempted());
    }
}
