package alcatel.tess.hometop.gateways.test;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ObjectPool;
import alcatel.tess.hometop.gateways.utils.Recyclable;

/**
 * The class <code>TestPool</code> tests the ObjectPool class.
 *
 */
public class TestPool2 {
  
  static Logger _logger = Logger.getLogger("test");
  
  public static void main(String args[]) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: java TestPool2 <nThreads> <maxPrims>");
    }
    
    ObjectPool pool = new ObjectPool(PrimeCalculator.class);
    
    int nThreads = Integer.parseInt(args[0]);
    int max = Integer.parseInt(args[1]);
    
    PrimeWorker[] workers = new PrimeWorker[nThreads];
    long start = System.currentTimeMillis();
    
    for (int i = 0; i < nThreads; i++) {
      workers[i] = new PrimeWorker(max, pool);
      workers[i].start();
    }
    
    for (int i = 0; i < nThreads; i++) {
      workers[i].join();
    }
    
    long end = System.currentTimeMillis();
    System.out.println("Test Duration: " + (end - start));
  }
  
  /********************************* Worker thread computing prime numbers */
  
  public static class PrimeWorker extends Thread {
    int _max;
    ObjectPool _pool;
    
    public PrimeWorker(int max, ObjectPool pool) {
      _max = max;
      _pool = pool;
    }
    
    public void run() {
      int primeCandidate = 2;
      int totalPrimsFound = 0;
      
      while (true) {
        if (totalPrimsFound >= _max) {
          break;
        }
        
        PrimeCalculator primeCalc = (PrimeCalculator) _pool.acquire();
        try {
          if (primeCalc.isPrime(primeCandidate)) {
            totalPrimsFound++;
          }
        }
        
        finally {
          _pool.release(primeCalc);
        }
        
        primeCandidate++;
      }
      
      System.out.println("Last Prims found is " + primeCandidate);
    }
  }
  
  /********************************* Prime calculator *********************/
  
  public static class PrimeCalculator implements Recyclable {
    public PrimeCalculator() {
    }
    
    public void recycled() {
    }
    
    public boolean isValid() {
      return true;
    }
    
    private boolean isPrime(int candidate) {
      int factor = 2;
      while (factor < candidate) {
        if (candidate % factor == 0) {
          return false;
        }
        
        factor++;
      }
      return true;
    }
  }
}
