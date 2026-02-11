import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryManager{

    private ConcurrentHashMap<String,AtomicInteger>stockMap=new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,ConcurrentLinkedQueue<Long>> waitingMap = new ConcurrentHashMap<>();

    public void addProduct(String productId,int stock) {
        stockMap.put(productId,  new AtomicInteger(stock));
        waitingMap.put(productId, new ConcurrentLinkedQueue<>());
    }

    public String checkStock(String productId) {
        if (!stockMap.containsKey(productId)) {
            return  "Product not found" ;
        }
        return stockMap.get(productId).get()+"units available";
    }

    public String purchaseItem(String productId,long userId) {

        if (!stockMap.containsKey(productId)) {
            return "Product not found";
        }

        AtomicInteger stock=stockMap.get(productId);

        while(true){
            int current=stock.get();

            if (current>0) {

                if (stock.compareAndSet(current, current - 1)) {

                    return "Success, "+(current - 1)+" units remaining";
                }
            } else
            {
                waitingMap.get(productId).add(userId);

                int position=waitingMap.get(productId).size();

                return "Added to waiting list, position #"+position;
            }
        }
    }

    public int getWaitingCount(String productId) {

        return waitingMap.get(productId).size();
    }


    public static void main(String[] args) throws InterruptedException {

        InventoryManager manager=new InventoryManager();

        manager.addProduct("IPHONE15_256GB",100);

           System.out.println(manager.checkStock("IPHONE15_256GB"));

        ExecutorService service=Executors.newFixedThreadPool(50);

        for (int i=1;i<=50000;i++) {
            long userId=i;
            service.execute(()->{
                System.out.println(manager.purchaseItem("IPHONE15_256GB",userId));
            });
        }

         service.shutdown();
          service.awaitTermination(1,TimeUnit.MINUTES);

          System.out.println("Final Stock: "+manager.checkStock("IPHONE15_256GB"));

        System.out.println("Waiting List Count: "+manager.getWaitingCount("IPHONE15_256GB"));
    }
}
