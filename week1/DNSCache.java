import java.util.*;
import java.util.concurrent.*;

public class DNSCache {

    private class Node {
        String domain;
        String ipAddress;
        long expiryTime;
        Node prev, next;

        Node(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + ttlSeconds * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int capacity;
    private final Map<String, Node> cache = new HashMap<>();
    private Node head, tail;


    private long totalRequests = 0;
    private long hits = 0;
    private long misses = 0;
    private long totalLookupTime = 0;

    public DNSCache(int capacity) {
        this.capacity = capacity;
        startCleanupThread();
    }


    public synchronized String resolve(String domain) {
        long start = System.nanoTime();
        totalRequests++;

        if (cache.containsKey(domain)) {
            Node node = cache.get(domain);

            if (!node.isExpired()) {
                moveToHead(node);
                hits++;
                totalLookupTime += System.nanoTime() - start;
                return node.ipAddress;
            } else {
                removeNode(node);
                cache.remove(domain);
            }
        }


        misses++;
        String ip = queryUpstreamDNS(domain);
        put(domain, ip, 5);

        totalLookupTime += System.nanoTime() - start;
        return ip;
    }

    private void put(String domain, String ip, long ttl) {
        if (cache.size() >= capacity) {
            cache.remove(tail.domain);
            removeNode(tail);
        }

        Node node = new Node(domain, ip, ttl);
        addToHead(node);
        cache.put(domain, node);
    }

    private String queryUpstreamDNS(String domain) {
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        return "172.217.14." + new Random().nextInt(255);
    }

    private void addToHead(Node node) {
        node.next = head;
        node.prev = null;

        if (head != null)
            head.prev = node;

        head = node;

        if (tail == null)
            tail = head;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private void removeNode(Node node) {
        if (node.prev != null)
            node.prev.next = node.next;
        else
            head = node.next;

        if (node.next != null)
            node.next.prev = node.prev;
        else
            tail = node.prev;
    }

    private void startCleanupThread() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    synchronized (this) {
                        Node current = head;
                        while (current != null) {
                            Node next = current.next;
                            if (current.isExpired()) {
                                cache.remove(current.domain);
                                removeNode(current);
                            }
                            current = next;
                        }
                    }
                }, 5, 5, TimeUnit.SECONDS);
    }

    public synchronized void getCacheStats() {
        double hitRate = totalRequests == 0 ? 0 :
                (double) hits / totalRequests * 100;

        double avgLookupMs = totalRequests == 0 ? 0 :
                totalLookupTime / 1_000_000.0 / totalRequests;

        System.out.printf("Hit Rate: %.2f%%\n", hitRate);
        System.out.printf("Avg Lookup Time: %.2f ms\n", avgLookupMs);
        System.out.println("Hits: " + hits + ", Misses: " + misses);
    }


    public static void main(String[] args) throws Exception {
        DNSCache cache = new DNSCache(3);

        System.out.println("First lookup (MISS): " + cache.resolve("google.com"));
        System.out.println("Second lookup (HIT): " + cache.resolve("google.com"));

        Thread.sleep(6000); // wait for TTL to expire

        System.out.println("After expiry (MISS): " + cache.resolve("google.com"));

        cache.getCacheStats();
    }
}
